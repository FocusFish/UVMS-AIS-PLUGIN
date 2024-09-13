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

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.spi.*;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * AISManagedConnection
 *
 * @version $Revision: $
 */
public class AISManagedConnection implements ManagedConnection {
    private static final Logger LOG = Logger.getLogger(AISManagedConnection.class.getName());

    private static final AtomicInteger NUMBER_OF_STARTED_THREADS = new AtomicInteger(0);

    private static final int RETRY_DELAY_TIME_SEC = 10;
    private static final int SOCKET_SO_TIMEOUT = 5 * 60 * 1000;

    private static final Pattern COMMENT_BLOCK_PATTERN = Pattern.compile("\\\\(.*?)\\\\(.*)");

    private volatile boolean continueRetry = true;
    private final AtomicReference<CountDownLatch> atomicCountDownLatch = new AtomicReference<>(new CountDownLatch(1));

    private ConcurrentLinkedQueue<Sentence> sentences;

    private Socket socket;

    private PrintWriter logWriter;

    private final AISManagedConnectionFactory mcf;

    private final List<ConnectionEventListener> listeners;
    private final Set<AISConnectionImpl> connections;

    /**
     * Default constructor
     *
     * @param mcf mcf
     */
    public AISManagedConnection(AISManagedConnectionFactory mcf) {
        this.mcf = mcf;
        this.logWriter = null;
        this.listeners = Collections.synchronizedList(new ArrayList<>(1));
        this.connections = new HashSet<>();
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
        LOG.finest("getConnection()");
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
        LOG.finest("associateConnection()");

        if (connection == null)
            throw new ResourceException("Null connection handle");

        if (!(connection instanceof AISConnectionImpl))
            throw new ResourceException("Wrong connection handle");

        AISConnectionImpl handle = (AISConnectionImpl) connection;
        connections.add(handle);
    }

    /**
     * Application server calls this method to force any cleanup on the ManagedConnection instance.
     *
     * @throws ResourceException generic exception if operation fails
     */
    public void cleanup() throws ResourceException {
        LOG.finest("cleanup()");
        connections.clear();
    }

    /**
     * Destroys the physical connection to the underlying resource manager.
     *
     * @throws ResourceException generic exception if operation fails
     */
    public void destroy() throws ResourceException {
        LOG.finest("destroy()");
        connections.forEach(AISConnectionImpl::close);
        connections.clear();
    }

    /**
     * Adds a connection event listener to the ManagedConnection instance.
     *
     * @param listener A new ConnectionEventListener to be registered
     */
    public void addConnectionEventListener(ConnectionEventListener listener) {
        LOG.finest("addConnectionEventListener()");
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
        LOG.finest("removeConnectionEventListener()");
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
        LOG.finest("closing connection handle");
        closeSocket();
        connections.remove((AISConnectionImpl) handle);
        ConnectionEvent event = new ConnectionEvent(this, ConnectionEvent.CONNECTION_CLOSED);
        event.setConnectionHandle(handle);
        for (ConnectionEventListener cel : listeners) {
            cel.connectionClosed(event);
        }
    }

    public void closeSocket() {
        LOG.finest("Closing socket");
        continueRetry = false;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException e) {
                LOG.warning("Error when closing socket. " + e);
            }
        }
    }

    /**
     * Gets the log writer for this ManagedConnection instance.
     *
     * @return Character output stream associated with this Managed-Connection instance
     * @throws ResourceException generic exception if operation fails
     */
    public PrintWriter getLogWriter() throws ResourceException {
        LOG.finest("getLogWriter()");
        return logWriter;
    }

    /**
     * Sets the log writer for this ManagedConnection instance.
     *
     * @param out Character Output stream to be associated
     * @throws ResourceException generic exception if operation fails
     */
    public void setLogWriter(PrintWriter out) throws ResourceException {
        LOG.finest("setLogWriter()");
        logWriter = out;
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
        LOG.finest("getMetaData()");
        return new AISManagedConnectionMetaData();
    }

    public boolean isOpen() {
        boolean socketIsOpen = atomicCountDownLatch.get().getCount() == 0;
        LOG.finest("socket open = " + socketIsOpen);
        return socketIsOpen;
    }

    public List<Sentence> getSentences() {
        if (sentences == null) {
            sentences = new ConcurrentLinkedQueue<>();
        }

        List<Sentence> returnList = new ArrayList<>(sentences.size());

        for (Sentence sentence = sentences.poll(); sentence != null; sentence = sentences.poll()) {
            returnList.add(sentence);
        }

        return returnList;
    }

    public void open(final String host, final Integer port, final String userName, final String password) {
        LOG.finest("Starting AIS reader thread");
        continueRetry = true;
        new Thread("AIS Read thread" + NUMBER_OF_STARTED_THREADS.getAndIncrement()) {
            @Override
            public void run() {
                LOG.finest("AIS reader thread started");
                atomicCountDownLatch.get().countDown();
                while (continueRetry) {
                    socket = new Socket();
                    try {
                        BufferedReader commandInput = tryOpen(host, port, userName, password);
                        read(commandInput);
                    } catch (Exception e) {
                        LOG.warning("AIS connection lost: " + e.getLocalizedMessage());
                        LOG.warning("Exception: " + e);
                    } finally {
                        try {
                            if (socket.isConnected()) {
                                socket.close();
                            }
                            Thread.sleep(RETRY_DELAY_TIME_SEC * 1000L);
                        } catch (Exception e) {
                            LOG.info("//NOP: {}" + e.getLocalizedMessage());
                            LOG.info("Exception:" + e);
                        }
                    }
                }

                LOG.finest("Stopping AIS reader thread");
                CountDownLatch oldLatch = atomicCountDownLatch.get();
                atomicCountDownLatch.compareAndSet(oldLatch, new CountDownLatch(1));
            }
        }.start();

        waitForThreadStart();
    }

    private void waitForThreadStart() {
        try {
            boolean isStarted = atomicCountDownLatch.get().await(1, SECONDS);
            if (!isStarted) {
                LOG.warning("Failed to start the AIS Reader Thread within the allotted 1s.");
            }
        } catch (InterruptedException e) {
            LOG.warning("Thread got interrupted while waiting for AIS read thread to start.");
            Thread.currentThread().interrupt();
        }
    }

    BufferedReader tryOpen(final String host, final Integer port, final String userName, final String password) throws IOException {
        LOG.info("Trying to connect to " + host + " on port " + port);
        sentences = new ConcurrentLinkedQueue<>();

        socket.setKeepAlive(true);
        socket.setSoTimeout(SOCKET_SO_TIMEOUT);
        socket.connect(new InetSocketAddress(InetAddress.getByName(host), port));

        BufferedWriter commandOut = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        LOG.info("AISWorker: Connection established");
        LOG.info("AISWorker: Socket-parameter: " + socket);

        String loginCmd = '\u0001' + userName + '\u0000' + password + '\u0000';
        commandOut.write(loginCmd);
        commandOut.flush();

        return new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    void read(BufferedReader commandInput) throws IOException {
        String input;
        String payload = "";
        String commentBlock = null;
        // Infinite read until read is EOF
        while ((input = commandInput.readLine()) != null) {
            try {
                Matcher matcher = COMMENT_BLOCK_PATTERN.matcher(input);
                if (matcher.matches()) {
                    if (commentBlock == null) {
                        commentBlock = matcher.group(1);
                    }
                    input = matcher.group(2);
                }
                // Split the incoming line
                String[] arr = input.split(",");
                if (arr.length <= 4 || "$ABVSI".equals(arr[0])) {
                    continue;
                }

                if (Integer.parseInt(arr[1]) == 2) {
                    payload += arr[5];
                    // If this part is the last sentence part, cache it
                    if (Integer.parseInt(arr[1]) == Integer.parseInt(arr[2])) {
                        sentences.add(new Sentence(commentBlock, payload));
                        payload = "";
                        commentBlock = null;
                    }
                } else {
                    // This is a single sentence message, cache it
                    sentences.add(new Sentence(commentBlock, arr[5]));
                    commentBlock = null;
                }
            } catch (Exception e) {
                LOG.warning("Input:" + input);
                LOG.warning("Exception: " + e);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AISManagedConnection that = (AISManagedConnection) o;
        return continueRetry == that.continueRetry &&
                Objects.equals(sentences, that.sentences) &&
                Objects.equals(socket, that.socket) &&
                Objects.equals(logWriter, that.logWriter) &&
                Objects.equals(mcf, that.mcf) &&
                listeners.equals(that.listeners);
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(continueRetry);
        result = 31 * result + atomicCountDownLatch.hashCode();
        result = 31 * result + Objects.hashCode(sentences);
        result = 31 * result + Objects.hashCode(socket);
        result = 31 * result + Objects.hashCode(logWriter);
        result = 31 * result + Objects.hashCode(mcf);
        result = 31 * result + listeners.hashCode();
        return result;
    }
}