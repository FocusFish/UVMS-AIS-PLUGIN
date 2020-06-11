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
package eu.europa.ec.fisheries.uvms.plugins.ais.mapper;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import eu.europa.ec.fisheries.schema.exchange.module.v1.ExchangeModuleMethod;
import eu.europa.ec.fisheries.schema.exchange.module.v1.ReceiveAssetInformationRequest;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetId;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdList;
import eu.europa.ec.fisheries.schema.exchange.movement.asset.v1.AssetIdType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementBaseType;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementPoint;
import eu.europa.ec.fisheries.schema.exchange.movement.v1.MovementSourceType;
import eu.europa.ec.fisheries.uvms.asset.client.model.AssetDTO;
import eu.europa.ec.fisheries.uvms.plugins.ais.service.Conversion;

public class AisParser {

    private static final Logger LOG = LoggerFactory.getLogger(AisParser.class);

    //according to: http://emsa.europa.eu/cise-documentation/cise-data-model-1.5.3/model/guidelines/687507181.html
    private static final int AIS_SPEED_ERROR_CODE = 1023;
    
    private AisParser() {}
    
    public enum AisType {
        TYPE1(Type.POSITION), 
        TYPE2(Type.POSITION), 
        TYPE3(Type.POSITION), 
        TYPE5(Type.STATIC), 
        TYPE18(Type.POSITION), 
        TYPE24(Type.STATIC),
        UNKNOWN(null);
        
        private Type type;
        
        private AisType(Type type) {
            this.type = type;
        }
        
        public boolean isPositionReport() {
            return Type.POSITION.equals(type);
        }
        
        public boolean isStaticReport() {
            return Type.STATIC.equals(type);
        }
        
        private enum Type {
            POSITION, STATIC;
        }
    }
    
    public static AisType parseAisType(String binary) {
        if (binary == null) {
            return AisType.UNKNOWN;
        }
        int messageType = Integer.parseInt(binary.substring(0, 6), 2);
        switch (messageType) {
            case 1:
                return AisType.TYPE1;
            case 2:
                return AisType.TYPE2;
            case 3:
                return AisType.TYPE3;
            case 5:
                return AisType.TYPE5;
            case 18:
                return AisType.TYPE18;
            case 24:
                return AisType.TYPE24;
            default:
                return AisType.UNKNOWN;
        }
    }
    
    public static MovementBaseType parsePositionReport(String binary, AisType aisType) {
        switch (aisType) {
            case TYPE1:
            case TYPE2:
            case TYPE3:
                return parseReportType123(binary);
            case TYPE18:
                return parseReportType18(binary);
            default:
                return null;
        }
    }
    
    public static AssetDTO parseStaticReport(String binary, AisType aisType) {
        switch (aisType) {
            case TYPE5:
                return parseReportType5(binary);
            case TYPE24:
                return parseReportType24(binary);
            default:
                return null;
        }
    }
    
    public static MovementBaseType parseReportType123(String binary) {
        MovementBaseType movement = new MovementBaseType();
        Integer mmsiNumeric = Integer.MIN_VALUE;
        try{
            mmsiNumeric = Integer.parseInt(binary.substring(8, 38), 2);
        }
        catch(NumberFormatException nfe){
            LOG.warn("mmsi is not numeric", nfe);
        }
        String mmsi = String.valueOf(mmsiNumeric);
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));

        movement.setReportedSpeed(parseSpeedOverGround(binary, 50, 60));
        MovementPoint point = getMovementPoint(parseCoordinate(binary, 61, 89), parseCoordinate(binary, 89, 116));
        if (point == null) {
            return null;
        }
        movement.setPosition(point);
        movement.setReportedCourse(parseCourseOverGround(binary, 116, 128));

        String ansi3 = getAnsi3FromMMSI(mmsi);

        String trueHeadingStr = binary.substring(128, 137);
        Integer trueHeading = parseToNumeric("TrueHeading", trueHeadingStr);
        movement.setTrueHeading(trueHeading);
        movement.setPositionTime(getTimestamp(Integer.parseInt(binary.substring(137, 143), 2)));
        movement.setSource(MovementSourceType.AIS);
        movement.setFlagState(ansi3);
        return movement;
    }
 
    public static AssetDTO parseReportType5(String binary) {
        ReceiveAssetInformationRequest req = new ReceiveAssetInformationRequest();
        req.setMethod(ExchangeModuleMethod.RECEIVE_ASSET_INFORMATION);

        Integer mmsiNumeric = Integer.MIN_VALUE;
        try{
            mmsiNumeric = Integer.parseInt(binary.substring(8, 38), 2);
        }
        catch(NumberFormatException nfe){
            LOG.warn("mmsi is not numeric", nfe);
        }
        String mmsi = String.valueOf(mmsiNumeric);

        String vesselName = Conversion.getAsciiStringFromBinaryString(binary.substring(112, 232));
        String ircs = Conversion.getAsciiStringFromBinaryString(binary.substring(70, 112));
        Integer shipType = Integer.parseInt(binary.substring(232, 240), 2);

        String ansi3 = getAnsi3FromMMSI(mmsi);

        AssetDTO assetDTO = new AssetDTO();
        assetDTO.setMmsi(mmsi);

        assetDTO.setName(vesselName);
        assetDTO.setIrcs(ircs);
        assetDTO.setVesselType(Conversion.getShiptypeForCode(shipType));
        assetDTO.setFlagStateCode(ansi3);
        assetDTO.setUpdatedBy("AIS Message Type 5");
        return assetDTO;
    }
    
    public static MovementBaseType parseReportType18(String binary) {

        if (binary == null || binary.trim().length() < 1) {
            return null;
        }
        MovementBaseType movement = new MovementBaseType();

        // mmsi
        Integer mmsiNumeric = Integer.MIN_VALUE;
        try{
            mmsiNumeric = Integer.parseInt(binary.substring(8, 38), 2);
        }
        catch(NumberFormatException nfe){
            LOG.warn("mmsi is not numeric", nfe);
        }
        String mmsi = String.valueOf(mmsiNumeric);
        movement.setMmsi(mmsi);
        movement.setAssetId(getAssetId(mmsi));

        // speedOverGround
        double speedOverGround = parseSpeedOverGround(binary, 46, 56);
        movement.setReportedSpeed(speedOverGround);

        // position  longitude latitude
        MovementPoint point = getMovementPoint(parseCoordinate(binary, 57, 85), parseCoordinate(binary, 85, 112));
        if (point == null) {
            return null;
        }
        movement.setPosition(point);

        // course
        movement.setReportedCourse(parseCourseOverGround(binary, 112, 124));

        // trueHeading
        String trueHeadingStr = binary.substring(124, 133);
        Integer trueHeading = parseToNumeric("TrueHeading", trueHeadingStr);
        movement.setTrueHeading(trueHeading);

        String ansi3 = getAnsi3FromMMSI(mmsi);

        // timestamp
        movement.setPositionTime(getTimestamp(Integer.parseInt(binary.substring(133, 139), 2)));
        movement.setSource(MovementSourceType.AIS);
        movement.setFlagState(ansi3);
        return movement;
    }
    
    public static AssetDTO parseReportType24(String binary) {

        if (binary == null || binary.trim().length() < 1) {
            return null;
        }
        ReceiveAssetInformationRequest req = new ReceiveAssetInformationRequest();
        req.setMethod(ExchangeModuleMethod.RECEIVE_ASSET_INFORMATION);

        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        String vesselName = "";
        Integer shipType = null;
        String ircs = "";
        String ansi3 = null;

        // if partNumber == 0   the rest of the message is interpreted as a Part A
        // if partNumber == 1   the rest of the message is interpreted as a Part B
        // values of 2 and 3 is not allowed
        Integer partNumber = parseToNumeric("Part Number", binary, 38, 40);
        if (partNumber.equals(0)) {
            vesselName = Conversion.getAsciiStringFromBinaryString(binary.substring(40, 160));
        } else if (partNumber.equals(1)) {
            shipType = Integer.parseInt(binary.substring(40,48), 2);
            ircs = Conversion.getAsciiStringFromBinaryString(binary.substring(90, 132));
            ansi3 = getAnsi3FromMMSI(mmsi);
        }

        AssetDTO assetDTO = new AssetDTO();
        assetDTO.setMmsi(mmsi);
        assetDTO.setName(vesselName);
        assetDTO.setIrcs(ircs);
        if(shipType != null) {
            assetDTO.setVesselType(Conversion.getShiptypeForCode(shipType));
        }
        assetDTO.setFlagStateCode(ansi3);
        assetDTO.setUpdatedBy("AIS Message Type 24");
        return assetDTO;

    }
    
    private static String getAnsi3FromMMSI(String mmsi){
        String ansi3 = "ERR";
        if(mmsi != null && mmsi.length() >= 3) {
            String cc = mmsi.substring(0, 3);
            ansi3 = Conversion.getAnsi3ForCountryCode(cc);
        }
        return ansi3;
    }

    private static Double parseCoordinate(String data, int stringStart, int stringEnd) {
        try {
            String byteString = data.substring(stringStart, stringEnd);
            Long i = Long.parseLong(byteString, 2);
            if (byteString.charAt(0) == '1') {
                i -= (1L << byteString.length());
            }
            return (i.doubleValue() / 10000 / 60);
        }catch(NumberFormatException nfe){
            return null;
        }
    }

    private static double parseCourseOverGround(String s, int stringStart, int stringEnd) {
        Integer i = Integer.parseInt(s.substring(stringStart, stringEnd), 2);
        return i.doubleValue() / 10;
    }


    private static Double parseSpeedOverGround(String s, int stringStart, int stringEnd) {
        Integer speedOverGround = Integer.parseInt(s.substring(stringStart, stringEnd), 2);
        if(speedOverGround == AIS_SPEED_ERROR_CODE){
            return null;
        }
        return speedOverGround.doubleValue() / 10;
    }

    private static AssetId getAssetId(String mmsi) {
        AssetId assetId = new AssetId();
        AssetIdList assetIdList = new AssetIdList();
        assetIdList.setIdType(AssetIdType.MMSI);
        assetIdList.setValue(mmsi);
        assetId.getAssetIdList().add(assetIdList);
        return assetId;
    }

    private static MovementPoint getMovementPoint(Double longitude, Double latitude) {

        if (longitude == null || latitude == null || longitude.equals(181d) || latitude.equals(91d)) {
            return null;
        }

        MovementPoint point = new MovementPoint();
        point.setLongitude(longitude);
        point.setLatitude(latitude);
        return point;
    }

    private static Date getTimestamp(Integer utcSeconds) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.MILLISECOND, 0);
        if (utcSeconds != null && utcSeconds >= 0 && utcSeconds < 60) {
            if (utcSeconds >= cal.get(Calendar.SECOND)) {
                cal.add(Calendar.MINUTE, -1);
            }
            cal.set(Calendar.SECOND, utcSeconds);
        }
        return cal.getTime();
    }

    private static Integer parseToNumeric(String fieldName, String str) {
        try {
            return Integer.parseInt(str, 2);
        } catch (NumberFormatException e) {
            LOG.error(fieldName + " is not numeric", e);
            throw e;
        }
    }

    private static Integer parseToNumeric(String fieldName, String sentence, int startPosInclusive, int endPosExclusive) {
        try {
            String str = sentence.substring(startPosInclusive, endPosExclusive);
            return Integer.parseInt(str, 2);
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            LOG.error(fieldName + " parsing error", e);
            throw e;
        }
    }
}
