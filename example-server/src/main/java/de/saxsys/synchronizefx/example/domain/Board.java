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

package de.saxsys.synchronizefx.example.domain;

import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;

/**
 * Represents the board that contains the notes.
 * 
 * @author raik.bieniek
 *
 */
public class Board {
    private ListProperty<Note> notes = new SimpleListProperty<>(FXCollections.<Note> observableArrayList());

    /**
     * 
     * @return the notes that are currently placed on the board.
     */
    public List<Note> getNotes() {
        return notes.get();
    }

    /**
     * 
     * @see Board#getNotes()
     * @return the property
     */
    public ListProperty<Note> notesProperty() {
        return notes;
    }
}
