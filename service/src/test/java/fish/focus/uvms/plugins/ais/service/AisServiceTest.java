package fish.focus.uvms.plugins.ais.service;

import fish.focus.uvms.ais.AISConnection;
import fish.focus.uvms.ais.AISConnectionFactory;
import fish.focus.uvms.ais.Sentence;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.resource.ResourceException;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.with;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class AisServiceTest {

    @Mock
    private StartupBean startUp;

    @Mock
    private ProcessService processService;

    @Mock
    DownsamplingService downsamplingService;

    @Mock
    DownsamplingFishingService downsamplingFishingService;

    @Mock
    DownsamplingAssetService downsamplingAssetService;

    @Mock
    ManagedExecutorService managedExecutorService;

    @Mock
    AISConnectionFactory factory;

    @InjectMocks
    private AisService aisService;

    private int numberOfGeneratedReconnectAnswers = 0;
    private final List<Boolean> reconnectAnswersForBackOffs = List.of(
            // init
            false,
            // connectAndRetrieve -> isConnectionDown
            // Should have a "live" connection since these tests are about a
            // connection not receiving data while still open
            true, true, true
    );


    @Before
    public void setupMocks() {
        numberOfGeneratedReconnectAnswers = 0;

        when(startUp.isEnabled()).thenReturn(true);
        when(startUp.getSetting("HOST")).thenReturn("127.0.0.0");
        when(startUp.getSetting("PORT")).thenReturn("0");
        when(startUp.getSetting("USERNAME")).thenReturn("myusername");
        when(startUp.getSetting("PASSWORD")).thenReturn("mypassword");
    }

    private boolean generateConnectionAnswersForBackOffTests(InvocationOnMock input) {
        // mimics internal state in AisService

        numberOfGeneratedReconnectAnswers++;
        if (numberOfGeneratedReconnectAnswers == 25) {
            // need to skip the init in the 25th run since it should be in a "lockout" period then
            numberOfGeneratedReconnectAnswers++;
        }

        return reconnectAnswersForBackOffs.get((numberOfGeneratedReconnectAnswers - 1) % 4);
    }

    // White-box testing the reconnect functionality

    @Test
    public void shouldNotReconnectDuringShortBackOffTime() throws ResourceException {
        AISConnection connectionMock = mock(AISConnection.class);
        when(connectionMock.getSentences()).thenReturn(List.of());
        when(connectionMock.isOpen()).thenAnswer(this::generateConnectionAnswersForBackOffTests);
        when(factory.getConnection()).thenReturn(connectionMock);

        aisService.init();
        aisService.connectAndRetrieve();

        // One in init
        verify(connectionMock, times(1)).open(anyString(), anyInt(), anyString(), anyString());
        verify(connectionMock, times(0)).close();
    }

    @Test
    public void shouldReconnectWhenNoMessagesOnFirstTryWithNoBackOffTime() throws ResourceException {
        AISConnection connectionMock = mock(AISConnection.class);
        when(connectionMock.getSentences()).thenReturn(List.of());
        when(connectionMock.isOpen()).thenAnswer(this::generateConnectionAnswersForBackOffTests);
        when(factory.getConnection()).thenReturn(connectionMock);

        aisService.shortBackOffTime = 0;

        aisService.init();
        aisService.connectAndRetrieve();

        // One in init and one in reconnect
        verify(connectionMock, times(2)).open(anyString(), anyInt(), anyString(), anyString());
        verify(connectionMock).close();
    }

    @Test
    public void shouldReconnectWhenNoMessagesOnSixthTryAfterLongBackOffTime() throws ResourceException {
        AISConnection connectionMock = mock(AISConnection.class);
        when(connectionMock.getSentences()).thenReturn(List.of());
        when(connectionMock.isOpen()).thenAnswer(this::generateConnectionAnswersForBackOffTests);
        when(factory.getConnection()).thenReturn(connectionMock);

        aisService.shortBackOffTime = 0;
        // windows might have time resolution in 10s of millis?
        aisService.longBackOffTime = 50;
        aisService.backOffUnit = MILLIS;

        aisService.init();

        for (int i = 0; i < 6; i++) {
            aisService.connectAndRetrieve();
        }

        // Need to wait for the above lockout period to pass before running again
        with()
                .pollDelay(100, MILLISECONDS)
                .await()
                .atMost(1, SECONDS)
                .untilAsserted(() -> {
                    aisService.connectAndRetrieve();
                    verify(connectionMock, times(7)).open(anyString(), anyInt(), anyString(), anyString());
                    verify(connectionMock, times(6)).close();
                });
    }

    @Test
    public void shouldReconnectWhenConnectionIsClosed() throws ResourceException {
        AISConnection connectionMock = mock(AISConnection.class);
        when(connectionMock.getSentences()).thenReturn(List.of(new Sentence("", "")));
        when(connectionMock.isOpen()).thenReturn(false, // init
                true, false, false, true);
        when(factory.getConnection()).thenReturn(connectionMock);

        aisService.init();
        aisService.connectAndRetrieve();

        // One in init and one in reconnect
        verify(connectionMock, times(2)).open(anyString(), anyInt(), anyString(), anyString());
        verify(connectionMock).close();
    }

    @Test
    public void shouldCloseConnectionWhenPluginDisabled() throws ResourceException {
        when(startUp.isEnabled()).thenReturn(true, false);

        AISConnection connectionMock = mock(AISConnection.class);
        when(connectionMock.isOpen()).thenReturn(false, true);
        when(factory.getConnection()).thenReturn(connectionMock);

        aisService.init();
        aisService.connectAndRetrieve();

        verify(connectionMock, times(0)).getSentences();
        verify(connectionMock).close();
    }
}
