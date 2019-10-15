package scot.mygov.publishing.linkprocessors;

import org.hippoecm.hst.core.linking.HstLink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class TrailingSlashLinkProcessorTest {

    @Test
    public void postProcessAddsTrailingSlash() {
        // Arrange
        TrailingSlashLinkProcessor sut = new TrailingSlashLinkProcessor();
        HstLink input = link("sectors/energy");

        // Act
        String actual = sut.doPostProcess(input).getPath();

        // Assert
        String expected = input.getPath() + "/";
        assertEquals(expected, actual);
    }

    HstLink link(String path) {
        HstLink link = mock(HstLink.class);
        when(link.getPath()).thenReturn(path);
        return link;
    }
}
