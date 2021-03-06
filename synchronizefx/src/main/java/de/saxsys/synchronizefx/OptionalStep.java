/**
 * This file is part of SynchronizeFX.
 * 
 * Copyright (C) 2013-2014 Saxonia Systems AG
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

package de.saxsys.synchronizefx;

import java.util.concurrent.Executor;

import com.esotericsoftware.kryo.Serializer;

/**
 * Base interface for all steps that are optional and can be used for both the server and the client.
 * 
 * @param <K> The return type for the step methods. This has to be the extending interface itself.
 */
public interface OptionalStep<K> {

    /**
     * Sets a custom port that differs from the default port 54263.
     * 
     * @param port The port to connect if used for the client or the port on listen when used on server side.
     * @return The builder to provide a fluent API.
     */
    K port(int port);

    /**
     * Sets a custom serializer for some {@link Class}.
     * 
     * <p>
     * Internally Kryo is used for the serialization and deserialization of objects. You do not need to register
     * serializers for every class you use in your domain model but in some cases this is desirable. Registering
     * custom serializers can be necessary when you use classes without a No-Arg constructor or to increase the
     * performance and decrease the network usage.
     * </p>
     * 
     * <p>
     * An efficient serializer for {@link java.util.UUID} is already registered.
     * </p>
     * 
     * @param clazz The class for that the serializer should be registered.
     * @param <T> same as clazz.
     * @param serializer The serializer to register.
     * @return The builder to provide a fluent API.
     */
    <T> K customSerializer(final Class<T> clazz, final Serializer<T> serializer);

    /**
     * Sets a custom executor for changes done on the domain model.
     * 
     * <p>
     * This method allows to set a custom executor that should be used by SynchronizeFX for all changes done to the
     * JavaFX properties in the domain model.
     * </p>
     * 
     * <p>
     * If this method is not used, SynchronizeFX will uses an {@link Executor} for clients that executes all changes
     * in the JavaFX thread (see {@link javafx.application.Platform#runLater(Runnable)}. For servers an
     * {@link Executor} is used, that executes all changes in the network thread that received them.
     * </p>
     * 
     * <p>
     * This functionality is useful for Non-GUI clients that do not have a JavaFX thread for example.
     * </p>
     * 
     * @param executor The executor to user for changes done on the domain model.
     * @return The builder to provide a fluent API.
     */
    K modelChangeExecutor(final Executor executor);
}
