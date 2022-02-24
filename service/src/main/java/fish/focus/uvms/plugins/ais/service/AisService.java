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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Gauge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.uvms.ais.AISConnection;
import eu.europa.ec.fisheries.uvms.ais.AISConnectionFactoryImpl;
import fish.focus.uvms.plugins.ais.StartupBean;

@Singleton
@Startup
@DependsOn({"StartupBean"})
public class AisService {

    private static final Logger LOG = LoggerFactory.getLogger(AisService.class);

    @Inject
    StartupBean startUp;

    AISConnection connection;

    @EJB
    ProcessService processService;
    
    @Inject
    private DownsamplingService downsamplingService;
    
    @Inject
    private ExchangeService exchangeService;
    
    @Resource
    private ManagedExecutorService executorService;

    private List<CompletableFuture<Void>> processes = new ArrayList<>();

    private List<MovementBaseType> failedSendList = new ArrayList<>();
    private Set<String> knownFishingVessels = new HashSet<>();

    @PostConstruct
    public void init() {
        try {
            Context ctx = new InitialContext();
            AISConnectionFactoryImpl factory = (AISConnectionFactoryImpl) ctx.lookup("java:/eis/AISConnectionFactory");
            if (factory != null) {
                LOG.debug("Factory lookup done! {}, {}", factory.toString(), factory.getClass());
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
            List<String> sentences = connection.getSentences();
            CompletableFuture<Void> process = CompletableFuture.supplyAsync(() -> processService.processMessages(sentences, knownFishingVessels), executorService)
                    .thenAccept(result -> {
                        downsamplingService.getDownSampledMovements().putAll(result.getDownsampledMovements());
                        downsamplingService.getStoredAssetInfo().putAll(result.getDownsampledAssets());
                        }
                    );
            processes.add(process);
            LOG.info("Got {} sentences from AIS RA. Currently running {} parallel threads", sentences.size(), processes.size());
        }
    }

    @Schedule(minute = "*/15", hour = "*", persistent = false)
    public void resend(Timer timer) {
        try {
            if (startUp.isRegistered()) {
                List<MovementBaseType> list = getAndClearFailedMovementList();
                exchangeService.sendToExchange(list, startUp.getRegisterClassName());
            }
        } catch (Exception e) {
            LOG.error(e.toString(), e);
        }
    }

    public void addCachedMovement(MovementBaseType movementBaseType) {
        synchronized (failedSendList) {
            failedSendList.add(movementBaseType);
        }
    }

    public List<MovementBaseType> getAndClearFailedMovementList() {
        List<MovementBaseType> tmp = new ArrayList<>();
        synchronized (failedSendList) {
            tmp.addAll(failedSendList);
            failedSendList.clear();
        }
        return tmp;
    }

    public Set<String> getKnownFishingVessels(){
        return knownFishingVessels;
    }

    @Gauge(unit = MetricUnits.NONE, name = "ais_knownfishingvessels_size", absolute = true)
    public int getKnownFishingVesselsSize() {
        return knownFishingVessels.size();
    }
}