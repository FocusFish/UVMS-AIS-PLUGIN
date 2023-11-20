package fish.focus.uvms.plugins.ais.service;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import javax.enterprise.concurrent.ManagedExecutorService;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DownsamplingServiceTest {
    
    @Mock
    private StartupBean startUp;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private ManagedExecutorService executorService; //injected in tested class (otherwise NPE)

    @InjectMocks
    private DownsamplingService downsamplingService;

    @Captor
    private ArgumentCaptor<Collection<AssetDTO>> captorAssets;

    @Test
    public void sendDownSampledFishingVesselMovementsTest() throws InterruptedException {
        assertThat(downsamplingService.getDownSampledFishingVesselMovements().size(), is(0));
        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");
        downsamplingService.getDownSampledFishingVesselMovements().put(movementBaseType.getMmsi(), movementBaseType);
        assertThat(downsamplingService.getDownSampledFishingVesselMovements().size(), is(1));
        downsamplingService.handleDownSampledFishingVesselMovements();
        assertThat(downsamplingService.getDownSampledFishingVesselMovements().size(), is(0));
    }

    @Test
    public void sendDownSampledMovementsTest() throws InterruptedException {
        when(startUp.getSetting("onlyAisFromFishingVessels")).thenReturn("false");

        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");

        downsamplingService.getDownSampledMovements().put(movementBaseType.getMmsi(), movementBaseType);
        assertThat(downsamplingService.getDownSampledMovements().size(), is(1));
        downsamplingService.handleDownSampledMovements();
        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
        //verify(exchangeService, Mockito.times(1)).sendMovements(Mockito.anyList());


    }

    @Test
    public void onlyLogDownSampledMovementsTest() {
        when(startUp.getSetting("onlyAisFromFishingVessels")).thenReturn("true");
        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");

        downsamplingService.getDownSampledMovements().put(movementBaseType.getMmsi(), movementBaseType);
        assertThat(downsamplingService.getDownSampledMovements().size(), is(1));

        downsamplingService.handleDownSampledMovements();

        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
        verify(exchangeService, Mockito.times(0)).sendMovements(Mockito.anyList());

    }
    @Test
    public void sendAssetUpdatesTest() {
        when(startUp.isEnabled()).thenReturn(true);
        assertThat(downsamplingService.getStoredAssetInfo().size(), is(0));
        downsamplingService.getStoredAssetInfo().put("123456", new AssetDTO());
        assertThat(downsamplingService.getStoredAssetInfo().size(), is(1));
        
        downsamplingService.sendAssetUpdates();
        assertThat(downsamplingService.getStoredAssetInfo().size(), is(0));

        verify(exchangeService, Mockito.times(1)).sendAssetUpdates(Mockito.anyCollection());
        verify(exchangeService).sendAssetUpdates(captorAssets.capture());
        assertThat(captorAssets.getAllValues().size(), is(1));
    }
}
