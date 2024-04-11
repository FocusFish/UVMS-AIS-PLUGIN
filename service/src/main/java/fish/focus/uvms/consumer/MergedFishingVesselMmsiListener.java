package fish.focus.uvms.consumer;

import fish.focus.uvms.commons.date.JsonBConfigurator;
import fish.focus.uvms.plugins.ais.service.AisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.json.bind.Jsonb;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/topic/EventStream"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "aisMergedAsset"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "aisMergedAsset"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "event='Merged Asset'")

})
public class MergedFishingVesselMmsiListener implements MessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(MergedFishingVesselMmsiListener.class);

    @Inject
    private AisService aisService;

    Jsonb jsonb;

    @PostConstruct
    public void init() {
        jsonb =  new JsonBConfigurator().getContext(null);
    }

    @Override
    public void onMessage(Message inMessage) {
        LOG.debug("On merged message event stream listener for ais");
        EventStreamListenerHelper.addFishingVesselonMessage(jsonb, aisService, inMessage);
    }
}
