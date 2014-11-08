/**
 * This file is part of SynchronizeFX.
 * 
 * Copyright (C) 2013-2014 Saxonia Systems AG
 *
 * SynchronizeFX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SynchronizeFX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SynchronizeFX. If not, see <http://www.gnu.org/licenses/>.
 */

package de.saxsys.synchronizefx.core.metamodel.executors;

import java.util.UUID;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import de.saxsys.synchronizefx.core.metamodel.WeakObjectRegistry;
import de.saxsys.synchronizefx.core.metamodel.commands.SetPropertyValue;
import de.saxsys.synchronizefx.core.metamodel.commands.Value;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static eu.lestard.assertj.javafx.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Checks if {@link SingleValuePropertyCommandExecutor} works as expected.
 * 
 * @author Raik Bieniek
 */
@RunWith(MockitoJUnitRunner.class)
public class SingleValuePropertyCommandExecutorTest {

    private final UUID exemplaryProperty1Id = UUID.randomUUID();
    private final String exemplaryProperty1Value = "original value";
    private final StringProperty exemplaryProperty1 = new SimpleStringProperty(exemplaryProperty1Value);
    private final SetPropertyValue exemplaryProperty1Change = new SetPropertyValue(UUID.randomUUID(),
            exemplaryProperty1Id, new Value("changed value"));

    private final UUID exemplaryProperty2Id = UUID.randomUUID();
    private final StringProperty exemplaryProperty2Value = new SimpleStringProperty();
    private final ObjectProperty<StringProperty> exemplaryProperty2 = new SimpleObjectProperty<>(
            exemplaryProperty2Value);
    private SetPropertyValue exemplaryProperty2Change = new SetPropertyValue(UUID.randomUUID(), exemplaryProperty2Id,
            new Value(exemplaryProperty1Id));

    @Mock
    private WeakObjectRegistry objectRegistry;

    @InjectMocks
    private SingleValuePropertyCommandExecutor cut;

    /**
     * Wires up the mocked dependencies.
     */
    @Before
    public void wireUpMocks() {
        when(objectRegistry.getByIdOrFail(exemplaryProperty1Id)).thenReturn(exemplaryProperty1);
        when(objectRegistry.getByIdOrFail(exemplaryProperty2Id)).thenReturn(exemplaryProperty2);
    }

    /**
     * When the command log is empty, every incoming command should be executed.
     */
    @Test
    public void shouldExecuteIncommingCommandWhenLogIsEmpty() {
        cut.executeRemoteCommand(exemplaryProperty1Change);
        assertThat(exemplaryProperty1).hasValue("changed value");

        cut.executeRemoteCommand(exemplaryProperty2Change);
        assertThat(exemplaryProperty2).hasValue(exemplaryProperty1);
    }

    /**
     * When an incoming command does not equal the first command in the log queue it should not be executed.
     */
    @Test
    public void shouldDropCommandWhenItsNotEqualToFirstCommandInTheLog() {
        cut.logLocalCommand(new SetPropertyValue(UUID.randomUUID(), new Value("dummy local change")));

        cut.executeRemoteCommand(exemplaryProperty1Change);
        // Property 1 has not changed
        assertThat(exemplaryProperty1).hasValue(exemplaryProperty1Value);

        cut.executeRemoteCommand(exemplaryProperty2Change);
        // Property 2 has not changed
        assertThat(exemplaryProperty2).hasValue(exemplaryProperty2Value);
    }

    /**
     * When an incoming command does equal the first command in the log queue it should not be executed and removed from
     * the queue.
     * 
     * <p>
     * This is tested by checking if its executed when it is received a second time.
     * </p>
     */
    @Test
    public void shouldDropCommandAndRemoveItFromTheLogWhenItIsEqualToTheFirstLogEntry() {
        cut.logLocalCommand(exemplaryProperty1Change);

        cut.executeRemoteCommand(exemplaryProperty1Change);
        // Property 1 has not changed
        assertThat(exemplaryProperty1).hasValue(exemplaryProperty1Value);

        cut.executeRemoteCommand(exemplaryProperty1Change);
        // Property 1 has changed
        assertThat(exemplaryProperty1).hasValue("changed value");
    }
}