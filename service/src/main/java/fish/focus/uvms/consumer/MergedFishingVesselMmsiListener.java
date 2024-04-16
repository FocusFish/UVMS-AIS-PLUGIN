package fish.focus.uvms.consumer;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/topic/EventStream"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "aisMergedAsset"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "aisMergedAsset"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "event='Merged Asset'")

})
public class MergedFishingVesselMmsiListener extends EventStreamListener {
}
