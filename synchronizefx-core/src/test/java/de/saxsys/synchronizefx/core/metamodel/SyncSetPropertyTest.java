package de.saxsys.synchronizefx.core.metamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashSet;
import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleSetProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;

import org.junit.Before;
import org.junit.Test;

import de.saxsys.synchronizefx.core.metamodel.commands.AddToSet;
import de.saxsys.synchronizefx.core.metamodel.commands.CreateObservableObject;
import de.saxsys.synchronizefx.core.metamodel.commands.RemoveFromSet;
import de.saxsys.synchronizefx.core.metamodel.commands.SetPropertyValue;
import de.saxsys.synchronizefx.core.testutils.SaveParameterCallback;

/**
 * Syncing of {@link SetProperty} is not supported yet. This test just makes sure, that the framework fails properly
 * when somebody has SetProperties in his domain model.
 * 
 * @author raik.bieniek
 */
public class SyncSetPropertyTest {
    private Root root;
    private SaveParameterCallback cb;
    private MetaModel model;

    /**
     * Initializes an example domain object and the meta model.
     */
    @Before
    public void init() {
        root = new Root();
        cb = new SaveParameterCallback();
        model = new MetaModel(cb, root);
    }

    /**
     * Tests that correct messages are generated for a domain model that contains {@link SetProperty}s.
     */
    @Test
    public void testManualCreate() {
        List<Object> commands = model.commandsForDomainModel();
        CreateObservableObject msg = (CreateObservableObject) commands.get(0);

        assertEquals(2, msg.getPropertyNameToId().size());
        assertNotNull(msg.getPropertyNameToId().get("set"));
        assertNotNull(msg.getPropertyNameToId().get("childSet"));
    }

    /**
     * Tests if the commands to reproduce an add operation to a set are correctly created.
     */
    @Test
    public void testAdd() {
        root.set.add("some string");
        AddToSet msg1 = (AddToSet) cb.getCommands().get(0);
        assertNotNull(msg1.getListId());
        assertNull(msg1.getObservableObjectId());
        assertEquals("some string", msg1.getSimpleObjectValue());

        root.childSet.add(new Child());
        CreateObservableObject msg2 = (CreateObservableObject) cb.getCommands().get(0);
        AddToSet msg3 = (AddToSet) cb.getCommands().get(2);
        assertEquals(Child.class.getName(), msg2.getClassName());
        assertNotNull(msg3.getListId());
        assertNotNull(msg3.getObservableObjectId());
        assertNull(msg3.getSimpleObjectValue());
    }

    /**
     * Tests that the correct commands are produced when something is removed from a map.
     */
    @Test
    public void testRemove() {
        // first, add some test data
        root.set.add("Test Value 0");
        root.set.add("Test Value 1");
        root.childSet.add(new Child(0));
        root.childSet.add(new Child(1));

        root.set.remove("Test Value 1");
        RemoveFromSet msg1 = (RemoveFromSet) cb.getCommands().get(0);
        assertNotNull(msg1.getListId());
        assertEquals("Test Value 1", msg1.getSimpleObjectValue());
        assertNull(msg1.getObservableObjectId());

        root.childSet.remove(new Child(0));
        // first two for child
        assertEquals(CreateObservableObject.class, cb.getCommands().get(0).getClass());
        assertEquals(SetPropertyValue.class, cb.getCommands().get(1).getClass());
        RemoveFromSet msg2 = (RemoveFromSet) cb.getCommands().get(2);
        assertNotNull(msg2.getListId());
        assertNull(msg2.getSimpleObjectValue());
        assertNotNull(msg2.getObservableObjectId());
    }

    /**
     * Tests that two model instances can be keep synchronous when change commands are applied.
     */
    @Test
    public void testApplyGeneratedMessages() {
        SaveParameterCallback copyCb = new SaveParameterCallback();
        MetaModel copy = new MetaModel(copyCb);

        copy.execute(model.commandsForDomainModel());
        Root copyRoot = (Root) copyCb.getRoot();

        assertEquals(root, copyRoot);
        
        root.set.add("someValue");
        assertFalse(root.equals(copyRoot));
        copy.execute(cb.getCommands());
        assertEquals(root, copyRoot);

        root.set.remove("someValue");
        assertFalse(root.equals(copyRoot));
        copy.execute(cb.getCommands());
        assertEquals(root, copyRoot);

        root.childSet.add(new Child(50));
        assertFalse(root.equals(copyRoot));
        copy.execute(cb.getCommands());
        assertEquals(root, copyRoot);
    }

    /**
     * Tests that commands for changes on elements in sets are also generated.
     */
    @Test
    public void testChangesOnChilds() {
        Child someChild = new Child();
        root.childSet.add(someChild);

        someChild.someInt.set(475);

        SetPropertyValue msg = (SetPropertyValue) cb.getCommands().get(0);
        assertEquals(475, msg.getSimpleObjectValue());
        assertNull(msg.getObservableObjectId());
    }

    /**
     * An example domain class that should be synchronized.
     * 
     * Unused fields are accessed via reflection in the framework.
     * 
     * @author raik.bieniek
     */
    private static final class Root {
        final ObservableSet<String> notSynchronized = FXCollections.observableSet(new HashSet<String>());
        final SetProperty<String> set = new SimpleSetProperty<>(FXCollections.<String> observableSet());
        final SetProperty<Child> childSet = new SimpleSetProperty<>(FXCollections.<Child> observableSet());
        
        public Root() {
            
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((childSet.get() == null) ? 0 : childSet.get().hashCode());
            result = prime * result + ((notSynchronized == null) ? 0 : notSynchronized.hashCode());
            result = prime * result + ((set.get() == null) ? 0 : set.get().hashCode());
            return result;
        }

        // CHECKSTYLE:OFF more or less generated code
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Root other = (Root) obj;
            if (childSet.get() == null) {
                if (other.childSet.get() != null) {
                    return false;
                }
            } else if (!childSet.get().equals(other.childSet.get())) {
                return false;
            }
            if (notSynchronized == null) {
                if (other.notSynchronized != null) {
                    return false;
                }
            } else if (!notSynchronized.equals(other.notSynchronized)) {
                return false;
            }
            if (set.get() == null) {
                if (other.set.get() != null) {
                    return false;
                }
            } else if (!set.get().equals(other.set.get())) {
                return false;
            }
            return true;
        }
        // CHECKSTYLE:ON
    }

    /**
     * Part of the example domain class.
     * 
     * @see Root
     */
    private static final class Child {
        final IntegerProperty someInt = new SimpleIntegerProperty();

        public Child() {

        }

        public Child(final int someInt) {
            this.someInt.set(someInt);
        }

        @Override
        public int hashCode() {
            return someInt.get();
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Child other = (Child) obj;
            if (someInt.get() != other.someInt.get()) {
                return false;
            }
            return true;
        }
    }
}
