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
import fish.focus.uvms.ais.AISConnectionFactory;
import fish.focus.uvms.ais.Sentence;
import fish.focus.uvms.inject.Managed;
import fish.focus.uvms.plugins.ais.StartupBean;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.DependsOn;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.resource.ResourceException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static java.time.temporal.ChronoUnit.MINUTES;

@Singleton
@Startup
@DependsOn({"StartupBean"})
public class AisService {

    private static final Logger LOG = LoggerFactory.getLogger(AisService.class);

    private final List<CompletableFuture<Void>> processes = new ArrayList<>();
    private final Set<String> knownFishingVessels = new HashSet<>();

    private StartupBean startUp;
    private ProcessService processService;
    private DownsamplingService downsamplingService;
    private DownsamplingFishingService downsamplingFishingService;
    private DownsamplingAssetService downsamplingAssetService;
    private ManagedExecutorService executorService;
    private AISConnectionFactory factory;

    private AISConnection connection;

    /**
     * Used for keeping track of reconnect attempts for the "socket stuck" problem
     */
    private int numberOfReconnectAttempts = 0;

    /**
     * Used when the socket has gotten "stuck".
     */
    Instant lastConnectionAttempt;

    int shortBackOffTime = 1;
    int longBackOffTime = 10;
    ChronoUnit backOffUnit = MINUTES;

    public AisService() {
    }

    @Inject
    public AisService(StartupBean startUp, ProcessService processService, DownsamplingService downsamplingService,
                      DownsamplingFishingService downsamplingFishingService, DownsamplingAssetService downsamplingAssetService,
                      @Managed ManagedExecutorService executorService, @Managed AISConnectionFactory factory) {
        this.startUp = startUp;
        this.processService = processService;
        this.downsamplingService = downsamplingService;
        this.downsamplingFishingService = downsamplingFishingService;
        this.downsamplingAssetService = downsamplingAssetService;
        this.executorService = executorService;
        this.factory = factory;
    }

    @PostConstruct
    public void init() {
        LOG.debug("AisService init");
        try {
            if (factory == null) {
                return;
            }

            connection = factory.getConnection();

            if (connection == null || connection.isOpen()) {
                return;
            }

            if (!startUp.isEnabled()) {
                return;
            }

            connect();
        } catch (ResourceException e) {
            LOG.error("Exception during init: ", e);
        }
    }

    private void connect() {
        lastConnectionAttempt = Instant.now();

        String host = startUp.getSetting("HOST");
        int port = Integer.parseInt(startUp.getSetting("PORT"));
        String username = startUp.getSetting("USERNAME");
        String password = startUp.getSetting("PASSWORD");

        LOG.info("Trying to connect to {}:{}", host, port);
        connection.open(host, port, username, password);
    }

    @PreDestroy
    public void destroy() {
        LOG.debug("Shutting down AisService");
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
    public void connectAndRetrieve() {
        if (isConnectionDown()) {
            return;
        }

        processes.removeIf(process -> process.isDone() || process.isCancelled());

        List<Sentence> sentences = connection.getSentences();
        startUp.incrementAisIncomingAll(sentences.size());
        CompletableFuture<Void> process = CompletableFuture.supplyAsync(() -> processService.processMessages(sentences, knownFishingVessels), executorService)
                .thenAccept(result -> {
                            downsamplingService.getDownSampledMovements().putAll(result.getDownsampledMovements());
                            downsamplingAssetService.getStoredAssetInfo().putAll(result.getDownsampledAssets());
                            downsamplingFishingService.getDownSampledFishingVesselMovements().putAll(result.getDownSampledFishingVesselMovements());
                        }
                );
        processes.add(process);
        LOG.info("Got {} sentences from AIS RA. Currently running {} parallel threads", sentences.size(), processes.size());

        if (!sentences.isEmpty()) {
            // reconnecting worked and are now receiving messages again
            numberOfReconnectAttempts = 0;
        }

        if (sentences.isEmpty() && shouldTryToReconnect()) {
            // no new data was sent. This might indicate the "socket stuck" problem
            LOG.warn("No new data received. Reconnecting socket.");
            reconnect();
        }
    }

    private boolean isConnectionDown() {
        if (!startUp.isEnabled()) {
            destroy();
            return true;
        }

        if (connection != null && !connection.isOpen()) {
            connect();
        }

        if (connection == null || !connection.isOpen()) {
            LOG.warn("Connection was down. Reconnecting again.");
            reconnect();
        }

        if (connection == null || !connection.isOpen()) {
            // failed to init a new connection above => try again later
            LOG.warn("Failed to init an AIS connection");
            return true;
        }

        return false;
    }

    /**
     * Back off 1 min between connection retries for the first 5 tries. Then wait 10min before retrying again.
     *
     * @return true if the connection should be reconnected. False otherwise.
     */
    private boolean shouldTryToReconnect() {
        var now = Instant.now();

        int backOffTime = longBackOffTime;

        if (numberOfReconnectAttempts < 5) {
            backOffTime = shortBackOffTime;
        }

        Instant earliestNextAttempt = lastConnectionAttempt.plus(backOffTime, backOffUnit);
        // is before or equal
        boolean shouldTryReconnect = now.isAfter(earliestNextAttempt);
        LOG.info("{} connection attempts. Last attempt at {}. Earliest next attempt at {}. Now is {}. Reconnect now = {}",
                numberOfReconnectAttempts, lastConnectionAttempt, earliestNextAttempt, now, shouldTryReconnect);
        return shouldTryReconnect;
    }

    private void reconnect() {
        if (connection != null) {
            connection.close();
        }
        numberOfReconnectAttempts += 1;
        init();
    }

    public Set<String> getKnownFishingVessels() {
        return knownFishingVessels;
    }

    @Gauge(unit = MetricUnits.NONE, name = "ais_knownfishingvessels_size", absolute = true)
    public int getKnownFishingVesselsSize() {
        return knownFishingVessels.size();
    }
}