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
import org.mockito.junit.MockitoJUnitRunner;

import javax.enterprise.concurrent.ManagedExecutorService;
import javax.resource.ResourceException;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.NANOS;
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

    @Before
    public void setupMocks() {
        when(startUp.isEnabled()).thenReturn(true);
        when(startUp.getSetting("HOST")).thenReturn("127.0.0.0");
        when(startUp.getSetting("PORT")).thenReturn("0");
        when(startUp.getSetting("USERNAME")).thenReturn("myusername");
        when(startUp.getSetting("PASSWORD")).thenReturn("mypassword");
    }

    // White-box testing the reconnect functionality

    @Test
    public void shouldNotReconnectDuringShortBackOffTime() throws ResourceException {
        AISConnection connectionMock = mock(AISConnection.class);
        when(connectionMock.getSentences()).thenReturn(List.of());
        when(connectionMock.isOpen()).thenReturn(false, true, true, true, false);
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
        when(connectionMock.isOpen()).thenReturn(false, true, true, true, false);
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
        // mimics internal state in AisService
        when(connectionMock.isOpen()).thenReturn(false, // init from this method

                // 1 connectAndRetrieve
                true, true, true, // isConnectionDown
                false, // init

                // 2 connectAndRetrieve
                true, true, true, // isConnectionDown
                false, // init

                // 3 connectAndRetrieve
                true, true, true, // isConnectionDown
                false, // init

                // 4 connectAndRetrieve
                true, true, true, // isConnectionDown
                false, // init

                // 5 connectAndRetrieve
                true, true, true, // isConnectionDown
                false, // init

                // 6 connectAndRetrieve
                true, true, true, // isConnectionDown

                // 7 connectAndRetrieve
                true, true, true, // isConnectionDown
                false // init
        );
        when(factory.getConnection()).thenReturn(connectionMock);

        aisService.shortBackOffTime = 0;
        aisService.backOffUnit = MILLIS;

        aisService.init();

        for (int i = 0; i < 6; i++) {
            aisService.connectAndRetrieve();
        }

        // Need to wait for the above lockout period to pass before running again
        with().pollDelay(20, MILLISECONDS).await().atMost(2, SECONDS)
                .untilAsserted(() -> aisService.connectAndRetrieve());

        verify(connectionMock, times(7)).open(anyString(), anyInt(), anyString(), anyString());
        verify(connectionMock, times(6)).close();
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
