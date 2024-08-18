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

import fish.focus.uvms.plugins.ais.service.Conversion;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.junit.Assert;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    /**
     *
     */
    public void testType5_1() throws Exception {

        String aisMsg = "5CpuqR029m2U<pLP00084i@T<40000000000000N1HN814lf0<1i6CR@@PC52@ii6CR@@00";
        int lenAis = aisMsg.length();
        String binary = "";
        for (int i = 0; i < lenAis; i++) {
            String binChar = Conversion.getBinaryForSymbol(aisMsg.charAt(i));
            binary += binChar;
        }
        String vesselName = Conversion.getAsciiStringFromBinaryString(binary.substring(112, 232));
        Assert.assertEquals("BALTICA", vesselName);

        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        String cc = mmsi.substring(0, 3);
        String ansi3 = Conversion.getAnsi3ForCountryCode(cc);
        Assert.assertEquals("POL", ansi3);


        String ircs = Conversion.getAsciiStringFromBinaryString(binary.substring(70, 112));
        String shipType = Conversion.getAsciiStringFromBinaryString(binary.substring(232, 240));

        Assert.assertEquals("SNGH", ircs);
        Assert.assertEquals("G", shipType);

    }

    public void testType5_2() throws Exception {

        String aisMsg = "53oTbV029MP4haDt000`tPp0000000000000000l1@G556eA0<Tk0AiCP00000000000000";
        int lenAis = aisMsg.length();
        String binary = "";
        for (int i = 0; i < lenAis; i++) {
            String binChar = Conversion.getBinaryForSymbol(aisMsg.charAt(i));
            binary += binChar;
        }
        String vesselName = Conversion.getAsciiStringFromBinaryString(binary.substring(112, 232));
        Assert.assertEquals("JOHN", vesselName);

        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));
        String cc = mmsi.substring(0, 3);
        String ansi3 = Conversion.getAnsi3ForCountryCode(cc);
        Assert.assertEquals("NOR", ansi3);

        String ircs = Conversion.getAsciiStringFromBinaryString(binary.substring(70, 112));
        String shipType = Conversion.getAsciiStringFromBinaryString(binary.substring(232, 240));

        Assert.assertEquals("LJUO", ircs);
        Assert.assertEquals("M", shipType);

    }

    public void testType5_3() {
        String vesselName = Conversion.getAsciiStringFromBinaryString("");
        Assert.assertEquals("", vesselName);
    }


    public void testType24() {

        String aisMsg = "H3uHE`058du=DpA>0L5=@P4lp00";
        int lenAis = aisMsg.length();
        String binary = "";
        for (int i = 0; i < lenAis; i++) {
            String binChar = Conversion.getBinaryForSymbol(aisMsg.charAt(i));
            binary += binChar;
        }
        String vesselName = Conversion.getAsciiStringFromBinaryString(binary.substring(40, 160));
        Assert.assertEquals("ARKOSUNDS GASTHAMN", vesselName);

        String mmsi = String.valueOf(Integer.parseInt(binary.substring(8, 38), 2));

        String cc = mmsi.substring(0, 3);
        String ansi3 = Conversion.getAnsi3ForCountryCode(cc);
        Assert.assertEquals("SWE", ansi3);

        String ircs = Conversion.getAsciiStringFromBinaryString(binary.substring(90, 132));
        String shipType = Conversion.getAsciiStringFromBinaryString(binary.substring(40, 48));


        //Assert.assertEquals("LJUO",ircs);
        Assert.assertEquals("A", shipType);

    }
}