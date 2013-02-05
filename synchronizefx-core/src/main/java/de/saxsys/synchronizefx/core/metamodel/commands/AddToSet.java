package de.saxsys.synchronizefx.core.metamodel.commands;

import java.util.UUID;

/**
 * A command that states that an element should be added to a set.
 * 
 */
public class AddToSet {
    @Override
    public String toString() {
        return "AddToSet [setId=" + setId + ", observableObjectId=" + observableObjectId + ", simpleObjectValue="
                + simpleObjectValue + "]";
    }

    private UUID setId;
    private UUID observableObjectId;
    private Object simpleObjectValue;

    /**
     * @return The id of the set where an element should be added.
     */
    public UUID getListId() {
        return setId;
    }
    
    /**
     * @see AddToSet#getListId()
     * @param setId The id
     */
    public void setSetId(final UUID setId) {
        this.setId = setId;
    }
    
    /**
     * @return The id of the observable object that should be added to the set. If this is null, than the value is a
     *         simple object an can be retrieved through through {@link AddToSet#getSimpleObjectValue()}.
     */
    public UUID getObservableObjectId() {
        return observableObjectId;
    }

    /**
     * @see AddToSet#getObservableObjectId()
     * @param id The id
     */
    public void setObservableObjectId(final UUID id) {
        this.observableObjectId = id;
    }
    
    /**
     * @return The simple object that should be added to the set. The returned value is only valid if
     *         {@link #getObservableObjectId()} returns null.
     */
    public Object getSimpleObjectValue() {
        return simpleObjectValue;
    }
    
    /**
     * @see AddToSet#getSimpleObjectValue()
     * @param value The value
     */
    public void setSimpleObjectValue(final Object value) {
        this.simpleObjectValue = value;
    }
}
