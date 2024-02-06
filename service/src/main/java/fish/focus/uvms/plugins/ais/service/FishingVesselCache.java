package fish.focus.uvms.plugins.ais.service;

import fish.focus.uvms.asset.client.AssetClient;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.asset.client.model.AssetIdentifier;
import fish.focus.uvms.commons.cache.HavCache;
import org.apache.commons.lang3.StringUtils;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.annotation.PostConstruct;
import java.time.Duration;

@ApplicationScoped
public class FishingVesselCache {

    @Inject
    private AssetClient assetClient;

    private HavCache<String, AssetDTO> vesselCache;

    @PostConstruct
    public void setup() {
        vesselCache = new HavCache<>(mmsi -> StringUtils.isBlank(mmsi) ? null : assetClient.getAssetById(AssetIdentifier.MMSI, mmsi), Duration.ofMinutes(5L));
    }

    public AssetDTO get(String key, AssetDTO defaultValue) {
        return vesselCache.get(key, defaultValue);
    }
}
