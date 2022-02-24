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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Conversion {

    private static final Logger LOG = LoggerFactory.getLogger(Conversion.class);

    private static Map<String, String> symbolMap = buildSymbolMap();
    private static Map<String, Character> binToAsciiMap = buildBinToAsciiMap();
    private static Map<String, CountryCode> countryCodeMap = buildCountryCodeMap();
    private static Map<Integer, String> shipTypeMap = buildShiptypeMap();

    private Conversion() {}
    
    public static String getShiptypeForCode(Integer code){
        if(code == null) return "";
        if((code < 0) || (code > 99)){
            return "No such code : " + code;
        }
        return shipTypeMap.get(code);
    }

    public static String getAnsi3ForCountryCode(String countryCode)  {
        CountryCode cc = countryCodeMap.get(countryCode);
        if(cc == null){
            LOG.warn("Unknown country code: {}", countryCode);
            return "ERR";
        }
        return cc.getAnsi3();
    }

    public static String getBinaryForSymbol(char c)  {
        return symbolMap.get("" + c);
    }

    // decoder for binary6-strings
    public static String getAsciiStringFromBinaryString(String binary) {
        StringBuilder builder = new StringBuilder();
        if (binary == null || binary.length() < 1) {
            return "";
        }
        String[] parts = splitToNChar(binary, 6);
        int len = parts.length;
        if (len < 1) {
            return "";
        }
        for (int pos = 0; pos < len; pos++) {

            String bin = parts[pos];
            Character chr = binToAsciiMap.get(bin);
            if(chr != null){
                builder.append(chr);
            }
        }
        return removeTrailingSnabelA(builder.toString());
    }
    
    private static Map<Integer, String> buildShiptypeMap() {
        Map<Integer, String> shipTypes = new TreeMap<>();
        shipTypes.put(0,"Not available");
        shipTypes.put(1,"Reserved for future use");
        shipTypes.put(2,"Reserved for future use");
        shipTypes.put(3,"Reserved for future use");
        shipTypes.put(4,"Reserved for future use");
        shipTypes.put(5,"Reserved for future use");
        shipTypes.put(6,"Reserved for future use");
        shipTypes.put(7,"Reserved for future use");
        shipTypes.put(8,"Reserved for future use");
        shipTypes.put(9,"Reserved for future use");
        shipTypes.put(10,"Reserved for future use");
        shipTypes.put(11,"Reserved for future use");
        shipTypes.put(12,"Reserved for future use");
        shipTypes.put(13,"Reserved for future use");
        shipTypes.put(14,"Reserved for future use");
        shipTypes.put(15,"Reserved for future use");
        shipTypes.put(16,"Reserved for future use");
        shipTypes.put(17,"Reserved for future use");
        shipTypes.put(18,"Reserved for future use");
        shipTypes.put(19,"Reserved for future use");
        shipTypes.put(20,"WIG"); //Wing in ground
        shipTypes.put(21,"WIG");
        shipTypes.put(22,"WIG");
        shipTypes.put(23,"WIG");
        shipTypes.put(24,"WIG");
        shipTypes.put(25,"WIG");
        shipTypes.put(26,"WIG");
        shipTypes.put(27,"WIG");
        shipTypes.put(28,"WIG");
        shipTypes.put(29,"WIG");
        shipTypes.put(30,"Fishing");
        shipTypes.put(31,"Towing");
        shipTypes.put(32,"Towing");
        shipTypes.put(33,"Dredging");
        shipTypes.put(34,"Diving");
        shipTypes.put(35,"Military");
        shipTypes.put(36,"Sailing");
        shipTypes.put(37,"Pleasure Craft");
        shipTypes.put(38,"Reserved");
        shipTypes.put(39,"Reserved");
        shipTypes.put(40,"HSC");    //High Speed Craft
        shipTypes.put(41,"HSC");
        shipTypes.put(42,"HSC");
        shipTypes.put(43,"HSC");
        shipTypes.put(44,"HSC");
        shipTypes.put(45,"HSC");
        shipTypes.put(46,"HSC");
        shipTypes.put(47,"HSC");
        shipTypes.put(48,"HSC");
        shipTypes.put(49,"HSC");
        shipTypes.put(50,"Pilot");
        shipTypes.put(51,"SAR");
        shipTypes.put(52,"Tug");
        shipTypes.put(53,"Port Tender");
        shipTypes.put(54,"Anti-pollution");
        shipTypes.put(55,"Law Enforcement");
        shipTypes.put(56,"Local Vessel");
        shipTypes.put(57,"Local Vessel");
        shipTypes.put(58,"Medical Transport");
        shipTypes.put(59,"Ships according to RR");
        shipTypes.put(60,"Passenger");
        shipTypes.put(61,"Passenger");
        shipTypes.put(62,"Passenger");
        shipTypes.put(63,"Passenger");
        shipTypes.put(64,"Passenger");
        shipTypes.put(65,"Passenger");
        shipTypes.put(66,"Passenger");
        shipTypes.put(67,"Passenger");
        shipTypes.put(68,"Passenger");
        shipTypes.put(69,"Passenger");
        shipTypes.put(70,"Cargo");
        shipTypes.put(71,"Cargo");
        shipTypes.put(72,"Cargo");
        shipTypes.put(73,"Cargo");
        shipTypes.put(74,"Cargo");
        shipTypes.put(75,"Cargo");
        shipTypes.put(76,"Cargo");
        shipTypes.put(77,"Cargo");
        shipTypes.put(78,"Cargo");
        shipTypes.put(79,"Cargo");
        shipTypes.put(80,"Tanker");
        shipTypes.put(81,"Tanker");
        shipTypes.put(82,"Tanker");
        shipTypes.put(83,"Tanker");
        shipTypes.put(84,"Tanker");
        shipTypes.put(85,"Tanker");
        shipTypes.put(86,"Tanker");
        shipTypes.put(87,"Tanker");
        shipTypes.put(88,"Tanker");
        shipTypes.put(89,"Tanker");
        shipTypes.put(90,"Other");
        shipTypes.put(91,"Other");
        shipTypes.put(92,"Other");
        shipTypes.put(93,"Other");
        shipTypes.put(94,"Other");
        shipTypes.put(95,"Other");
        shipTypes.put(96,"Other");
        shipTypes.put(97,"Other");
        shipTypes.put(98,"Other");
        shipTypes.put(99,"Other");
        
        return shipTypes;
    }

    private static Map<String, CountryCode> buildCountryCodeMap() {
        Map<String, CountryCode> countryCodes = new TreeMap<>();
        /* This mapper is taken from: http://www.vtexplorer.com/mmsi-mid-codes-en/
         * There is a local copy named: LANDSKODER.html in the project folder */
        countryCodes.put("201",new CountryCode("201","Albania","AL","ALB"));
        countryCodes.put("202",new CountryCode("202","Andorra","AD","AND"));
        countryCodes.put("203",new CountryCode("203","Austria","AT","AUT"));
        countryCodes.put("204",new CountryCode("204","Portugal","PT","PRT"));
        countryCodes.put("205",new CountryCode("205","Belgium","BE","BEL"));
        countryCodes.put("206",new CountryCode("206","Belarus","BY","BLR"));
        countryCodes.put("207",new CountryCode("207","Bulgaria","BG","BGR"));
        countryCodes.put("208",new CountryCode("208","Vatican","VA","VAT"));
        countryCodes.put("209",new CountryCode("209","Cyprus","CY","CYP"));
        countryCodes.put("210",new CountryCode("210","Cyprus","CY","CYP"));
        countryCodes.put("211",new CountryCode("211","Germany","DE","DEU"));
        countryCodes.put("212",new CountryCode("212","Cyprus","CY","CYP"));
        countryCodes.put("213",new CountryCode("213","Georgia","GE","GEO"));
        countryCodes.put("214",new CountryCode("214","Moldova","MD","MDA"));
        countryCodes.put("215",new CountryCode("215","Malta","MT","MLT"));
        countryCodes.put("216",new CountryCode("216","Armenia","ZZ","ARM"));
        countryCodes.put("218",new CountryCode("218","Germany","DE","DEU"));
        countryCodes.put("219",new CountryCode("219","Denmark","DK","DNK"));
        countryCodes.put("220",new CountryCode("220","Denmark","DK","DNK"));
        countryCodes.put("224",new CountryCode("224","Spain","ES","ESP"));
        countryCodes.put("225",new CountryCode("225","Spain","ES","ESP"));
        countryCodes.put("226",new CountryCode("226","France","FR","FRA"));
        countryCodes.put("227",new CountryCode("227","France","FR","FRA"));
        countryCodes.put("228",new CountryCode("228","France","FR","FRA"));
        countryCodes.put("229",new CountryCode("229","Malta","MT","MLT"));
        countryCodes.put("230",new CountryCode("230","Finland","FI","FIN"));
        countryCodes.put("231",new CountryCode("231","Faroe Is","FO","FRO"));
        countryCodes.put("232",new CountryCode("232","United Kingdom","GB","GBR"));
        countryCodes.put("233",new CountryCode("233","United Kingdom","GB","GBR"));
        countryCodes.put("234",new CountryCode("234","United Kingdom","GB","GBR"));
        countryCodes.put("235",new CountryCode("235","United Kingdom","GB","GBR"));
        countryCodes.put("236",new CountryCode("236","Gibraltar","GI","GIB"));
        countryCodes.put("237",new CountryCode("237","Greece","GR","GRC"));
        countryCodes.put("238",new CountryCode("238","Croatia","HR","HRV"));
        countryCodes.put("239",new CountryCode("239","Greece","GR","GRC"));
        countryCodes.put("240",new CountryCode("240","Greece","GR","GRC"));
        countryCodes.put("241",new CountryCode("241","Greece","GR","GRC"));
        countryCodes.put("242",new CountryCode("242","Morocco","MA","MAR"));
        countryCodes.put("243",new CountryCode("243","Hungary","HU","HUN"));
        countryCodes.put("244",new CountryCode("244","Netherlands","NL","NLD"));
        countryCodes.put("245",new CountryCode("245","Netherlands","NL","NLD"));
        countryCodes.put("246",new CountryCode("246","Netherlands","NL","NLD"));
        countryCodes.put("247",new CountryCode("247","Italy","IT","ITA"));
        countryCodes.put("248",new CountryCode("248","Malta","MT","MLT"));
        countryCodes.put("249",new CountryCode("249","Malta","MT","MLT"));
        countryCodes.put("250",new CountryCode("250","Ireland","IE","IRL"));
        countryCodes.put("251",new CountryCode("251","Iceland","IS","ISL"));
        countryCodes.put("252",new CountryCode("252","Liechtenstein","LI","LIE"));
        countryCodes.put("253",new CountryCode("253","Luxembourg","LU","LUX"));
        countryCodes.put("254",new CountryCode("254","Monaco","MC","MCO"));
        countryCodes.put("255",new CountryCode("255","Portugal","PT","PRT"));
        countryCodes.put("256",new CountryCode("256","Malta","MT","MLT"));
        countryCodes.put("257",new CountryCode("257","Norway","NO","NOR"));
        countryCodes.put("258",new CountryCode("258","Norway","NO","NOR"));
        countryCodes.put("259",new CountryCode("259","Norway","NO","NOR"));
        countryCodes.put("261",new CountryCode("261","Poland","PL","POL"));
        countryCodes.put("262",new CountryCode("262","Montenegro","ME","MNE"));
        countryCodes.put("263",new CountryCode("263","Portugal","PT","PRT"));
        countryCodes.put("264",new CountryCode("264","Romania","RO","ROU"));
        countryCodes.put("265",new CountryCode("265","Sweden","SE","SWE"));
        countryCodes.put("266",new CountryCode("266","Sweden","SE","SWE"));
        countryCodes.put("267",new CountryCode("267","Slovakia","SK","SVK"));
        countryCodes.put("268",new CountryCode("268","San Marino","SM","SMR"));
        countryCodes.put("269",new CountryCode("269","Switzerland","CH","CHE"));
        countryCodes.put("270",new CountryCode("270","Czech Republic","CZ","CZE"));
        countryCodes.put("271",new CountryCode("271","Turkey","TR","TUR"));
        countryCodes.put("272",new CountryCode("272","Ukraine","UA","UKR"));
        countryCodes.put("273",new CountryCode("273","Russia","RU","RUS"));
        countryCodes.put("274",new CountryCode("274","FYR Macedonia","MK","MKD"));
        countryCodes.put("275",new CountryCode("275","Latvia","LV","LVA"));
        countryCodes.put("276",new CountryCode("276","Estonia","EE","EST"));
        countryCodes.put("277",new CountryCode("277","Lithuania","LT","LTU"));
        countryCodes.put("278",new CountryCode("278","Slovenia","SI","SVN"));
        countryCodes.put("279",new CountryCode("279","Serbia","RS","SRB"));
        countryCodes.put("301",new CountryCode("301","Anguilla","AI","AIA"));
        countryCodes.put("303",new CountryCode("303","USA","US","USA"));
        countryCodes.put("304",new CountryCode("304","Antigua Barbuda","AG","ATG"));
        countryCodes.put("305",new CountryCode("305","Antigua Barbuda","AG","ATG"));
        countryCodes.put("306",new CountryCode("306","Curacao","CW","???"));
        countryCodes.put("307",new CountryCode("307","Aruba","AW","ABW"));
        countryCodes.put("308",new CountryCode("308","Bahamas","BS","BHS"));
        countryCodes.put("309",new CountryCode("309","Bahamas","BS","BHS"));
        countryCodes.put("310",new CountryCode("310","Bermuda","BM","BMU"));
        countryCodes.put("311",new CountryCode("311","Bahamas","BS","BHS"));
        countryCodes.put("312",new CountryCode("312","Belize","BZ","BLZ"));
        countryCodes.put("314",new CountryCode("314","Barbados","BB","BRB"));
        countryCodes.put("316",new CountryCode("316","Canada","CA","CAN"));
        countryCodes.put("319",new CountryCode("319","Cayman Is","KY","CYM"));
        countryCodes.put("321",new CountryCode("321","Costa Rica","CR","CRI"));
        countryCodes.put("323",new CountryCode("323","Cuba","CU","CUB"));
        countryCodes.put("325",new CountryCode("325","Dominica","DM","DMA"));
        countryCodes.put("327",new CountryCode("327","Dominican Rep","DO","DOM"));
        countryCodes.put("329",new CountryCode("329","Guadeloupe","GP","GLP"));
        countryCodes.put("330",new CountryCode("330","Grenada","GD","GRD"));
        countryCodes.put("331",new CountryCode("331","Greenland","GL","GRL"));
        countryCodes.put("332",new CountryCode("332","Guatemala","GT","GTM"));
        countryCodes.put("334",new CountryCode("334","Honduras","HN","HND"));
        countryCodes.put("336",new CountryCode("336","Haiti","HT","HTI"));
        countryCodes.put("338",new CountryCode("338","USA","US","USA"));
        countryCodes.put("339",new CountryCode("339","Jamaica","JM","JAM"));
        countryCodes.put("341",new CountryCode("341","St Kitts Nevis","KN","KNA"));
        countryCodes.put("343",new CountryCode("343","St Lucia","LC","LCA"));
        countryCodes.put("345",new CountryCode("345","Mexico","MX","MEX"));
        countryCodes.put("347",new CountryCode("347","Martinique","MQ","MTQ"));
        countryCodes.put("348",new CountryCode("348","Montserrat","MS","MSR"));
        countryCodes.put("350",new CountryCode("350","Nicaragua","NI","NIC"));
        countryCodes.put("351",new CountryCode("351","Panama","PA","PAN"));
        countryCodes.put("352",new CountryCode("352","Panama","PA","PAN"));
        countryCodes.put("353",new CountryCode("353","Panama","PA","PAN"));
        countryCodes.put("354",new CountryCode("354","Panama","PA","PAN"));
        countryCodes.put("355",new CountryCode("355","Panama","PA","PAN"));
        countryCodes.put("356",new CountryCode("356","Panama","PA","PAN"));
        countryCodes.put("357",new CountryCode("357","Panama","PA","PAN"));
        countryCodes.put("358",new CountryCode("358","Puerto Rico","PR","PRI"));
        countryCodes.put("359",new CountryCode("359","El Salvador","SV","SLV"));
        countryCodes.put("361",new CountryCode("361","St Pierre Miquelon","PM","SPM"));
        countryCodes.put("362",new CountryCode("362","Trinidad Tobago","TT","TTO"));
        countryCodes.put("364",new CountryCode("364","Turks Caicos Is","TC","TCA"));
        countryCodes.put("366",new CountryCode("366","USA","US","USA"));
        countryCodes.put("367",new CountryCode("367","USA","US","USA"));
        countryCodes.put("368",new CountryCode("368","USA","US","USA"));
        countryCodes.put("369",new CountryCode("369","USA","US","USA"));
        countryCodes.put("370",new CountryCode("370","Panama","PA","PAN"));
        countryCodes.put("371",new CountryCode("371","Panama","PA","PAN"));
        countryCodes.put("372",new CountryCode("372","Panama","PA","PAN"));
        countryCodes.put("373",new CountryCode("373","Panama","PA","PAN"));
        countryCodes.put("374",new CountryCode("374","Panama","PA","PAN"));
        countryCodes.put("375",new CountryCode("375","St Vincent Grenadines","VC","VCT"));
        countryCodes.put("376",new CountryCode("376","St Vincent Grenadines","VC","VCT"));
        countryCodes.put("377",new CountryCode("377","St Vincent Grenadines","VC","VCT"));
        countryCodes.put("378",new CountryCode("378","British Virgin Is","VG","VGB"));
        countryCodes.put("379",new CountryCode("379","US Virgin Is","VI","VIR"));
        countryCodes.put("401",new CountryCode("401","Afghanistan","AF","AFG"));
        countryCodes.put("403",new CountryCode("403","Saudi Arabia","SA","SAU"));
        countryCodes.put("405",new CountryCode("405","Bangladesh","BD","BGD"));
        countryCodes.put("408",new CountryCode("408","Bahrain","BH","BHR"));
        countryCodes.put("410",new CountryCode("410","Bhutan","BT","BTN"));
        countryCodes.put("412",new CountryCode("412","China","CN","CHN"));
        countryCodes.put("413",new CountryCode("413","China","CN","CHN"));
        countryCodes.put("414",new CountryCode("414","China","CN","CHN"));
        countryCodes.put("416",new CountryCode("416","Taiwan","TW","TWN"));
        countryCodes.put("417",new CountryCode("417","Sri Lanka","LK","LKA"));
        countryCodes.put("419",new CountryCode("419","India","IN","IND"));
        countryCodes.put("422",new CountryCode("422","Iran","IR","IRN"));
        countryCodes.put("423",new CountryCode("423","Azerbaijan","AZ","AZE"));
        countryCodes.put("425",new CountryCode("425","Iraq","IQ","IRQ"));
        countryCodes.put("428",new CountryCode("428","Israel","IL","ISR"));
        countryCodes.put("431",new CountryCode("431","Japan","JP","JPN"));
        countryCodes.put("432",new CountryCode("432","Japan","JP","JPN"));
        countryCodes.put("434",new CountryCode("434","Turkmenistan","TM","TKM"));
        countryCodes.put("436",new CountryCode("436","Kazakhstan","KZ","KAZ"));
        countryCodes.put("437",new CountryCode("437","Uzbekistan","UZ","UZB"));
        countryCodes.put("438",new CountryCode("438","Jordan","JO","JOR"));
        countryCodes.put("440",new CountryCode("440","Korea","KR","KOR"));
        countryCodes.put("441",new CountryCode("441","Korea","KR","KOR"));
        countryCodes.put("443",new CountryCode("443","Palestine","PS","PSE"));
        countryCodes.put("445",new CountryCode("445","DPR Korea","KP","PRK"));
        countryCodes.put("447",new CountryCode("447","Kuwait","KW","KWT"));
        countryCodes.put("450",new CountryCode("450","Lebanon","LB","LBN"));
        countryCodes.put("451",new CountryCode("451","Kyrgyz Republic","ZZ","KGZ"));
        countryCodes.put("453",new CountryCode("453","Macao","ZZ","???"));
        countryCodes.put("455",new CountryCode("455","Maldives","MV","MDV"));
        countryCodes.put("457",new CountryCode("457","Mongolia","MN","MNG"));
        countryCodes.put("459",new CountryCode("459","Nepal","NP","NPL"));
        countryCodes.put("461",new CountryCode("461","Oman","OM","OMN"));
        countryCodes.put("463",new CountryCode("463","Pakistan","PK","PAK"));
        countryCodes.put("466",new CountryCode("466","Qatar","QA","QAT"));
        countryCodes.put("468",new CountryCode("468","Syria","SY","SYR"));
        countryCodes.put("470",new CountryCode("470","UAE","AE","ARE"));
        countryCodes.put("472",new CountryCode("472","Tajikistan","TJ","TJK"));
        countryCodes.put("473",new CountryCode("473","Yemen","YE","YEM"));
        countryCodes.put("475",new CountryCode("475","Yemen","YE","YEM"));
        countryCodes.put("477",new CountryCode("477","Hong Kong","HK","???"));
        countryCodes.put("478",new CountryCode("478","Bosnia and Herzegovina","BA","BIH"));
        countryCodes.put("501",new CountryCode("501","Antarctica","AQ","ATA"));
        countryCodes.put("503",new CountryCode("503","Australia","AU","AUS"));
        countryCodes.put("506",new CountryCode("506","Myanmar","MM","???"));
        countryCodes.put("508",new CountryCode("508","Brunei","BN","BRN"));
        countryCodes.put("510",new CountryCode("510","Micronesia","FM","FSM"));
        countryCodes.put("511",new CountryCode("511","Palau","PW","PLW"));
        countryCodes.put("512",new CountryCode("512","New Zealand","NZ","NZL"));
        countryCodes.put("514",new CountryCode("514","Cambodia","KH","KHM"));
        countryCodes.put("515",new CountryCode("515","Cambodia","KH","KHM"));
        countryCodes.put("516",new CountryCode("516","Christmas Is","CX","CXR"));
        countryCodes.put("518",new CountryCode("518","Cook Is","CK","COK"));
        countryCodes.put("520",new CountryCode("520","Fiji","FJ","FJI"));
        countryCodes.put("523",new CountryCode("523","Cocos Is","CC","CCK"));
        countryCodes.put("525",new CountryCode("525","Indonesia","ID","IDN"));
        countryCodes.put("529",new CountryCode("529","Kiribati","KI","KIR"));
        countryCodes.put("531",new CountryCode("531","Laos","LA","LAO"));
        countryCodes.put("533",new CountryCode("533","Malaysia","MY","MYS"));
        countryCodes.put("536",new CountryCode("536","N Mariana Is","MP","MNP"));
        countryCodes.put("538",new CountryCode("538","Marshall Is","MH","MHL"));
        countryCodes.put("540",new CountryCode("540","New Caledonia","NC","NCL"));
        countryCodes.put("542",new CountryCode("542","Niue","NU","NIU"));
        countryCodes.put("544",new CountryCode("544","Nauru","NR","NRU"));
        countryCodes.put("546",new CountryCode("546","French Polynesia","TF","PYF"));
        countryCodes.put("548",new CountryCode("548","Philippines","PH","PHL"));
        countryCodes.put("553",new CountryCode("553","Papua New Guinea","PG","PNG"));
        countryCodes.put("555",new CountryCode("555","Pitcairn Is","PN","PCN"));
        countryCodes.put("557",new CountryCode("557","Solomon Is","SB","SLB"));
        countryCodes.put("559",new CountryCode("559","American Samoa","AS","ASM"));
        countryCodes.put("561",new CountryCode("561","Samoa","WS","WSM"));
        countryCodes.put("563",new CountryCode("563","Singapore","SG","SGP"));
        countryCodes.put("564",new CountryCode("564","Singapore","SG","SGP"));
        countryCodes.put("565",new CountryCode("565","Singapore","SG","SGP"));
        countryCodes.put("566",new CountryCode("566","Singapore","SG","SGP"));
        countryCodes.put("567",new CountryCode("567","Thailand","TH","THA"));
        countryCodes.put("570",new CountryCode("570","Tonga","TO","TON"));
        countryCodes.put("572",new CountryCode("572","Tuvalu","TV","TUV"));
        countryCodes.put("574",new CountryCode("574","Vietnam","VN","VNM"));
        countryCodes.put("576",new CountryCode("576","Vanuatu","VU","VUT"));
        countryCodes.put("577",new CountryCode("577","Vanuatu","VU","VUT"));
        countryCodes.put("578",new CountryCode("578","Wallis Futuna Is","WF","WLF"));
        countryCodes.put("601",new CountryCode("601","South Africa","ZA","ZAF"));
        countryCodes.put("603",new CountryCode("603","Angola","AO","AGO"));
        countryCodes.put("605",new CountryCode("605","Algeria","DZ","DZA"));
        countryCodes.put("607",new CountryCode("607","St Paul Amsterdam Is","XX","???"));
        countryCodes.put("608",new CountryCode("608","Ascension Is","IO","???"));
        countryCodes.put("609",new CountryCode("609","Burundi","BI","BDI"));
        countryCodes.put("610",new CountryCode("610","Benin","BJ","BEN"));
        countryCodes.put("611",new CountryCode("611","Botswana","BW","BWA"));
        countryCodes.put("612",new CountryCode("612","Cen Afr Rep","CF","CAF"));
        countryCodes.put("613",new CountryCode("613","Cameroon","CM","CMR"));
        countryCodes.put("615",new CountryCode("615","Congo","CG","COG"));
        countryCodes.put("616",new CountryCode("616","Comoros","KM","COM"));
        countryCodes.put("617",new CountryCode("617","Cape Verde","CV","CPV"));
        countryCodes.put("618",new CountryCode("618","Antarctica","AQ","ATA"));
        countryCodes.put("619",new CountryCode("619","Ivory Coast","CI","???"));
        countryCodes.put("620",new CountryCode("620","Comoros","KM","COM"));
        countryCodes.put("621",new CountryCode("621","Djibouti","DJ","DJI"));
        countryCodes.put("622",new CountryCode("622","Egypt","EG","EGY"));
        countryCodes.put("624",new CountryCode("624","Ethiopia","ET","ETH"));
        countryCodes.put("625",new CountryCode("625","Eritrea","ER","ERI"));
        countryCodes.put("626",new CountryCode("626","Gabon","GA","GAB"));
        countryCodes.put("627",new CountryCode("627","Ghana","GH","GHA"));
        countryCodes.put("629",new CountryCode("629","Gambia","GM","GMB"));
        countryCodes.put("630",new CountryCode("630","Guinea-Bissau","GW","GNB"));
        countryCodes.put("631",new CountryCode("631","Equ. Guinea","GQ","GIN"));
        countryCodes.put("632",new CountryCode("632","Guinea","GN","GIN"));
        countryCodes.put("633",new CountryCode("633","Burkina Faso","BF","BFA"));
        countryCodes.put("634",new CountryCode("634","Kenya","KE","KEN"));
        countryCodes.put("635",new CountryCode("635","Antarctica","AQ","ATA"));
        countryCodes.put("636",new CountryCode("636","Liberia","LR","LBR"));
        countryCodes.put("637",new CountryCode("637","Liberia","LR","LBR"));
        countryCodes.put("642",new CountryCode("642","Libya","LY","LBY"));
        countryCodes.put("644",new CountryCode("644","Lesotho","LS","LSO"));
        countryCodes.put("645",new CountryCode("645","Mauritius","MU","MUS"));
        countryCodes.put("647",new CountryCode("647","Madagascar","MG","MDG"));
        countryCodes.put("649",new CountryCode("649","Mali","ML","MLI"));
        countryCodes.put("650",new CountryCode("650","Mozambique","MZ","MOZ"));
        countryCodes.put("654",new CountryCode("654","Mauritania","MR","MRT"));
        countryCodes.put("655",new CountryCode("655","Malawi","MW","MWI"));
        countryCodes.put("656",new CountryCode("656","Niger","NE","NER"));
        countryCodes.put("657",new CountryCode("657","Nigeria","NG","NGA"));
        countryCodes.put("659",new CountryCode("659","Namibia","NA","NAM"));
        countryCodes.put("660",new CountryCode("660","Reunion","RE","REU"));
        countryCodes.put("661",new CountryCode("661","Rwanda","RW","RWA"));
        countryCodes.put("662",new CountryCode("662","Sudan","SD","SDN"));
        countryCodes.put("663",new CountryCode("663","Senegal","SN","SEN"));
        countryCodes.put("664",new CountryCode("664","Seychelles","SC","SYC"));
        countryCodes.put("665",new CountryCode("665","St Helena","SH","SHN"));
        countryCodes.put("666",new CountryCode("666","Somalia","SO","SOM"));
        countryCodes.put("667",new CountryCode("667","Sierra Leone","SL","SLE"));
        countryCodes.put("668",new CountryCode("668","Sao Tome Principe","ST","STP"));
        countryCodes.put("669",new CountryCode("669","Swaziland","SZ","SWZ"));
        countryCodes.put("670",new CountryCode("670","Chad","TD","TCD"));
        countryCodes.put("671",new CountryCode("671","Togo","TG","TGO"));
        countryCodes.put("672",new CountryCode("672","Tunisia","TN","TUN"));
        countryCodes.put("674",new CountryCode("674","Tanzania","TZ","TZA"));
        countryCodes.put("675",new CountryCode("675","Uganda","UG","UGA"));
        countryCodes.put("676",new CountryCode("676","DR Congo","CD","COD"));
        countryCodes.put("677",new CountryCode("677","Tanzania","TZ","TZA"));
        countryCodes.put("678",new CountryCode("678","Zambia","ZM","ZMB"));
        countryCodes.put("679",new CountryCode("679","Zimbabwe","ZW","ZWE"));
        countryCodes.put("701",new CountryCode("701","Argentina","AR","ARG"));
        countryCodes.put("710",new CountryCode("710","Brazil","BR","BRA"));
        countryCodes.put("720",new CountryCode("720","Bolivia","BO","BOL"));
        countryCodes.put("725",new CountryCode("725","Chile","CL","CHL"));
        countryCodes.put("730",new CountryCode("730","Colombia","CO","COL"));
        countryCodes.put("735",new CountryCode("735","Ecuador","EC","ECU"));
        countryCodes.put("740",new CountryCode("740","UK","UK","GBR"));
        countryCodes.put("745",new CountryCode("745","Guiana","GF","GIN"));
        countryCodes.put("750",new CountryCode("750","Guyana","GY","GUY"));
        countryCodes.put("755",new CountryCode("755","Paraguay","PY","PRY"));
        countryCodes.put("760",new CountryCode("760","Peru","PE","PER"));
        countryCodes.put("765",new CountryCode("765","Suriname","SR","SUR"));
        countryCodes.put("770",new CountryCode("770","Uruguay","UY","URY"));
        countryCodes.put("775",new CountryCode("775","Venezuela","VE","VEN"));
        return countryCodes;
    }


    

    private static String removeTrailingSnabelA(String s) {
        return s.replace("@","").trim();
    }


    /**
     * Split text into n number of characters.
     *
     * @param text the text to be split.
     * @param size the split size.
     * @return an array of the split text.
     */
    private static String[] splitToNChar(String text, int size) {
        List<String> parts = new ArrayList<>();

        int length = text.length();
        for (int i = 0; i < length; i += size) {
            parts.add(text.substring(i, Math.min(length, i + size)));
        }
        return parts.toArray(new String[0]);
    }

    private static Map<String, Character> buildBinToAsciiMap() {
        // Table 3 sixbit ascii
        // http://catb.org/gpsd/AIVDM.html

        Map<String, Character> binToAscii = new TreeMap<>();

        binToAscii.put("000000",'@');
        binToAscii.put("000001",'A');
        binToAscii.put("000010",'B');
        binToAscii.put("000011",'C');
        binToAscii.put("000100",'D');
        binToAscii.put("000101",'E');
        binToAscii.put("000110",'F');
        binToAscii.put("000111",'G');
        binToAscii.put("001000",'H');
        binToAscii.put("001001",'I');
        binToAscii.put("001010",'J');
        binToAscii.put("001011",'K');
        binToAscii.put("001100",'L');
        binToAscii.put("001101",'M');
        binToAscii.put("001110",'N');
        binToAscii.put("001111",'O');


        binToAscii.put("010000",'P');
        binToAscii.put("010001",'Q');
        binToAscii.put("010010",'R');
        binToAscii.put("010011",'S');
        binToAscii.put("010100",'T');
        binToAscii.put("010101",'U');
        binToAscii.put("010110",'V');
        binToAscii.put("010111",'W');
        binToAscii.put("011000",'X');
        binToAscii.put("011001",'Y');
        binToAscii.put("011010",'Z');
        binToAscii.put("011011",'[');
        binToAscii.put("011100",'\\');
        binToAscii.put("011101",']');
        binToAscii.put("011110",'^');
        binToAscii.put("011111",'_');

        binToAscii.put("100000",' ');
        binToAscii.put("100001",'!');
        binToAscii.put("100010",'\"');
        binToAscii.put("100011",'#');
        binToAscii.put("100100",'$');
        binToAscii.put("100101",'%');
        binToAscii.put("100110",'&');
        binToAscii.put("100111",'\'');
        binToAscii.put("101000",'(');
        binToAscii.put("101001",')');
        binToAscii.put("101010",'*');
        binToAscii.put("101011",'+');
        binToAscii.put("101100",',');
        binToAscii.put("101101",'-');
        binToAscii.put("101110",'.');
        binToAscii.put("101111",'/');

        binToAscii.put("110000",'0');
        binToAscii.put("110001",'1');
        binToAscii.put("110010",'2');
        binToAscii.put("110011",'3');
        binToAscii.put("110100",'4');
        binToAscii.put("110101",'5');
        binToAscii.put("110110",'6');
        binToAscii.put("110111",'7');
        binToAscii.put("111000",'8');
        binToAscii.put("111001",'9');
        binToAscii.put("111010",':');
        binToAscii.put("111011",';');
        binToAscii.put("111100",'<');
        binToAscii.put("111101",'=');
        binToAscii.put("111110",'>');
        binToAscii.put("111111",'?');
        return binToAscii;
    }

    private static Map<String, String> buildSymbolMap() {
        Map<String, String> symbols = new TreeMap<>();
        symbols.put("0", "000000");
        symbols.put("1", "000001");
        symbols.put("2", "000010");
        symbols.put("3", "000011");
        symbols.put("4", "000100");
        symbols.put("5", "000101");
        symbols.put("6", "000110");
        symbols.put("7", "000111");
        symbols.put("8", "001000");
        symbols.put("9", "001001");
        symbols.put(":", "001010");
        symbols.put(";", "001011");
        symbols.put("<", "001100");
        symbols.put("=", "001101");
        symbols.put(">", "001110");
        symbols.put("?", "001111");
        symbols.put("@", "010000");
        symbols.put("A", "010001");
        symbols.put("B", "010010");
        symbols.put("C", "010011");
        symbols.put("D", "010100");
        symbols.put("E", "010101");
        symbols.put("F", "010110");
        symbols.put("G", "010111");
        symbols.put("H", "011000");
        symbols.put("I", "011001");
        symbols.put("J", "011010");
        symbols.put("K", "011011");
        symbols.put("L", "011100");
        symbols.put("M", "011101");
        symbols.put("N", "011110");
        symbols.put("O", "011111");
        symbols.put("P", "100000");
        symbols.put("Q", "100001");
        symbols.put("R", "100010");
        symbols.put("S", "100011");
        symbols.put("T", "100100");
        symbols.put("U", "100101");
        symbols.put("V", "100110");
        symbols.put("W", "100111");
        symbols.put("`", "101000");
        symbols.put("a", "101001");
        symbols.put("b", "101010");
        symbols.put("c", "101011");
        symbols.put("d", "101100");
        symbols.put("e", "101101");
        symbols.put("f", "101110");
        symbols.put("g", "101111");
        symbols.put("h", "110000");
        symbols.put("i", "110001");
        symbols.put("j", "110010");
        symbols.put("k", "110011");
        symbols.put("l", "110100");
        symbols.put("m", "110101");
        symbols.put("n", "110110");
        symbols.put("o", "110111");
        symbols.put("p", "111000");
        symbols.put("q", "111001");
        symbols.put("r", "111010");
        symbols.put("s", "111011");
        symbols.put("t", "111100");
        symbols.put("u", "111101");
        symbols.put("v", "111110");
        symbols.put("w", "111111");
        return symbols;
    }
}