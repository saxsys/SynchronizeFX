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
 * A thread safe serializer and deserializer that serializes {@link List}s with Java objects to byte arrays.
 * 
 * For a list with classes that this serializer must be able to serialize and deserializer see
 * {@link MessageTransferClient}.
 * 
 * @author raik.bieniek
 */
public interface Serializer {

    /**
     * Serializes a list with messages to byte arrays.
     * 
     * This method must be implemented thread safe.
     * 
     * @param objects The messages that should be serialized.
     * @return The serialized form of the objects.
     * @throws SynchronizeFXException When the serialisation failed. When this exception is thrown, the
     *             serializer must still be able to serialize valid objects.
     */
    byte[] serialize(List<Object> objects) throws SynchronizeFXException;

    /**
     * Deserializes a byte array to objects that was created with {@link Serializer#serialize(List)}.
     * 
     * This method must be implemented thread safe.
     * 
     * @param message The byte array that contains the serialized messages.
     * @return The objects that where encoded in the message.
     * @throws SynchronizeFXException When the deserialisation failed. When this exception is thrown, the
     *             serializer must still be able to deserialize valid objects.
     */
    List<Object> deserialize(byte[] message) throws SynchronizeFXException;
}
