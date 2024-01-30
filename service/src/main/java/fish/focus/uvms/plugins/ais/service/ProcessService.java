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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.uvms.ais.Sentence;
import fish.focus.uvms.asset.client.AssetClient;
import fish.focus.uvms.asset.client.model.AssetDTO;
import fish.focus.uvms.asset.client.model.AssetIdentifier;
import fish.focus.uvms.plugins.ais.StartupBean;
import fish.focus.uvms.plugins.ais.mapper.AisParser;
import fish.focus.uvms.plugins.ais.mapper.AisParser.AisType;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

@Stateless
public class ProcessService {

    private static final Logger LOG = LoggerFactory.getLogger(ProcessService.class);

    @Inject
    private StartupBean startUp;
    
    @Inject
    private ExchangeService exchangeService;

    @Inject
    private AssetClient assetClient;

    private LoadingCache fishingVesselCache;

    public ProcessService() {
        fishingVesselCache = CacheBuilder.newBuilder()
                                         .maximumSize(1000)
                                         .expireAfterWrite(Duration.ofMinutes(5L))
                                         .build(new CacheLoader<AssetDTO, AssetDTO>() {
                                            public AssetDTO load(AssetDTO assetReport) {
                                                if (StringUtils.isNotBlank(assetReport.getMmsi())) {
                                                    return assetClient.getAssetById(AssetIdentifier.MMSI, assetReport.getMmsi());
                                                }
                                                return assetReport;
                                            }
                                         });
    }

    public ProcessResult processMessages(List<Sentence> sentences, Set<String> knownFishingVessels) {
        long start = System.currentTimeMillis();

        Map<String, MovementBaseType> downsampledMovements = new HashMap<>();
        Map<String, MovementBaseType> downSampledFishingVesselMovements = new HashMap<>();
        Map<String, AssetDTO> downsampledAssets = new HashMap<>();
        // collect
        for (Sentence sentence : sentences) {
            try {
                String binary = symbolToBinary(sentence.getSentence());
                AisType aisType = AisParser.parseAisType(binary);
                Instant lesTimestamp = null;
                if (sentence.hasValidCommentBlock()) {
                    lesTimestamp = sentence.getCommentBlockLesTimestamp();
                }
                if (aisType.isPositionReport()) {
                    MovementBaseType movement = AisParser.parsePositionReport(binary, aisType, lesTimestamp);

                    if (movement != null) {
                        if (knownFishingVessels.contains(movement.getMmsi())) {
                            downSampledFishingVesselMovements.put(movement.getMmsi(), movement);
                        } else {
                            downsampledMovements.put(movement.getMmsi(), movement);
                        }
                    }
                } else if (aisType.isStaticReport()) {
                    AssetDTO asset = AisParser.parseStaticReport(binary, aisType);
                    if (asset != null) {
                        downsampledAssets.put(asset.getMmsi(), asset);
	                    addFishingVessels(asset, knownFishingVessels);
                    } else {
			           LOG.error("Couldn't get asset from ais static report, ignoring it");
					}
                }
            } catch (Exception e) {
                exchangeService.sendToErrorQueueParsingError(sentence.getSentence());
                LOG.error("Could not parse AIS message {}", sentence, e);
            }
        }
        LOG.info("Processing time: {} for {} sentences", (System.currentTimeMillis() - start), sentences.size());
        return new ProcessResult(downsampledMovements, downSampledFishingVesselMovements, downsampledAssets);
    }

    private void addFishingVessels(AssetDTO assetReport, Set<String> knownFishingVessels) {
        AssetDTO asset  = assetReport;
        try {
            asset = (AssetDTO) fishingVesselCache.get(assetReport);
        } catch (ExecutionException e) {
            LOG.info("Cannot load asset from cache", e);
        }
        if ((asset.getVesselType() != null && asset.getVesselType().equals("Fishing")) || Boolean.TRUE.equals(asset.getActive())) {
            knownFishingVessels.add(asset.getMmsi());
        } else if (knownFishingVessels.contains(asset.getMmsi()) && asset.getVesselType() != null) {
            LOG.debug("Removing mmsi {} as fishing vessel, is now {}", asset.getMmsi(), asset.getVesselType());
            knownFishingVessels.remove(asset.getMmsi());
        }

    }

    private String symbolToBinary(String symbolString) {
        try {
            StringBuilder sb = new StringBuilder();
            switch (symbolString.charAt(0)) {
                case '1': // message id 1
                case '2': // message id 2
                case '3': // message id 3
                case '5': // message id 5
                case 'B': // message id 18
                case 'H': // message id 24
                    for (int i = 0; i < symbolString.length(); i++) {
                        sb.append(Conversion.getBinaryForSymbol(symbolString.charAt(i)));
                    }
                    return sb.toString();
                default:
            }
        } catch (Exception e) {
            LOG.info("Failed to parse {}", symbolString, e);
        }
        return null;
    }
}