package scot.mygov.publishing.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

public class GoogleTagManagerComponent extends BaseHstComponent {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleTagManagerComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);

        setGtmName(request);
        setGtmId(request);
        setGtmAuthAndEnv(request);
    }

    /**
     * set gtmName based on the page component from the resolved sitemap item
     *
     * the gtm name is the format fo ther page, e.g. 'article'
     */
    void setGtmName(HstRequest request) {
        HstComponentConfiguration componentConfig = request
                .getRequestContext()
                .getResolvedSiteMapItem()
                .getHstComponentConfiguration();
        String gtmName = componentConfig.getName();
        request.setAttribute("gtmName", gtmName);
    }

    /**
     * set gtmId on the path from the resolved sitemap item
     * the gtmId is the slug of this page
     */
    void setGtmId(HstRequest request) {
        String gtmId = request
                .getRequestContext()
                .getResolvedSiteMapItem()
                .getPathInfo();
        request.setAttribute("gtmId", gtmId);
    }

    /**
     * gtmAuth and gtmEnv are set based on the path stored on the mount.  This path is used to look up a node
     * containing the relevant values.
     */
    void setGtmAuthAndEnv(HstRequest request) {

        String gtmPath = request
                .getRequestContext()
                .getResolvedSiteMapItem()
                .getResolvedMount()
                .getMount()
                .getProperty("publishing:gtm");

        String auth = "";
        String env = "";

        try {
            HstRequestContext requestContext = request.getRequestContext();
            Session session = requestContext.getSession();
            Node gtmNode = session.getNode(gtmPath);
            auth = gtmNode.getProperty("publishing:auth").getString();
            env = gtmNode.getProperty("publishing:env").getString();
        } catch (RepositoryException e) {
            LOG.error("Unable to get gtm value from mount: gtmPath is {}", gtmPath, e);
        }

        request.setAttribute("gtmAuth", auth);
        request.setAttribute("gtmEnv", env);
    }
}
