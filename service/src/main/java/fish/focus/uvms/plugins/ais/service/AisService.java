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

import fish.focus.uvms.ais.AISConnection;
import fish.focus.uvms.ais.AISConnectionFactoryImpl;
import fish.focus.uvms.ais.Sentence;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.*;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Singleton
@Startup
@DependsOn({"StartupBean"})
public class AisService {

    private static final Logger LOG = LoggerFactory.getLogger(AisService.class);

    private final List<CompletableFuture<Void>> processes = new ArrayList<>();
    private final Set<String> knownFishingVessels = new HashSet<>();

    @Inject
    StartupBean startUp;

    @Inject
    private FailedMovementsService failedMovementsService;

    @EJB
    private ProcessService processService;

    @Inject
    private DownsamplingService downsamplingService;

    @Inject
    private DownsamplingFishingService downsamplingFishingService;

    @Inject
    private DownsamplingAssetService downsamplingAssetService;

    @Resource
    private ManagedExecutorService executorService;

    private AISConnection connection;

    @PostConstruct
    public void init() {
        try {
            Context ctx = new InitialContext();
            AISConnectionFactoryImpl factory = (AISConnectionFactoryImpl) ctx.lookup("java:/eis/AISConnectionFactory");
            if (factory != null) {
                LOG.debug("Factory lookup done! {}, {}", factory, factory.getClass());
                connection = factory.getConnection();

                if (startUp.isEnabled() && connection != null && !connection.isOpen()) {
                    String host = startUp.getSetting("HOST");
                    int port = Integer.parseInt(startUp.getSetting("PORT"));
                    String username = startUp.getSetting("USERNAME");
                    String password = startUp.getSetting("PASSWORD");

                    connection.open(host, port, username, password);
                }
            }
        } catch (NamingException | ResourceException e) {
            LOG.error("Exception: {}", e);
        }
    }

    @PreDestroy
    public void destroy() {
        if (connection != null) {
            connection.close();
        }
        Iterator<CompletableFuture<Void>> processIterator = processes.iterator();
        while (processIterator.hasNext()) {
            CompletableFuture<Void> process = processIterator.next();
            if (process.isDone() || process.isCancelled()) {
                processIterator.remove();
            } else {
                try {
                    process.get(15, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.error("Error during destroy: {}", e.getMessage());
                }
                process.cancel(true);
            }
        }
    }

    @Schedule(second = "*/15", minute = "*", hour = "*", persistent = false)
    public void connectAndRetrive() {
        if (!startUp.isEnabled()) {
            return;
        }
        if (connection != null && !connection.isOpen()) {
            String host = startUp.getSetting("HOST");
            int port = Integer.parseInt(startUp.getSetting("PORT"));
            String username = startUp.getSetting("USERNAME");
            String password = startUp.getSetting("PASSWORD");

            connection.open(host, port, username, password);
        }

        if (connection != null && connection.isOpen()) {
            Iterator<CompletableFuture<Void>> processIterator = processes.iterator();
            while (processIterator.hasNext()) {
                CompletableFuture<Void> process = processIterator.next();
                if (process.isDone() || process.isCancelled()) {
                    processIterator.remove();
                }
            }
            List<Sentence> sentences = connection.getSentences();
            CompletableFuture<Void> process = CompletableFuture.supplyAsync(() -> processService.processMessages(sentences, knownFishingVessels), executorService)
                    .thenAccept(result -> {
                                downsamplingService.getDownSampledMovements().putAll(result.getDownsampledMovements());
                                downsamplingAssetService.getStoredAssetInfo().putAll(result.getDownsampledAssets());
                                downsamplingFishingService.getDownSampledFishingVesselMovements().putAll(result.getDownSampledFishingVesselMovements());
                            }
                    );
            processes.add(process);
            LOG.info("Got {} sentences from AIS RA. Currently running {} parallel threads", sentences.size(), processes.size());
        }
    }

    public Set<String> getKnownFishingVessels() {
        return knownFishingVessels;
    }

    @Gauge(unit = MetricUnits.NONE, name = "ais_knownfishingvessels_size", absolute = true)
    public int getKnownFishingVesselsSize() {
        return knownFishingVessels.size();
    }
}