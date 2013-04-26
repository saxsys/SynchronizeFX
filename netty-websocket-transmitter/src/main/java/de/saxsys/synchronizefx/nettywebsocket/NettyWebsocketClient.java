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

package de.saxsys.synchronizefx.nettywebsocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.saxsys.synchronizefx.core.clientserver.MessageTransferClient;
import de.saxsys.synchronizefx.core.clientserver.NetworkToTopologyCallbackClient;
import de.saxsys.synchronizefx.core.clientserver.Serializer;
import de.saxsys.synchronizefx.core.exceptions.SynchronizeFXException;

/**
 * A client side transmitter implementation for SynchronizeFX that uses Netty and transferes messages over
 * websockets.
 * 
 */
public class NettyWebsocketClient implements MessageTransferClient {

    private static final Logger LOG = LoggerFactory.getLogger(NettyWebsocketClient.class);

    private Serializer serializer;
    private URI uri;
    private NetworkToTopologyCallbackClient callback;

    private EventLoopGroup eventLoopGroup;

    private Channel channel;

    /**
     * Initializes the transmitter.
     * 
     * @param uri The URI for the server to connect to. This must start with "ws:" as the protocol. Websockets over
     *            HTTPS ("wss:") are not supported at the moment.
     * @param serializer The serializer to use to serialize SynchronizeFX messages.
     */
    public NettyWebsocketClient(final URI uri, final Serializer serializer) {
        this.uri = uri;
        this.serializer = serializer;
    }

    @Override
    public void setTopologyCallback(final NetworkToTopologyCallbackClient callback) {
        this.callback = callback;
    }

    @Override
    public void connect() throws SynchronizeFXException {
        if ("wss".equals(uri.getScheme())) {
            throw new SynchronizeFXException(new IllegalArgumentException(
                    "Websockets over HTTPS are not supported at the moment. Sorry."));
        }
        if (!"ws".equals(uri.getScheme())) {
            throw new SynchronizeFXException(new IllegalArgumentException("The protocol of the uri is not Websocket."));
        }

        this.eventLoopGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        final NettyWebsocketConnection connection = new NettyWebsocketConnection(uri, this);
        bootstrap.group(eventLoopGroup).channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel channel) throws Exception {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast("http-codec", new HttpClientCodec());
                        pipeline.addLast("aggregator", new HttpObjectAggregator(8192));
                        pipeline.addLast("ws-handler", connection);
                    }
                });

        LOG.info("Connecting to server");
        try {
            this.channel = bootstrap.connect(uri.getHost(), uri.getPort()).sync().channel();
            connection.waitForHandshakeFinished();
        } catch (InterruptedException e) {
            eventLoopGroup.shutdown();
            throw new SynchronizeFXException(e);
        }
    }

    @Override
    public void send(final List<Object> messages) {
        byte[] serialized;
        try {
            serialized = serializer.serialize(messages);
        } catch (SynchronizeFXException e) {
            disconnect();
            callback.onError(e);
            return;
        }
        channel.write(new BinaryWebSocketFrame(Unpooled.wrappedBuffer(serialized)));
    }

    @Override
    public void disconnect() {
        disconnect(true);
    }

    private void disconnect(final boolean sendCloseWebsocketFrame) {
        if (sendCloseWebsocketFrame) {
            channel.write(new CloseWebSocketFrame());
        }

        try {
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            callback.onError(new SynchronizeFXException("Could not wait for the disconnect to finish.", e));
        }
        eventLoopGroup.shutdown();
    }

    /**
     * Call this when messages where recieved from the server.
     * 
     * @param msg The messages striped of all Websocket Overhead.
     */
    void onMessageRecived(final byte[] msg) {
        List<Object> deserialized;
        try {
            deserialized = serializer.deserialize(msg);
        } catch (SynchronizeFXException e) {
            disconnect();
            callback.onError(e);
            return;
        }
        callback.recive(deserialized);
    }

    /**
     * Call this when an error occurred.
     * 
     * @param cause The cause of the error.
     */
    void onError(final Throwable cause) {
        disconnect();
        callback.onError(new SynchronizeFXException(cause));
    }

    /**
     * Call this when the server closed the connection.
     */
    void onServerDisconnect() {
        callback.onServerDisconnect();
        disconnect(true);
    }
}
