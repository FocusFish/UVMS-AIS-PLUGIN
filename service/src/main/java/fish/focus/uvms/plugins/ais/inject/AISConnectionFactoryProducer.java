package fish.focus.uvms.plugins.ais.inject;

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
