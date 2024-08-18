package fish.focus.uvms.plugins.ais.service;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownsamplingFishingServiceTest {
    @Mock
    private ExchangeService exchangeService;

    @Mock
    private FailedMovementsService failedMovementsService;

    @InjectMocks
    private DownsamplingFishingService downsamplingFishingService;

    @Captor
    private ArgumentCaptor<Collection<MovementBaseType>> exchangeCaptorMovements;

    @Captor
    private ArgumentCaptor<List<MovementBaseType>> failedMovementsCaptor;

    @Test
    public void sendDownSampledFishingVesselMovementsTest() {
        assertThat(downsamplingFishingService.getDownSampledFishingVesselMovements().size(), is(0));

        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");
        downsamplingFishingService.getDownSampledFishingVesselMovements().put(movementBaseType.getMmsi(), movementBaseType);
        assertThat(downsamplingFishingService.getDownSampledFishingVesselMovements().size(), is(1));

        downsamplingFishingService.handleDownSampledFishingVesselMovements();
        assertThat(downsamplingFishingService.getDownSampledFishingVesselMovements().size(), is(0));

        verify(exchangeService, times(1)).sendMovements(exchangeCaptorMovements.capture());
        assertThat(exchangeCaptorMovements.getValue().size(), is(1));

        verify(failedMovementsService, times(1)).add(failedMovementsCaptor.capture());
        assertThat(failedMovementsCaptor.getValue().size(), is(0));
    }
}
