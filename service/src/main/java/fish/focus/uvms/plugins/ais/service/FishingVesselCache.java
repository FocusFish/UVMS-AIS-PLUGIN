package fish.focus.uvms.plugins.ais.service;

import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.commons.cache.HavCache;

public class FishingVesselCache {

    private HavCache<String, AssetDTO> vesselCache;

    public AssetDTO get(String key, AssetDTO defaultValue) {
        return vesselCache.get(key, defaultValue);
    }
}
