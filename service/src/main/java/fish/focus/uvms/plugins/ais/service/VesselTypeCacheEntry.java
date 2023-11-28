package fish.focus.uvms.plugins.ais.service;

import java.time.LocalDateTime;

public class VesselTypeCacheEntry {
    private LocalDateTime cacheDate;
    private boolean activeFishingVessel;

    public LocalDateTime getCacheDate() {
        return cacheDate;
    }

    public void setCacheDate(LocalDateTime cacheDate) {
        this.cacheDate = cacheDate;
    }

    public boolean isActiveFishingVessel() {
        return activeFishingVessel;
    }

    public void setActiveFishingVessel(boolean activeFishingVessel) {
        this.activeFishingVessel = activeFishingVessel;
    }
}
