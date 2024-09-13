package fish.focus.uvms.inject;

import javax.annotation.Resource;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.enterprise.inject.Produces;

public class ManagedExecutorServiceProducer {

    @Resource
    private ManagedExecutorService managedExecutorService;

    @Produces
    @Managed
    public ManagedExecutorService createManagedExecutorService() {
        return managedExecutorService;
    }
}
