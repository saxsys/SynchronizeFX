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

package de.saxsys.synchronizefx.core.clientserver;

import java.util.List;

import de.saxsys.synchronizefx.core.exceptions.SynchronizeFXException;

/**
 * This is an callback interface for the network library to inform the upper layer of incomming messages.
 * 
 * This is the interface for the client side. For the server side use {@link IncommingEventHandlerServer}.
 * 
 * @author raik.bieniek
 * 
 */
public interface NetworkToTopologyCallbackClient {

    /**
     * Messages were received.
     * 
     * @param messages The messages received.
     */
    void recive(List<Object> messages);

    /**
     * An error occurred that made the Client disconnect from the server.
     * 
     * When this method is called, the connection to the server has to be already closed.
     * 
     * This method must only be called after the client successfully connected to a server. If an error occurred while
     * trying to connect to the server, throw an exception there.
     * 
     * @param e an exception that describes the problem.
     */
    void onError(SynchronizeFXException e);
}
