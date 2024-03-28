package fish.focus.uvms.consumer;

import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.plugins.ais.service.AisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import java.util.Set;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/topic/EventStream"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "aisUpdatedAsset"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "aisUpdatedAsset"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "event='Updated Asset'")

})
public class EventStreamListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(fish.focus.uvms.consumer.EventStreamListener.class);

    @Inject
    private AisService aisService;

    Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb =  new JsonBConfigurator().getContext(null);
    }

    @Override
    public void onMessage(Message inMessage) {
        LOG.debug("On message event stream listener for ais");

        TextMessage textMessage = (TextMessage) inMessage;
        try {
            AssetDTO asset = jsonb.fromJson(textMessage.getText(), AssetDTO.class);
            Set<String> knownFishingVessels = aisService.getKnownFishingVessels();
            if (asset.getVesselType() != null && asset.getVesselType().equals("Fishing")) {
                LOG.debug("Adding mmsi {} as fishing vessel, is now {}", asset.getMmsi(), asset.getVesselType());
                knownFishingVessels.add(asset.getMmsi());
            } else if (knownFishingVessels.contains(asset.getMmsi())) {
                LOG.debug("Removing mmsi {} as fishing vessel, is now {}", asset.getMmsi(), asset.getVesselType());
                knownFishingVessels.remove(asset.getMmsi());
            }
        } catch (RuntimeException e) {
            LOG.error("[ Error when receiving asset message in ais EventStreamListener ]", e);
        } catch (JMSException e2) {
            LOG.error("[ Error when receiving asset JMS message in ais EventStreamListener ]", e2);
        }
    }
}