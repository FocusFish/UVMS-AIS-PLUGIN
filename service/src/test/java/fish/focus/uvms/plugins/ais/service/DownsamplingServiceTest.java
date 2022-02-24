package fish.focus.uvms.plugins.ais.service;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import javax.enterprise.concurrent.ManagedExecutorService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.plugins.ais.StartupBean;
import fish.focus.uvms.plugins.ais.service.DownsamplingService;
import fish.focus.uvms.plugins.ais.service.ExchangeService;
import fish.focus.uvms.plugins.ais.service.ProcessService;

@RunWith(MockitoJUnitRunner.class)
public class DownsamplingServiceTest {
    
    @Mock
    private StartupBean startUp;

    @Mock
    ProcessService processService;

    @Mock
    private ExchangeService exchangeService;
    
    @Mock
    private ManagedExecutorService executorService;

    @InjectMocks
    private DownsamplingService downsamplingService;

    @Test
    public void sendDownSampledMovementsTest() {
        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");
        downsamplingService.getDownSampledMovements().put(movementBaseType.getMmsi(), movementBaseType);
        assertThat(downsamplingService.getDownSampledMovements().size(), is(1));
        
        downsamplingService.sendDownSampledMovements();
        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
    }
    
    @Test
    public void sendAssetUpdatesTest() {
        when(startUp.isEnabled()).thenReturn(true);
        assertThat(downsamplingService.getStoredAssetInfo().size(), is(0));
        downsamplingService.getStoredAssetInfo().put("123456", new AssetDTO());
        assertThat(downsamplingService.getStoredAssetInfo().size(), is(1));
        
        downsamplingService.sendAssetUpdates();
        assertThat(downsamplingService.getStoredAssetInfo().size(), is(0));
    }
}
