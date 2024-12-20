package fish.focus.uvms.plugins.ais.consumer;

import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.plugins.ais.service.AisService;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import java.util.HashSet;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class EventStreamListenerTest extends TestCase {

    @Mock
    private AisService aisService;

    @InjectMocks
    private EventStreamListener eventStreamListener;

    @Test
    public void testOnMessage() throws JMSException {
        TextMessage message = Mockito.mock(TextMessage.class);
        String json = "{\"mmsi\": \"12345\",\"vesselType\":\"Fishing\"}";
        Mockito.when(message.getText()).thenReturn(json);
        Set<String> knownFishingVessels = new HashSet<>();
        Mockito.when(aisService.getKnownFishingVessels()).thenReturn(knownFishingVessels);
        eventStreamListener.jsonb = Mockito.mock(Jsonb.class);
        AssetDTO assetDTO = new AssetDTO();
        assetDTO.setMmsi("123");
        assetDTO.setVesselType("Fishing");
        Mockito.when(eventStreamListener.jsonb.fromJson(Mockito.anyString(), Mockito.any(Class.class))).thenReturn(assetDTO);
        eventStreamListener.onMessage(message);
        assertEquals(1, knownFishingVessels.size());
    }
}