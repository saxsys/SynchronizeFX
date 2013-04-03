/**
 * This file is part of SynchronizeFX.
 * 
 * Copyright (C) 2013 Saxonia Systems AG
 *
 * SynchronizeFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SynchronizeFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SynchronizeFX. If not, see <http://www.gnu.org/licenses/>.
 */

package de.saxsys.synchronizefx.tomcat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.saxsys.synchronizefx.core.clientserver.MessageTransferServer;
import de.saxsys.synchronizefx.core.clientserver.NetworkToTopologyCallbackServer;
import de.saxsys.synchronizefx.core.clientserver.Serializer;
import de.saxsys.synchronizefx.core.clientserver.SynchronizeFxServer;
import de.saxsys.synchronizefx.core.exceptions.SynchronizeFXException;

/**
 * An server-side network layer implementation for SynchronizeFX that uses the websocket implementation of Apache
 * Tomcat for the network transfer.
 * 
 * Clients wishing to connect to this implementation the Websocket sub-protocol used must be
 * "v1.synchronizefx.saxsys.de". It must be ensured that this server and the client use {@link Serializer}
 * implementations that are compatible. Ideally both sides use the same implementations. Each message that is created
 * by {@link Serializer#serialize(java.util.List)} must be send as is in a single websocket binary frame. Each
 * content binary frames must be passed through {@link Serializer#deserialize(byte[])} to reproduce the SynchronizeFX
 * messages.
 * 
 * @author raik.bieniek
 * 
 */
public abstract class SynchronizeFXTomcatServlet extends WebSocketServlet implements MessageTransferServer {
    private static final long serialVersionUID = -1859780171572536501L;

    private static final Logger LOG = LoggerFactory.getLogger(SynchronizeFXTomcatServlet.class);

    private final List<MessageInbound> connections = new LinkedList<>();
    private NetworkToTopologyCallbackServer callback;

    private Serializer serializer;

    /**
     * Returns a serializer that should be used to serialize and deserialize the messages of the SynchronizeFX
     * framework.
     * 
     * This method is called only once so you can use <code>new</code> for the serializer object (e.g.
     * <code>return new MyCustomSerializer();</code>).
     * 
     * @return The serializer
     */
    protected abstract Serializer getSerializer();

    /**
     * This method ensures that this object calls {@link SynchronizeFXTomcatServlet#getSerializer()} only once.
     * 
     * @return The serializer instance.
     */
    private Serializer getSerializerInternal() {
        if (serializer == null) {
            serializer = getSerializer();
        }
        return serializer;
    }

    // WebSocketServlet

    /**
     * Prevents clients from connecting until a {@link SynchronizeFxServer} has started to listen for incoming
     * websocket messages.
     * 
     * @param req see {@link HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}
     * @param resp see {@link HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}
     * @throws ServletException When no {@link SynchronizeFxServer} listens for messages until now.
     * @throws IOException see {@link HttpServlet#doGet(HttpServletRequest, HttpServletResponse)}
     * @see HttpServlet
     */
    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException,
        IOException {
        if (callback == null) {
            throw new UnavailableException(
                    "The system isn't fully set up to handle your requests on this resource yet.", 5);
        }
        super.doGet(req, resp);
    }

    /**
     * Disconnect all clients an clear the connections when the handler is destroyed by CDI.
     */
    @PreDestroy
    public void destroy() {
        LOG.info("Destroying SynchronizeFXTomcatServlet");
        shutdown();
        LOG.info("SynchronizeFXTomcatServlet destroyed.");
    }

    @Override
    protected StreamInbound createWebSocketInbound(final String subProtocol, final HttpServletRequest request) {
        // TODO validate sub protocol and find out how to refuse connections that send an unsupported sub protocol.
        return new SynchronizeFXTomcatConnection(this);
    }

    /**
     * Sends {@link WsKryoTomcatHandler#kryobuffer} to a destination.
     * 
     * @param buffer the bytes to send.
     * @param destination The peer to send to.
     */
    public void send(final byte[] buffer, final Object destination) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending from thread: id: " + Thread.currentThread().getName() + ", name: "
                    + Thread.currentThread().getName());
        }

        final WsOutbound outbound = ((MessageInbound) destination).getWsOutbound();
        try {
            outbound.writeBinaryMessage(ByteBuffer.wrap(buffer));
        } catch (final IOException e) {
            LOG.warn("Sending data to a client failed. Closing connection to this client.");
            try {
                outbound.close(1002, null);
                // CHECKSTYLE:OFF
            } catch (IOException e1) {
                // Maybe the connection is already closed. This is no exceptional state but rather the default in
                // this
                // case. So it's safe to ignore this exception.
            }
            // CHECKSTYLE:ON
            connectionCloses((SynchronizeFXTomcatConnection) destination);
        }
    }

    // Used by SynchronizeFXTomcatConnection objects

    /**
     * Informs this {@link MessageTransferServer} that a new client connection is ready.
     * 
     * @param connection The connection that just got ready.
     */
    void clientConnectionReady(final SynchronizeFXTomcatConnection connection) {
        LOG.info("Client connected.");
        callback.onConnect(connection);
        connections.add(connection);
    }

    /**
     * Informs this {@link MessageTransferServer} that a client received a message.
     * 
     * @param message The message that was received.
     * @param sender The connection that received the message.
     */
    void recivedMessage(final ByteBuffer message, final SynchronizeFXTomcatConnection sender) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Received a message in thread: id: " + Thread.currentThread().getName() + ", name: "
                    + Thread.currentThread().getName());
        }
        List<Object> messages;
        try {
            messages = getSerializerInternal().deserialize(message.array());
        } catch (SynchronizeFXException e) {
            try {
                sender.getWsOutbound().close(0, null);
            } catch (IOException e1) {
                callback.onClientConnectionError(new SynchronizeFXException(e1));
            }
            callback.onClientConnectionError(e);
            return;
        }
        callback.recive(messages, sender);
    }

    /**
     * Informs this {@link MessageTransferServer} that a client connection got closed.
     * 
     * @param connection The connection that was closed
     */
    void connectionCloses(final SynchronizeFXTomcatConnection connection) {
        LOG.info("Client connection closed.");
        connections.remove(connection);
    }

    // MessageTransferServer

    @Override
    public void setTopologyLayerCallback(final NetworkToTopologyCallbackServer callback) {
        this.callback = callback;
    }

    @Override
    public void send(final List<Object> messages, final Object destination) {
        byte[] buffer;
        try {
            buffer = getSerializerInternal().serialize(messages);
        } catch (SynchronizeFXException e) {
            shutdown();
            callback.onFatalError(e);
            return;
        }
        send(buffer, destination);
    }

    @Override
    public void sendToAll(final List<Object> messages) {
        sendToAllExcept(messages, null);
    }

    @Override
    public void sendToAllExcept(final List<Object> messages, final Object nonReciver) {
        byte[] buffer;
        try {
            buffer = getSerializerInternal().serialize(messages);
        } catch (SynchronizeFXException e) {
            shutdown();
            callback.onFatalError(e);
            return;
        }
        for (final MessageInbound connection : connections) {
            if (connection != nonReciver) {
                send(buffer, connection);
            }
        }
    }

    @Override
    public void start() throws SynchronizeFXException {
        // Starting is done by starting the servlet in the way servlets usualy get started. So this method does
        // nothing.

    }

    /**
     * Disconnects all clients and makes the servlet refuse new connections.
     */
    @Override
    public void shutdown() {
        callback = null;
        for (MessageInbound connection : connections) {
            try {
                connection.getWsOutbound().close(0, null);
            } catch (IOException e) {
                LOG.error("Connection [" + connection.toString() + "] can't be closed.", e);
            }
        }
        connections.clear();
        // TODO unload servlet
    }
}
