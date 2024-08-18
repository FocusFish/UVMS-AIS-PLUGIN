package fish.focus.uvms.plugins.ais.service;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DownsamplingServiceTest {

    @Mock
    private StartupBean startUp;

    @Mock
    private ExchangeService exchangeService;

    @Mock
    private FailedMovementsService failedMovementsService;

    @InjectMocks
    private DownsamplingService downsamplingService;

    @Captor
    private ArgumentCaptor<Collection<MovementBaseType>> exchangeCaptorMovements;

    @Captor
    private ArgumentCaptor<List<MovementBaseType>> failedMovementsCaptor;

    @Test
    public void sendDownSampledMovementsTest() {
        when(startUp.getSetting("onlyAisFromFishingVessels")).thenReturn("false");

        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
        MovementBaseType movementBaseType = new MovementBaseType();
        movementBaseType.setMmsi("123456789");

        downsamplingService.getDownSampledMovements().put(movementBaseType.getMmsi(), movementBaseType);
        assertThat(downsamplingService.getDownSampledMovements().size(), is(1));
        downsamplingService.handleDownSampledMovements();
        assertThat(downsamplingService.getDownSampledMovements().size(), is(0));
        verify(exchangeService, Mockito.times(1)).sendMovements(exchangeCaptorMovements.capture());
        assertThat(exchangeCaptorMovements.getValue().size(), is(1));

        verify(failedMovementsService, times(1)).add(failedMovementsCaptor.capture());
        assertThat(failedMovementsCaptor.getValue().size(), is(0));
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
}
