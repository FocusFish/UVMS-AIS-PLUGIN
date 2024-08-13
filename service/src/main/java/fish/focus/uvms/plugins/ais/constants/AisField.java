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
package fish.focus.uvms.plugins.ais.constants;

public enum AisField {
    ID(0),
    REPEAT_INDICATOR(1),
    MMSI(2),
    NAVIGATIONAL_STATUS(3),
    RATE_OF_TURN(4),
    SPEED_OVER_GROUND(5),
    POSITION_ACCURACY(6),
    LONGITUDE(7),
    LATITUDE(8),
    COURSE_OVER_GROUND(9),
    TRUE_HEADING(10),
    UTC_SECOND(11);

    private int index;

    private AisField(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}