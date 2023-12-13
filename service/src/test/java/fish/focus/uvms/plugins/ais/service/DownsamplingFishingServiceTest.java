package fish.focus.uvms.plugins.ais.service;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class DownsamplingFishingServiceTest {
    @Mock
    private ExchangeService exchangeService;

    @InjectMocks
    private DownsamplingFishingService downsamplingFishingService;

    @Captor
    private ArgumentCaptor<Collection<MovementBaseType>> captorMovements;

    @Test
    public void sendDownSampledFishingVesselMovementsTest() throws InterruptedException {
        assertThat(downsamplingFishingService.getDownSampledFishingVesselMovements().size(), is(0));
        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");
        downsamplingFishingService.getDownSampledFishingVesselMovements().put(movementBaseType.getMmsi(), movementBaseType);
        assertThat(downsamplingFishingService.getDownSampledFishingVesselMovements().size(), is(1));
        downsamplingFishingService.handleDownSampledFishingVesselMovements();
        assertThat(downsamplingFishingService.getDownSampledFishingVesselMovements().size(), is(0));
        verify(exchangeService, Mockito.times(1)).sendMovements(captorMovements.capture());
        assertThat(captorMovements.getValue().size(), is(1));
    }

}
