package scot.mygov.publishing.channels;

import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.core.parameters.Parameter;

public interface WebsiteInfo extends ChannelInfo {

    @Parameter(name = "logo")
    String getLogoPath();

    @Parameter(name = "themeCss")
    String getThemeCss();

    @Parameter(name = "colour")
    String getColour();
}
