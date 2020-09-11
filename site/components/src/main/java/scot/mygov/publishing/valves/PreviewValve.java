package scot.mygov.publishing.valves;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.container.valves.AbstractOrderableValve;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.container.ContainerException;
import org.hippoecm.hst.core.container.ValveContext;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.repository.util.JcrUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Calendar;

public class PreviewValve extends AbstractOrderableValve {

    private static final Logger LOG = LoggerFactory.getLogger(PreviewValve.class);
    private static final String PREVIEW_KEY = "previewkey";

    @Override
    public void invoke(ValveContext context) throws ContainerException {
        try {
            HstRequestContext requestContext = context.getRequestContext();
            Mount resolvedMount = requestContext.getResolvedMount().getMount();
            if ("preview".equals(resolvedMount.getType()) && resolvedMount.getAlias().endsWith("-staging")) {
                //fetching the content bean
                HippoBean contentBean = requestContext.getContentBean();
                //fetching the previewkey
                String previewKey = getPreviewKey(context, resolvedMount);
                try {
                    //intercepting requests having the id in the url
                    if (contentBean != null && StringUtils.isNotBlank(previewKey)) {
                        boolean found = false;
                        NodeIterator iterator = contentBean.getNode().getNodes("previewId");
                        while (iterator.hasNext() && !found) {
                            Node node = iterator.nextNode();
                            Calendar expirationCalendar = JcrUtils.getDateProperty(node, "staging:expirationdate", null);
                            if (JcrUtils.getStringProperty(node, "staging:key", "").equals(previewKey) && isValid(expirationCalendar)) {
                                found = true;
                            }
                        }
                        if (!found) {
                            LOG.debug("Preview key {} for document {} is invalid or preview link has expired.", contentBean.getPath(), previewKey);
                            requestContext.getServletResponse().sendError(403);
                        }
                    } else {
                        LOG.debug("Preview request doesn't contain content bean or preview key");
                        requestContext.getServletResponse().sendError(403);
                    }
                } catch (RepositoryException repositoryException) {
                    LOG.error("Something with repo went wrong while accessing this node {}.", requestContext.getSiteContentBaseBean(), repositoryException);
                } catch (IOException ioException) {
                    LOG.error("Something with IO went wrong while accessing this node {}.", requestContext.getSiteContentBaseBean(), ioException);
                }
            }
        } finally {
            context.invokeNext();
        }
    }

    private boolean isValid(final Calendar expirationCalendar){
        return expirationCalendar == null || Calendar.getInstance().before(expirationCalendar);
    }

    private String getPreviewKey(final ValveContext context, final Mount resolvedMount){
        String previewKey = context.getServletRequest().getParameter(PREVIEW_KEY);
        Cookie previewCookie = getPreviewCookie(context);

        if(StringUtils.isNotEmpty(previewKey) && previewCookie == null){
            Cookie cookie = new Cookie(PREVIEW_KEY, previewKey);
            cookie.setPath(getPath(context.getServletRequest()));
            boolean httpOnly = Boolean.parseBoolean(resolvedMount.getProperty("preview-cookie-httponly"));
            boolean secure = Boolean.parseBoolean(resolvedMount.getProperty("preview-cookie-secure"));
            cookie.setHttpOnly(httpOnly);
            cookie.setSecure(secure);
            context.getServletResponse().addCookie(cookie);
            return previewKey;
        } else if (StringUtils.isNotEmpty(previewKey) && previewCookie != null){
            if(!previewKey.equals(previewCookie.getValue())){
                previewCookie.setValue(previewKey);
                context.getServletResponse().addCookie(previewCookie);
            }
            return previewKey;
        } else if (StringUtils.isEmpty(previewKey) && previewCookie != null){
            return previewCookie.getValue();
        } else {
            return null;
        }
    }

    private static String getPath(HttpServletRequest request) {
        final String contextPath = request.getContextPath();
        if (StringUtils.isBlank(contextPath)) {
            return "/";
        }
        return contextPath;
    }

    private Cookie getPreviewCookie(final ValveContext context){
        Cookie[] cookies = context.getServletRequest().getCookies();
        if(cookies!=null) {
            for (Cookie cookie : cookies) {
                if (PREVIEW_KEY.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

}

