package fish.focus.uvms.plugins.ais.producer;

import fish.focus.uvms.asset.client.AssetClient;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.asset.client.model.AssetIdentifier;
import fish.focus.uvms.commons.cache.HavCache;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.time.Duration;

@ApplicationScoped
public class HavCacheProducer {

    @Inject
    private AssetClient assetClient;

    @Produces
    @ApplicationScoped
    public HavCache<String, AssetDTO> produceCache() {
        return new HavCache<>(mmsi -> StringUtils.isBlank(mmsi) ? null : assetClient.getAssetById(AssetIdentifier.MMSI, mmsi), Duration.ofMinutes(5L));
    }
}
