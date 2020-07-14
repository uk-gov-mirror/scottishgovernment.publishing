package scot.mygov.publishing.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scot.mygov.publishing.channels.WebsiteInfo;

public class DetermineStylingComponent extends BaseHstComponent {

    private static final Logger LOG = LoggerFactory.getLogger(DetermineStylingComponent.class);

    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);

        Mount mount = request.getRequestContext().getResolvedMount().getMount();
        WebsiteInfo info = mount.getChannelInfo();

        String logoPath = info.getLogoPath();
        String themeCssPath = info.getThemeCss();
        LOG.info("CSS Path: {}", themeCssPath);

        LOG.info("Setting styling attribute(s)");
        request.setAttribute("themeCssPath", themeCssPath);
    }

}
