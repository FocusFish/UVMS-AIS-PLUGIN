package fish.focus.uvms.plugins.ais.service;

import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.plugins.ais.StartupBean;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class DownsamplingAssetService {

    @Inject
    private StartupBean startUp;

    @Inject
    private ExchangeService exchangeService;

    private Map<String, AssetDTO> downSampledAssetInfo = new HashMap<>();

    @Schedule(minute = "6", hour = "*", persistent = false )
    public void sendAssetUpdates() {
        if (!startUp.isEnabled()) {
            return;
        }
        exchangeService.sendAssetUpdates(downSampledAssetInfo.values());
        downSampledAssetInfo.clear();
    }

    public Map<String, AssetDTO> getStoredAssetInfo(){
        return downSampledAssetInfo;
    }

}
