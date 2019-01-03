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
package eu.europa.ec.fisheries.uvms.plugins.ais.service;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementComChannelType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.SetReportMovementType;
import eu.europa.ec.fisheries.schema.exchange.plugin.types.v1.PluginType;
import eu.europa.ec.fisheries.uvms.plugins.ais.StartupBean;

/**
 **/
@LocalBean
@Stateless
public class ProcessService {

    final static Logger LOG = LoggerFactory.getLogger(ProcessService.class);
    boolean detailLog = true;

    @Inject
    ExchangeService exchangeService;

    @Inject
    StartupBean startUp;

    @Inject
    private Conversion conversion;

    @Asynchronous
    public Future<Long> processMessages(List<String> sentences) {
        long start = System.currentTimeMillis();
        for (String sentence : sentences) {
            String binary = symbolToBinary(sentence);
            if (binary != null) {
                try {
                    MovementBaseType movement = binaryToMovement(binary);
                    if (movement != null) {
                        saveData(movement);
                    }
                } catch (NumberFormatException e) {
                    LOG.info("Got badly formed message: {}", binary);
                } catch (Exception e) {
                    LOG.info("//NOP: {}", e.getLocalizedMessage());
                }
            }
        }
        LOG.info("Processing time: {} for {} sentences", (System.currentTimeMillis() - start), sentences.size());
        return new AsyncResult<Long>(new Long(System.currentTimeMillis() - start));
    }

    private String symbolToBinary(String symbolString) {
        try {
            StringBuilder sb = new StringBuilder();

            switch (symbolString.charAt(0)) {
                case '0': // message id 0
                case '1': // message id 1
                case '2': // message id 2
                case 'B': // message id 18
                    for (int i = 0; i < symbolString.length(); i++) {
                        sb.append(conversion.getBinaryForSymbol(symbolString.charAt(i)));
                    }
                    return sb.toString();
            }
        } catch (Exception e) {
            LOG.info("//NOP: {}", e.getLocalizedMessage());
        }
        return null;
    }

    private MovementBaseType binaryToMovement(String sentence) {
        if (sentence == null || sentence.length() < 144) {
            return null;
        }

        try {
            int id = Integer.parseInt(sentence.substring(0, 6), 2);
            // Check that the type of msg is a valid postion msg print for info. Should already be handled.
            LOG.info("Sentence: {}", sentence);
            switch (id) {
                case 0:
                case 1:
                case 2:
                    return parseReport(sentence);
                case 18:
                    // MSG ID 18 Class B Equipment Position report
                    return parseClassBEquipmentPositionReport(sentence);
                default:
                    return null;
            }
        } catch (NumberFormatException e) {
            LOG.error("Error when reading binary data. {}", e.getMessage());
        }

        return null;
    }

    private MovementBaseType parseReport(String sentence) {
        MovementBaseType movement = new MovementBaseType();
        String mmsi = String.valueOf(Integer.parseInt(sentence.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));
        movement.setReportedSpeed(parseSpeedOverGround(sentence, 50, 60));
        movement.setPosition(getMovementPoint(parseCoordinate(sentence, 61, 89), parseCoordinate(sentence, 89, 116)));
        movement.setReportedCourse(parseCourseOverGround(sentence, 116, 128));
        movement.setPositionTime(getTimestamp(Integer.parseInt(sentence.substring(137, 143), 2)));
        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        return movement;
    }



    private MovementBaseType parseClassBEquipmentPositionReport(String sentence) throws NumberFormatException {

        if(sentence == null || sentence.trim().length() < 1) {
            return null;
        }
        int sentenceLength = sentence.length();

        MovementBaseType movement = new MovementBaseType();
        String mmsi = String.valueOf(Integer.parseInt(sentence.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));
        movement.setReportedSpeed(parseSpeedOverGround(sentence, 46, 56));
        movement.setPosition(getMovementPoint(parseCoordinate(sentence, 57, 85), parseCoordinate(sentence, 85, 112)));
        movement.setReportedCourse(parseCourseOverGround(sentence, 112, 124));
        movement.setPositionTime(getTimestamp(Integer.parseInt(sentence.substring(133, 139), 2)));
        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        // within previous length of sentence parse
        // type b = boolean
        String positionAccuracyStr = sentence.substring(56,57);
        // type = u unsigned integer
        String trueHeadingStr = sentence.substring(124,133);
        Integer trueHeading = parseToNumertic("TrueHeading", trueHeadingStr);


        if(detailLog) {
            LOG.info("positionAccuracy      " + positionAccuracyStr);
            LOG.info("trueHeading           " + trueHeading);
        }



        String assetName = null;
        String typeOfShipAndCargoStr =  null;
        String dimensionToBowStr =  null;
        String dimensionToSternStr = null;
        String dimensionToPortStr = null;
        String dimensionToStarBoardStr = null;
        String positionFixType = null;
        String RAIMFlag = null;
        String DTE = null;
        String assignedModeFlagStr = null;

        if(sentenceLength >= 309) {

            assetName = sentence.substring(143, 263);
            movement.setAssetName(assetName);

            // type = u unsigned integer
            typeOfShipAndCargoStr = sentence.substring(263, 271);
            Integer typeOfShipAndCargo = parseToNumertic("Type of ship and cargo", typeOfShipAndCargoStr);
            movement.setTypeOfShipAndCargo(typeOfShipAndCargo);

            // type = u unsigned integer
            dimensionToBowStr = sentence.substring(271, 280);
            Integer dimensionToBow = parseToNumertic("DimensionToBow", dimensionToBowStr);
            movement.setDimensionToBow(dimensionToBow);

            // type = u unsigned integer
            dimensionToSternStr = sentence.substring(280, 289);
            Integer dimensionToStern = parseToNumertic("DimensionToStern", dimensionToSternStr);
            movement.setDimensionToStern(dimensionToStern);

            // type = u unsigned integer
            dimensionToPortStr = sentence.substring(289, 295);
            Integer dimensionToPort = parseToNumertic("DimensionToPort", dimensionToPortStr);
            movement.setDimensionToPort(dimensionToPort);

            // type = u unsigned integer
            dimensionToStarBoardStr = sentence.substring(295, 301);
            Integer dimensionToStarBoard = parseToNumertic("DimensionToStarBoard", dimensionToStarBoardStr);
            movement.setDimensionToStarBoard(dimensionToStarBoard);

            // type = e enumerated type
            positionFixType = sentence.substring(301, 305);
            movement.setPositionFixType(positionFixType);

            // type = b
            RAIMFlag = sentence.substring(305, 306);
            Boolean rf = RAIMFlag == null ? false : RAIMFlag.equals("1");
            movement.setRAIMFlag(rf);

            // type = b
            DTE = sentence.substring(306, 307);
            Boolean dte = DTE == null ? false : DTE.equals("1");
            movement.setDTE(dte);


            assignedModeFlagStr = sentence.substring(307, 308);
            Boolean amf = assignedModeFlagStr == null ? false : assignedModeFlagStr.equals("1");
            movement.setAssignedModeFlag(amf);



            if(detailLog) {
                LOG.info("assetName             " + assetName);
                LOG.info("typeOfShipAndCargo    " + typeOfShipAndCargo);
                LOG.info("dimensionToBow        " + dimensionToBow);
                LOG.info("dimensionToStern      " + dimensionToStern);
                LOG.info("dimensionToPort       " + dimensionToPort);
                LOG.info("dimensionToStarBoard  " + dimensionToStarBoard);
                LOG.info("positionFixType       " + positionFixType);
                LOG.info("RAIMFlag              " + RAIMFlag);
                LOG.info("DTE                   " + DTE);
                LOG.info("AssignedModeFlag      " + assignedModeFlagStr);
            }
        }

        return movement;
    }

    private MovementBaseType parseClassBEquipmentPositionReportOLD(String sentence) throws NumberFormatException {
        MovementBaseType movement = new MovementBaseType();
        String mmsi = String.valueOf(Integer.parseInt(sentence.substring(8, 38), 2));
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));
        movement.setReportedSpeed(parseSpeedOverGround(sentence, 46, 56));
        movement.setPosition(getMovementPoint(parseCoordinate(sentence, 57, 85), parseCoordinate(sentence, 85, 112)));
        movement.setReportedCourse(parseCourseOverGround(sentence, 112, 124));
        movement.setPositionTime(getTimestamp(Integer.parseInt(sentence.substring(133, 139), 2)));
        movement.setSource(MovementSourceType.AIS);
        movement.setComChannelType(MovementComChannelType.NAF);

        return movement;
    }

    private Double parseCoordinate(String data, int stringStart, int stringEnd) throws NumberFormatException {
        Integer i = Integer.parseInt(data.substring(stringStart, stringEnd), 2);
        return (i.doubleValue() / 10000 / 60);
    }

    private double parseCourseOverGround(String s, int stringStart, int stringEnd) throws NumberFormatException {
        Integer i = Integer.parseInt(s.substring(stringStart, stringEnd), 2);
        return i.doubleValue() / 10;
    }

    private double parseSpeedOverGround(String s, int stringStart, int stringEnd) throws NumberFormatException {
        Integer speedOverGround = Integer.parseInt(s.substring(stringStart, stringEnd), 2);
        return speedOverGround.doubleValue() / 10;
    }

    void saveData(MovementBaseType movement) {
        SetReportMovementType movementReport = getMovementReport(movement);
        exchangeService.sendMovementReportToExchange(movementReport);
    }

    private AssetId getAssetId(String mmsi) {
        AssetId assetId = new AssetId();
        AssetIdList assetIdList = new AssetIdList();
        assetIdList.setIdType(AssetIdType.MMSI);
        assetIdList.setValue(mmsi);
        assetId.getAssetIdList().add(assetIdList);
        return assetId;
    }

    SetReportMovementType getMovementReport(MovementBaseType movement) {
        SetReportMovementType report = new SetReportMovementType();
        report.setTimestamp(getTimestamp());
        report.setPluginName(startUp.getRegisterClassName());
        report.setPluginType(PluginType.OTHER);
        report.setMovement(movement);
        return report;
    }

    private MovementPoint getMovementPoint(double longitude, double latitude) {
        MovementPoint point = new MovementPoint();
        point.setLongitude(longitude);
        point.setLatitude(latitude);
        return point;
    }

    Date getTimestamp() {
        return getTimestamp(null);
    }

    private Date getTimestamp(Integer utcSeconds) {
        GregorianCalendar cal = (GregorianCalendar) GregorianCalendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        if (utcSeconds != null) {
            cal.set(GregorianCalendar.SECOND, utcSeconds);
        }

        return cal.getTime();
    }

    private Integer parseToNumertic(String fieldName , String str) throws NumberFormatException
    {
        try{
            return Integer.parseInt(str,2);
        }
        catch(NumberFormatException e){
            LOG.error(fieldName + " is not numeric", e);
            throw e;
        }
    }



}