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

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import fish.focus.uvms.plugins.ais.StartupBean;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.annotation.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import fish.focus.schema.exchange.module.v1.ExchangeModuleMethod;
import fish.focus.schema.exchange.movement.v1.MovementBaseType;
import fish.focus.schema.exchange.movement.v1.SetReportMovementType;
import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.uvms.exchange.model.mapper.ExchangeModuleRequestMapper;
import fish.focus.uvms.asset.client.model.AssetDTO;

@Stateless
public class ExchangeService {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeService.class);
    
    @Resource(mappedName = "java:/ConnectionFactory")
    private ConnectionFactory connectionFactory;

    @Resource(mappedName = "java:/jms/queue/UVMSExchangeEvent")
    private Queue exchangeQueue;

    @Resource(mappedName = "java:/jms/queue/UVMSPluginFailedReport")
    private Queue errorQueue;

    @Inject
    private AisService aisService;

    @Inject
    private StartupBean startupBean;
    
    @Inject
    @Metric(name = "ais_incoming", absolute = true)
    private Counter aisIncoming;
    
    private Jsonb jsonb = JsonbBuilder.create();
    
    public boolean sendAssetUpdates(Collection<AssetDTO> assets) {
        boolean ok = true;
        if (assets==null || assets.isEmpty()){
            return ok;
        }

        String json = jsonb.toJson(assets);
        LOG.trace(json);
        LOG.info("Sending {} ais assets updates to exchange as a json array", assets.size());

        try (Connection connection = connectionFactory.createConnection();
             Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
             MessageProducer producer = session.createProducer(exchangeQueue)
        ) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            // emit
            try {
                String text = ExchangeModuleRequestMapper.createReceiveAssetInformation(json, "AIS", PluginType.OTHER, "AIS Plugin");
                TextMessage message = session.createTextMessage();
                message.setStringProperty("FUNCTION", ExchangeModuleMethod.RECEIVE_ASSET_INFORMATION.toString());
                message.setText(text);
                producer.send(message);
            } catch (RuntimeException e) {
                LOG.error("Couldn't map movement to setreportmovementtype");
                sendToErrorQueueParsingError(json);
            } catch (Exception e) {
                LOG.info("//NOP: {}", e.getLocalizedMessage());
            }
        } catch (JMSException e) {
            LOG.error("couldn't send movement");
            ok = false;
        }
        return ok;
    }



    public void sendMovements(Collection<MovementBaseType> movements) {
        LOG.info("Sending {} positions to exchange", movements.size());
        try (Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(exchangeQueue)
        ) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // emit
            for (MovementBaseType movement : movements) {
                try {
                    SetReportMovementType movementReport = getMovementReport(movement, startupBean.getRegisterClassName());
                    String text = ExchangeModuleRequestMapper.createSetMovementReportRequest(movementReport, "AIS", null, Instant.now(),  PluginType.OTHER, "AIS", null);
                    TextMessage message = session.createTextMessage();
                    message.setStringProperty("FUNCTION", ExchangeModuleMethod.SET_MOVEMENT_REPORT.value());
                    message.setText(text);
                    producer.send(message);
                    aisIncoming.inc();
                } catch (RuntimeException e) {
                    LOG.error("Couldn't map movement to setreportmovementtype");
                    sendToErrorQueueParsingError(movement.toString());
                } catch (JMSException e) {
                    // save it and try again in a scheduled thread
                    aisService.addCachedMovement(movement);
                } catch (Exception e) {
                    LOG.info("//NOP: {}", e.getLocalizedMessage());
                }
            }
        } catch (JMSException e) {
            LOG.error("couldn't send movement");
        }
    }
    
    public void sendToErrorQueueParsingError(String movement) {
        try (Connection connection = connectionFactory.createConnection();
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(errorQueue)) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);

            // emit

            try {
                BytesMessage message_bytes = session.createBytesMessage();
                message_bytes.setStringProperty("source", "AIS");
                message_bytes.setStringProperty("type", "byte");
                message_bytes.writeBytes(movement.getBytes());
                producer.send(message_bytes);
            } catch (Exception e) {
                LOG.info("//NOP: {}", e.getLocalizedMessage());
            }
        } catch (JMSException e) {
            LOG.error("couldn't send movement");
        }
    }
    
    private SetReportMovementType getMovementReport(MovementBaseType movement, String pluginName) {
        SetReportMovementType report = new SetReportMovementType();
        report.setTimestamp(new Date());
        report.setPluginName(pluginName);
        report.setPluginType(PluginType.OTHER);
        report.setMovement(movement);
        return report;
    }
}
