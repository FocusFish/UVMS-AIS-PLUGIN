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
package fish.focus.uvms.plugins.ais;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import fish.focus.schema.exchange.plugin.types.v1.PluginType;
import fish.focus.schema.exchange.registry.v1.ExchangeRegistryMethod;
import fish.focus.schema.exchange.registry.v1.RegisterServiceRequest;
import fish.focus.schema.exchange.service.v1.CapabilityType;
import fish.focus.schema.exchange.service.v1.CapabilityTypeType;
import fish.focus.schema.exchange.service.v1.ServiceType;
import fish.focus.uvms.exchange.model.constant.ExchangeModelConstants;
import fish.focus.uvms.plugins.ais.PluginDataHolder;
import fish.focus.uvms.plugins.ais.StartupBean;
import fish.focus.uvms.plugins.ais.producer.PluginMessageProducer;
import fish.focus.uvms.plugins.ais.service.FileHandlerBean;

public class TestStartupBean {

    private final static String APP_GROUP_ID = "eu.europa.ec.fisheries.uvms.plugins.ais";
    private final static String APP_NAME = "ais";
    private final static String APP_RESPONSE_TOPIC = "PLUGIN_RESPONSE";

    StartupBean startupBean = new StartupBean();
    PluginMessageProducer messageProducer = Mockito.mock(PluginMessageProducer.class);
    FileHandlerBean fileHandler = Mockito.mock(FileHandlerBean.class);
    Properties capabilities = mockCapabilities();
    Properties pluginProperties = mockPluginProperties();
    
    @Before
    public void setup() {
        startupBean.messageProducer = messageProducer;
        startupBean.fileHandler = fileHandler;

        Mockito.when(fileHandler.getPropertiesFromFile(PluginDataHolder.PLUGIN_PROPERTIES)).thenReturn(pluginProperties);
        Mockito.when(fileHandler.getPropertiesFromFile(PluginDataHolder.PROPERTIES)).thenReturn(new Properties());
        Mockito.when(fileHandler.getPropertiesFromFile(PluginDataHolder.CAPABILITIES_PROPERTIES)).thenReturn(capabilities);
    }

    @Test
    public void testRegisterPlugin() throws JMSException, JAXBException {
        startupBean.startup();

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(messageProducer, Mockito.times(1)).sendEventBusMessage(messageCaptor.capture(), Mockito.eq(ExchangeModelConstants.EXCHANGE_REGISTER_SERVICE), Mockito.eq(ExchangeRegistryMethod.REGISTER_SERVICE.value()));
        verifyRequest(unmarshallMessage(messageCaptor.getValue()));
    }

    private static void verifyRequest(RegisterServiceRequest request) {
        assertEquals(ExchangeRegistryMethod.REGISTER_SERVICE, request.getMethod());
        verifyService(request.getService());
        verifyCapabilities(request.getCapabilityList().getCapability());
    }
    
    private static void verifyService(ServiceType service) {
        assertEquals(APP_GROUP_ID, service.getServiceClassName());
        assertEquals(APP_NAME, service.getName());
        assertFalse(StringUtils.isBlank(service.getDescription()));
        assertEquals(PluginType.OTHER, service.getPluginType());
        assertEquals(APP_GROUP_ID + APP_RESPONSE_TOPIC, service.getServiceResponseMessageName());
        assertNull(service.getSatelliteType());
    }

    private static void verifyCapabilities(List<CapabilityType> capabilities) {
        assertEquals(5, capabilities.size());
        Map<CapabilityTypeType, String> capabilityMap = getCapabilityMap(capabilities);
        assertEquals("TRUE", capabilityMap.get(CapabilityTypeType.POLLABLE));
        assertEquals("TRUE", capabilityMap.get(CapabilityTypeType.ONLY_SINGLE_OCEAN));
        assertEquals("TRUE", capabilityMap.get(CapabilityTypeType.MULTIPLE_OCEAN));
        assertEquals("TRUE", capabilityMap.get(CapabilityTypeType.CONFIGURABLE));
        assertEquals("TRUE", capabilityMap.get(CapabilityTypeType.SAMPLING));
    }

    private static Map<CapabilityTypeType, String> getCapabilityMap(List<CapabilityType> capabilities) {
        Map<CapabilityTypeType, String> map = new HashMap<>();
        for (CapabilityType capability : capabilities) {
            map.put(capability.getType(), capability.getValue());
        }

        return map;
    }

    private RegisterServiceRequest unmarshallMessage(String message) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(RegisterServiceRequest.class);
        return (RegisterServiceRequest) context.createUnmarshaller().unmarshal(new StringReader(message));
    }

    private Properties mockCapabilities() {
        Properties capabilities = new Properties();
        capabilities.setProperty("POLLABLE", "TRUE");
        capabilities.setProperty("ONLY_SINGLE_OCEAN", "TRUE");
        capabilities.setProperty("MULTIPLE_OCEAN", "TRUE");
        capabilities.setProperty("CONFIGURABLE", "TRUE");
        capabilities.setProperty("SAMPLING", "TRUE");
        return capabilities;
    }

    private Properties mockPluginProperties() {
        Properties pluginProperties = new Properties();
        pluginProperties.setProperty("application.groupid", APP_GROUP_ID);
        pluginProperties.setProperty("application.name", APP_NAME);
        pluginProperties.setProperty("application.responseTopicName", APP_RESPONSE_TOPIC);
        return pluginProperties;
    }

}