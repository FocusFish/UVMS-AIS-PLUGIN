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
/*
 * IronJacamar, a Java EE Connector Architecture implementation
 * Copyright 2013, Red Hat Inc, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package fish.focus.uvms.ais;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * AISConnectionImpl
 *
 * @version $Revision: $
 */
public class AISConnectionImpl implements AISConnection {
    /**
     * The logger
     */
    private static final Logger LOG = Logger.getLogger(AISConnectionImpl.class.getName());

    /**
     * ManagedConnection
     */
    private AISManagedConnection mc;

    /**
     * ManagedConnectionFactory
     */
    private AISManagedConnectionFactory mcf;

    /**
     * Default constructor
     *
     * @param mc  AISManagedConnection
     * @param mcf AISManagedConnectionFactory
     */
    public AISConnectionImpl(AISManagedConnection mc, AISManagedConnectionFactory mcf) {
        this.mc = mc;
        this.mcf = mcf;
    }

    @Override
    public void open(String host, Integer port, String userName, String password) {
        LOG.finest("Open connection");
        if (mc != null) {
            mc.open(host, port, userName, password);
        }
    }

    @Override
    public boolean isOpen() {
        LOG.finest("isOpen");
        if (mc == null) {
            return false;
        }
        return mc.isOpen();
    }

    @Override
    public List<Sentence> getSentences() {
        LOG.finest("getSentences");
        if (mc == null) {
            return List.of();
        }
        return mc.getSentences();
    }

    @Override
    public void close() {
        LOG.finest("Closing connection");
        if (mc != null) {
            mc.closeHandle(this);
            mc = null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AISConnectionImpl that = (AISConnectionImpl) o;
        return Objects.equals(mc, that.mc) && Objects.equals(mcf, that.mcf);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(mc);
        result = 31 * result + Objects.hashCode(mcf);
        return result;
    }
}