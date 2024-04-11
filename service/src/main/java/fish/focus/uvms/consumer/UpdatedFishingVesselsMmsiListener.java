package fish.focus.uvms.consumer;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;

@MessageDriven(activationConfig = {
        @ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Topic"),
        @ActivationConfigProperty(propertyName = "destinationLookup", propertyValue = "jms/topic/EventStream"),
        @ActivationConfigProperty(propertyName = "subscriptionDurability", propertyValue = "Durable"),
        @ActivationConfigProperty(propertyName = "subscriptionName", propertyValue = "aisUpdatedAsset"),
        @ActivationConfigProperty(propertyName = "clientId", propertyValue = "aisUpdatedAsset"),
        @ActivationConfigProperty(propertyName = "messageSelector", propertyValue = "event='Updated Asset'")

})
public class UpdatedFishingVesselsMmsiListener extends EventStreamListener {
}