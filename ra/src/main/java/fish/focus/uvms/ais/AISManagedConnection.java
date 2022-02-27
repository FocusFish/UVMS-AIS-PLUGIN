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

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionEventListener;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.LocalTransaction;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionMetaData;

import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

/**
 * AISManagedConnection
 *
 * @version $Revision: $
 */
public class AISManagedConnection implements ManagedConnection {
    private static final int RETRY_DELAY_TIME_SEC = 10;
    private static final int SOCKET_SO_TIMEOUT = 5 * 60 * 1000;

    /**
     * The logger
     */
    private static Logger log = Logger.getLogger(AISManagedConnection.class.getName());

    /**
     * The logwriter
     */
    private PrintWriter logwriter;

    /**
     * ManagedConnectionFactory
     */
    private AISManagedConnectionFactory mcf;

    /**
     * Listeners
     */
    private List<ConnectionEventListener> listeners;

    /**
     * Connections
     */
    private Set<AISConnectionImpl> connections;

    private ConcurrentLinkedQueue<String> sentences;

    private boolean open = false;
    private boolean continueRetry = true;
    private Socket socket;

    /**
     * Default constructor
     *
     * @param mcf mcf
     */
    public AISManagedConnection(AISManagedConnectionFactory mcf) {
        this.mcf = mcf;
        this.logwriter = null;
        this.listeners = Collections.synchronizedList(new ArrayList<ConnectionEventListener>(1));
        this.connections = new HashSet<AISConnectionImpl>();
    }

    /**
     * Creates a new connection handle for the underlying physical connection
     * represented by the ManagedConnection instance.
     *
     * @param subject       Security context as JAAS subject
     * @param cxRequestInfo ConnectionRequestInfo instance
     * @return generic Object instance representing the connection handle.
     * @throws ResourceException generic exception if operation fails
     */
    public Object getConnection(Subject subject,
                                ConnectionRequestInfo cxRequestInfo) throws ResourceException {
        log.finest("getConnection()");
        AISConnectionImpl connection = new AISConnectionImpl(this, mcf);
        connections.add(connection);
        return connection;
    }

    /**
     * Used by the container to change the association of an
     * application-level connection handle with a ManagedConneciton instance.
     *
     * @param connection Application-level connection handle
     * @throws ResourceException generic exception if operation fails
     */
    public void associateConnection(Object connection) throws ResourceException {
        log.finest("associateConnection()");

        if (connection == null)
            throw new ResourceException("Null connection handle");

        if (!(connection instanceof AISConnectionImpl))
            throw new ResourceException("Wrong connection handle");

        AISConnectionImpl handle = (AISConnectionImpl) connection;
        handle.setManagedConnection(this);
        connections.add(handle);
    }

    /**
     * Application server calls this method to force any cleanup on the ManagedConnection instance.
     *
     * @throws ResourceException generic exception if operation fails
     */
    public void cleanup() throws ResourceException {
        log.finest("cleanup()");
        for (AISConnectionImpl connection : connections) {
            connection.setManagedConnection(null);
        }
        connections.clear();

    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     *
     * @throws ResourceException generic exception if operation fails
     */
    public void destroy() throws ResourceException {
        log.finest("destroy()");

    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     *
     * @param listener A new ConnectionEventListener to be registered
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        log.finest("addConnectionEventListener()");
        if (listener == null)
            throw new IllegalArgumentException("Listener is null");
        listeners.add(listener);
    }

    /**
     * Removes an already registered connection event listener from the ManagedConnection instance.
     *
     * @param listener already registered connection event listener to be removed
     */
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        log.finest("removeConnectionEventListener()");
        if (listener == null)
            throw new IllegalArgumentException("Listener is null");
        listeners.remove(listener);
    }

    /**
     * Close handle
     *
     * @param handle The handle
     */
    void closeHandle(AISConnection handle) {
        continueRetry = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                log.warning("Error when closing socket. " + e);
            }
        }
        connections.remove((AISConnectionImpl) handle);
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener cel : listeners) {
            cel.connectionClosed(event);
        }

    }

    /**
     * Gets the log writer for this ManagedConnection instance.
     *
     * @return Character output stream associated with this Managed-Connection instance
     * @throws ResourceException generic exception if operation fails
     */
    public PrintWriter getLogWriter() throws ResourceException {
        log.finest("getLogWriter()");
        return logwriter;
    }

    /**
     * Sets the log writer for this ManagedConnection instance.
     *
     * @param out Character Output stream to be associated
     * @throws ResourceException generic exception if operation fails
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        log.finest("setLogWriter()");
        logwriter = out;
    }

    /**
     * Returns an <code>javax.resource.spi.LocalTransaction</code> instance.
     *
     * @return LocalTransaction instance
     * @throws ResourceException generic exception if operation fails
     */
    public LocalTransaction getLocalTransaction() throws ResourceException {
        throw new NotSupportedException("getLocalTransaction() not supported");
    }

    /**
     * Returns an <code>javax.transaction.xa.XAresource</code> instance.
     *
     * @return XAResource instance
     * @throws ResourceException generic exception if operation fails
     */
    public XAResource getXAResource() throws ResourceException {
        throw new NotSupportedException("getXAResource() not supported");
    }

    /**
     * Gets the metadata information for this connection's underlying EIS resource manager instance.
     *
     * @return ManagedConnectionMetaData instance
     * @throws ResourceException generic exception if operation fails
     */
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        log.finest("getMetaData()");
        return new AISManagedConnectionMetaData();
    }

    /**
     * Call me
     */
    void callMe() {
        log.finest("callMe()");
        if (sentences == null) {
            sentences = new ConcurrentLinkedQueue<>();
        }
        //System.out.println("Sentences: " + sentences.size());
        //System.out.println("Sentences: " + sentences);
    }

    public boolean isOpen() {
        return open;
    }

    public List<String> getSentences() {
        if (sentences == null) {
            sentences = new ConcurrentLinkedQueue<>();
        }

        ArrayList<String> returnList = new ArrayList<>();
        returnList.addAll(sentences);
        sentences.clear();

        return returnList;
    }

    public long getQueueSize() {
        if (sentences == null) {
            sentences = new ConcurrentLinkedQueue<>();
        }

        return sentences.size();
    }

    void open(final String host, final Integer port, final String userName, final String password) {
        new Thread("AIS Read thread") {
            @Override
            public void run() {
                open = true;
                while (continueRetry) {
                    socket = new Socket();
                    try {
                        BufferedReader commandInput = tryOpen(host, port, userName, password);
                        read(commandInput);
                    } catch (Exception e) {
                        log.warning("AIS connection lost: " + e.getLocalizedMessage());
                        log.warning("Exception: " + e);
                    } finally {
                        try {
                            if (socket.isConnected()) {
                                socket.close();
                            }
                            Thread.sleep(RETRY_DELAY_TIME_SEC * 1000);
                        } catch (Exception e) {
                            log.info("//NOP: {}" + e.getLocalizedMessage());
                            log.info("Exception:" + e);
                        }
                    }
                }

                open = false;
            }
        }.start();
    }

    BufferedReader tryOpen(final String host, final Integer port, final String userName, final String password) throws IOException {
        sentences = new ConcurrentLinkedQueue<>();

        socket.setKeepAlive(true);
        socket.setSoTimeout(SOCKET_SO_TIMEOUT);
        socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));

        BufferedWriter commandOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        log.info("AISWorker: Connection established");
        log.info("AISWorker: Socket-parameter: " + socket);

        String loginCmd = '\u0001' + userName + '\u0000' + password + '\u0000';
        commandOut.write(loginCmd);
        commandOut.flush();

        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    void read(BufferedReader commandInput) throws IOException {
        String input;
        String tmp = "";
        // Infinite read until read is EOF
        while ((input = commandInput.readLine()) != null) {

            try {
                // Split the incoming line
                String[] arr = input.split(",");
                if (arr != null && arr.length > 4 && !"$ABVSI".equals(arr[0])) {
                    if (Integer.parseInt(arr[1]) == 2) {
                        tmp += arr[5];
                        // If this part is the last sentence part, cache it
                        if (Integer.parseInt(arr[1]) == Integer.parseInt(arr[2])) {
                            sentences.add(tmp);
                            tmp = "";
                        }
                    } else {
                        // This is a single sentence message, cache it
                        sentences.add(arr[5]);
                    }
                }
            } catch (Exception e) {
                log.fine("Input:" + input);
                log.fine("Exception: " + e);
            }

        }
    }
}