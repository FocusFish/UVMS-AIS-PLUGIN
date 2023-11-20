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

import java.util.Map;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.asset.client.model.AssetDTO;

public class ProcessResult {

    private Map<String, MovementBaseType> downsampledMovements;
    private Map<String, MovementBaseType> downSampledFishingVesselMovements;
    private Map<String, AssetDTO> downsampledAssets;
    
    public ProcessResult(Map<String, MovementBaseType> downsampledMovements, Map<String, MovementBaseType> downSampledFishingVesselMovements, Map<String, AssetDTO> downsampledAssets) {
        this.downsampledMovements = downsampledMovements;
        this.downSampledFishingVesselMovements = downSampledFishingVesselMovements;
        this.downsampledAssets = downsampledAssets;
    }
    
    public Map<String, MovementBaseType> getDownsampledMovements() {
        return downsampledMovements;
    }

    public Map<String, MovementBaseType> getDownSampledFishingVesselMovements() {
        return downSampledFishingVesselMovements;
    }

    public void setDownSampledFishingVesselMovements(Map<String, MovementBaseType> downSampledFishingVesselMovements) {
        this.downSampledFishingVesselMovements = downSampledFishingVesselMovements;
    }

    public void setDownsampledMovements(Map<String, MovementBaseType> downsampledMovements) {
        this.downsampledMovements = downsampledMovements;
    }
    public Map<String, AssetDTO> getDownsampledAssets() {
        return downsampledAssets;
    }
    public void setDownsampledAssets(Map<String, AssetDTO> downsampledAssets) {
        this.downsampledAssets = downsampledAssets;
    }
}
