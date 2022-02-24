/*
﻿Developed with the contribution of the European Commission - Directorate General for Maritime Affairs and Fisheries
© European Union, 2015-2016.

This file is part of the Integrated Fisheries Data Management (IFDM) Suite. The IFDM Suite is free software: you can
redistribute it and/or modify it under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or any later version. The IFDM Suite is distributed in
the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details. You should have received a
copy of the GNU General Public License along with the IFDM Suite. If not, see <http://www.gnu.org/licenses/>.
 */
package fish.focus.uvms.plugins.ais.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.plugins.ais.StartupBean;

@Singleton
public class DownsamplingService {

    @Inject
    private StartupBean startUp;
    
    @Inject
    private ExchangeService exchangeService;
    
    @Resource
    private ManagedExecutorService executorService;
    
    private ConcurrentMap<String, MovementBaseType> downSampledMovements = new ConcurrentHashMap<>();
    private Map<String, AssetDTO> downSampledAssetInfo = new HashMap<>();
    
//    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    @Schedule(minute = "*/5", hour = "*", persistent = false )
    public void sendDownSampledMovements() {
        if (downSampledMovements.isEmpty()) {
            return;
        }

        List<MovementBaseType> movements = new ArrayList<>(downSampledMovements.values());
        downSampledMovements.clear();
        CompletableFuture.runAsync(() -> exchangeService.sendToExchange(movements, startUp.getRegisterClassName()), executorService);
    }
    
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

    public Map<String, MovementBaseType> getDownSampledMovements() {
        return downSampledMovements;
    }
    
}
