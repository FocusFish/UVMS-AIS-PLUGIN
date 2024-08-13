package fish.focus.uvms.plugins.ais.service;

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Singleton
public class DownsamplingFishingService {
    private static final Logger LOG = LoggerFactory.getLogger(DownsamplingFishingService.class);

    @Inject
    private ExchangeService exchangeService;

    private ConcurrentMap<String, MovementBaseType> downSampledFishingVesselMovements = new ConcurrentHashMap<>();

    @Schedule(minute = "*/1", hour = "*", persistent = false)
    public void handleDownSampledFishingVesselMovements() {
        if (downSampledFishingVesselMovements.isEmpty()) {
            return;
        }
        LOG.info("Handle {} downSampledFishingVesselMovements", downSampledFishingVesselMovements.size());
        List<MovementBaseType> movements = new ArrayList<>(downSampledFishingVesselMovements.values());
        downSampledFishingVesselMovements.clear();
        exchangeService.sendMovements(movements);
    }

    public Map<String, MovementBaseType> getDownSampledFishingVesselMovements() {
        return downSampledFishingVesselMovements;
    }

}
