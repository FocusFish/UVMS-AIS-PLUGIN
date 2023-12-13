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

import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.plugins.ais.StartupBean;
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
public class DownsamplingService {
    private static final Logger LOG= LoggerFactory.getLogger(DownsamplingService.class);
    private static final Logger LOG_SAVED_MOVEMENTS = LoggerFactory.getLogger("SAVED_MOVEMENTS");
	
    @Inject
    private StartupBean startUp;
    
    @Inject
    private ExchangeService exchangeService;

    private ConcurrentMap<String, MovementBaseType> downSampledMovements = new ConcurrentHashMap<>();

    @Schedule(minute = "*/5", hour = "*", persistent = false )
    public void handleDownSampledMovements() {
        if (downSampledMovements.isEmpty()) {
            return;
        }
        boolean onlyAisFromFishingVessels="true".equalsIgnoreCase(startUp.getSetting("onlyAisFromFishingVessels"));

        LOG.info ("Handle {} downSampledMovements, onlyAisFromFishingVessels:{}", downSampledMovements.size(),onlyAisFromFishingVessels);

        List<MovementBaseType> movements = new ArrayList<>(downSampledMovements.values());
        downSampledMovements.clear();

        if (onlyAisFromFishingVessels) {
            LOG_SAVED_MOVEMENTS.info("---- START logging {} downSampledMovements", movements.size());
            movements.forEach(movement -> LOG_SAVED_MOVEMENTS.info("{}-{}", movement.getMmsi(), movement));
            LOG_SAVED_MOVEMENTS.info("#### END logged {} downSampledMovements ####", movements.size());

        } else {
            exchangeService.sendMovements(movements);
        }
    }

    public Map<String, MovementBaseType> getDownSampledMovements() {
        return downSampledMovements;
    }

}
