package fish.focus.uvms.plugins.ais.service;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class FailedMovementServiceTest {

    @Mock
    private StartupBean startUp;

    @Mock
    private ExchangeService exchangeService;

    @InjectMocks
    private FailedMovementsService failedMovementsService;

    @Captor
    private ArgumentCaptor<List<MovementBaseType>> captorExchange;

    @Test
    public void resendFailedMovementsTest() {
        when(startUp.isRegistered()).thenReturn(true);

        MovementBaseType failedMovement = new MovementBaseType();
        List<MovementBaseType> failedMovements = List.of(failedMovement);
        failedMovementsService.add(failedMovements);

        failedMovementsService.resend();

        verify(exchangeService).sendMovements(Mockito.anyCollection());
        verify(exchangeService).sendMovements(captorExchange.capture());

        assertThat(captorExchange.getAllValues().size(), is(1));
    }
}
