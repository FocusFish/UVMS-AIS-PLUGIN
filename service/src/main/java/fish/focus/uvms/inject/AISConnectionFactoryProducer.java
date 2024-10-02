package fish.focus.uvms.inject;

import fish.focus.uvms.ais.AISConnectionFactory;

import javax.annotation.Resource;
import javax.enterprise.inject.Produces;

public class AISConnectionFactoryProducer {

    @Resource(lookup = "java:/eis/AISConnectionFactory")
    private AISConnectionFactory aisConnectionFactory;

    @Produces
    @Managed
    public AISConnectionFactory createAISConnectionFactory() {
        return aisConnectionFactory;
    }
}
