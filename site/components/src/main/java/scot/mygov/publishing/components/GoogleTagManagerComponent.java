package scot.mygov.publishing.components;

import org.apache.commons.lang3.StringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.mygov.publishing.HippoUtils;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import static org.apache.commons.lang3.StringUtils.*;

/**
 * Set fields required to correctly output google tag manager fields.
 *
 * We set the following attributes:
 * - useLiveAnalytics: indicate to the template whether to use the values for gtmAuth and gtmEnv (this is set to true
 *                     if both gtmAuth and gtmEnv have no empty values
 * - gtmName: the format of the page, e.g. article
 * - gtmId: slug for this page
 * - gtmContainerId: the google tag manager container id
 * - gtmAuth: value to use for the gtm_auth parameter
 * - gtmEnv: value to use in the gtm_preview parameter
 */
public class GoogleTagManagerComponent extends BaseHstComponent {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleTagManagerComponent.class);

    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);

        setGtmName(request);
        setGtmId(request);
        setMountDependentAttributes(request);
    }

    /**
     * set gtmName based on the page component from the resolved sitemap item
     *
     * the gtm name is the format of the page, e.g. 'article'
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
     * gtmContainerId, gtmAuth and gtmEnv are set based on the path stored on the mount.
     *
     * This path is used to look up a node containing the relevant values.
     */
    void setMountDependentAttributes(HstRequest request) {

        Mount mount = request
                .getRequestContext()
                .getResolvedSiteMapItem()
                .getResolvedMount()
                .getMount();
        String gtmPath = mount.getProperty("publishing:gtm");

        if (isBlank(gtmPath)) {
            LOG.error("Mount has not publishing:gtm path:", mount.getName());
            setEmptyGtmValues(request);
            return;
        }

        try {
            Node gtmNode = getGtmNode(request, gtmPath);
            if (gtmNode == null) {
                setEmptyGtmValues(request);
                return;
            }

            setGtmValues(request,
                    gtmNode.getProperty("publishing:containerId").getString(),
                    gtmNode.getProperty("publishing:auth").getString(),
                    gtmNode.getProperty("publishing:env").getString());
        } catch (RepositoryException e) {
            LOG.error("Unexpected repository excpetion trying to set gtm values, gtmPath is {}", gtmPath, e);
            setEmptyGtmValues(request);
        }
    }

    void setGtmValues(HstRequest request, String containerId, String auth, String env) {
        request.setAttribute("gtmAuth", auth);
        request.setAttribute("gtmEnv", env);
        request.setAttribute("gtmContainerId", containerId);
        request.setAttribute("useLiveAnalytics", StringUtils.isNoneBlank(auth, env));
    }

    void setEmptyGtmValues(HstRequest request) {
        setGtmValues(request, "", "", "");
    }

    Node getGtmNode(HstRequest request, String path) throws RepositoryException {
        HstRequestContext requestContext = request.getRequestContext();
        Session session = requestContext.getSession();

        if (!session.nodeExists(path)) {
            LOG.error("gtm path does not exist: {}", path);
            return null;
        }

        Node gtmHandle = session.getNode(path);
        Node gtmNode = HippoUtils.getPublishedVariant(gtmHandle);
        if (gtmNode == null) {
            LOG.info("No published gtm document for path: {}", path);
        }

        return gtmNode;
    }

}
