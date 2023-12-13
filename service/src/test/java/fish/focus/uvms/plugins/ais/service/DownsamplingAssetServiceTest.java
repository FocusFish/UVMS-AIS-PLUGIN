package fish.focus.uvms.plugins.ais.service;

import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownsamplingAssetServiceTest {
    @Mock
    private StartupBean startUp;

    @Mock
    private ExchangeService exchangeService;

    @InjectMocks
    private DownsamplingAssetService downsamplingAssetService;

    @Captor
    private ArgumentCaptor<Collection<AssetDTO>> captorAssets;

    @Test
    public void sendAssetUpdatesTest() {
        when(startUp.isEnabled()).thenReturn(true);
        assertThat(downsamplingAssetService.getStoredAssetInfo().size(), is(0));
        downsamplingAssetService.getStoredAssetInfo().put("123456", new AssetDTO());
        assertThat(downsamplingAssetService.getStoredAssetInfo().size(), is(1));

        downsamplingAssetService.sendAssetUpdates();
        assertThat(downsamplingAssetService.getStoredAssetInfo().size(), is(0));

        verify(exchangeService, Mockito.times(1)).sendAssetUpdates(Mockito.anyCollection());
        verify(exchangeService).sendAssetUpdates(captorAssets.capture());
        assertThat(captorAssets.getAllValues().size(), is(1));
    }

}
