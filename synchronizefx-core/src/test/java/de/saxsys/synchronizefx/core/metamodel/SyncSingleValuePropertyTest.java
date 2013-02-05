package de.saxsys.synchronizefx.core.metamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SetProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.junit.Before;
import org.junit.Test;

import de.saxsys.synchronizefx.core.metamodel.MetaModel;
import de.saxsys.synchronizefx.core.metamodel.commands.CreateObservableObject;
import de.saxsys.synchronizefx.core.metamodel.commands.SetPropertyValue;
import de.saxsys.synchronizefx.core.testutils.SaveParameterCallback;

/**
 * Tests if {@link Property} fields that aren't {@link ListProperty}, {@link MapProperty}, or {@link SetProperty} are
 * synchronized correctly.
 * 
 * @author raik.bieniek
 * 
 */
public class SyncSingleValuePropertyTest {

    private Root root;
    private SaveParameterCallback cb;
    private MetaModel model;

    /**
     * Initializes an example domain object and the meta model.
     */
    @Before
    public void init() {
        root = new Root();
        this.cb = new SaveParameterCallback();
        model = new MetaModel(this.cb, root);
    }

    /**
     * Tests the user initiated creation of commands for an observable object that contains {@link Property}s.
     */
    @Test
    public void testManualCreate() {
        // set some test data
        root.someString.set("Test");

        // create commands
        List<Object> commands = model.commandsForDomainModel();

        // check created commands
        boolean createRootObject = false;
        boolean createChildObject = false;
        boolean setTestValue = false;

        for (Object command : commands) {
            if (command instanceof CreateObservableObject) {
                CreateObservableObject cob = (CreateObservableObject) command;
                if (Root.class.getName().equals(cob.getClassName())) {
                    assertEquals(2, cob.getPropertyNameToId().size());
                    createRootObject = true;
                } else if (Child.class.getName().equals(cob.getClassName())) {
                    assertEquals(1, cob.getPropertyNameToId().size());
                    createChildObject = true;
                }
            } else if (command instanceof SetPropertyValue) {
                SetPropertyValue spv = (SetPropertyValue) command;
                if ("Test".equals(spv.getSimpleObjectValue())) {
                    setTestValue = true;
                }
            }
        }

        assertTrue(createRootObject);
        assertTrue(createChildObject);
        assertTrue(setTestValue);
    }

    /**
     * Tests that appropriate messages are generated when the value of a property is changed.
     */
    @Test
    public void testSetProperty() {
        // set simple object value
        root.someString.set("Some Test String");
        SetPropertyValue msg1 = (SetPropertyValue) cb.getCommands().get(0);
        assertNull(msg1.getObservableObjectId());
        assertEquals("Some Test String", msg1.getSimpleObjectValue());

        // set observable object value;
        Child newChild = new Child();
        newChild.childInt.set(275);
        root.someChild.set(newChild);
        // create the new child
        assertEquals(cb.getCommands().get(0).getClass(), CreateObservableObject.class);
        // get(1) = SetPropertyValue for childInt in child; get(2) = SetPropertyValue for child in Root
        SetPropertyValue msg2 = (SetPropertyValue) cb.getCommands().get(2);
        assertNull(msg2.getSimpleObjectValue());
        assertNotNull(msg2.getObservableObjectId());
    }

    /**
     * Tests that changes done to child observable objects that are saved in a {@link Property} of parent observable
     * object are also synchronized.
     */
    @Test
    public void testChangesOnChilds() {
        root.someChild.get().childInt.set(547);
        SetPropertyValue msg = (SetPropertyValue) cb.getCommands().get(0);

        assertEquals(547, msg.getSimpleObjectValue());
        assertNull(msg.getObservableObjectId());
    }

    /**
     * Tests that the generated messages can be a applied on copies of the domain model.
     * 
     * When done so, the copies should be equal to the original.
     */
    @Test
    public void testApplyGeneratedMessages() {
        // setup
        SaveParameterCallback copyCb = new SaveParameterCallback();
        MetaModel copyMeta = new MetaModel(copyCb);
        copyMeta.execute(model.commandsForDomainModel());
        Root copyRoot = (Root) copyCb.getRoot();

        assertEquals(copyRoot, root);

        // change simple object in Root
        root.someString.set("something");
        assertNotEquals(copyRoot, root);
        copyMeta.execute(cb.getCommands());
        assertEquals(copyRoot, root);

        // change simple object in Child
        root.someChild.get().childInt.set(42);
        assertNotEquals(copyRoot, root);
        copyMeta.execute(cb.getCommands());
        assertEquals(copyRoot, root);

        // change observable object in Root
        root.someChild.set(new Child());
        assertNotEquals(copyRoot, root);
        copyMeta.execute(cb.getCommands());
        assertEquals(copyRoot, root);
    }

    private static void assertNotEquals(final Object obj1, final Object obj2) {
        assertFalse(obj2.equals(obj1));
    }

    /**
     * An example domain class that should be synchronized.
     * 
     */
    private static final class Root {

        double notSynced = 2.0;
        final StringProperty someString = new SimpleStringProperty();
        final ObjectProperty<Child> someChild = new SimpleObjectProperty<>(new Child());

        public Root() {
            
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            long temp;
            temp = Double.doubleToLongBits(notSynced);
            result = prime * result + (int) (temp ^ (temp >>> 32));
            result = prime * result + ((someChild.get() == null) ? 0 : someChild.get().hashCode());
            result = prime * result + ((someString.get() == null) ? 0 : someString.get().hashCode());
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
            if (Double.doubleToLongBits(notSynced) != Double.doubleToLongBits(other.notSynced)) {
                return false;
            }
            if (someString.get() == null) {
                if (other.someString.get() != null) {
                    return false;
                }
            } else if (!someString.get().equals(other.someString.get())) {
                return false;
            }
            if (someChild.get() == null) {
                if (other.someChild.get() != null) {
                    return false;
                }
            } else if (!someChild.get().equals(other.someChild.get())) {
                return false;
            }
            return true;
        }

        // CHECKSTYLE:ON

        @Override
        public String toString() {
            return "Root [notSynced=" + notSynced + ", someString=" + someString + ", someChild=" + someChild + "]";
        }
    }

    /**
     * This class is part of {@link Root}.
     * 
     * @see Root
     * @author raik.bieniek
     * 
     */
    private static final class Child {
        IntegerProperty childInt = new SimpleIntegerProperty();

        public Child() {

        }

        @Override
        public int hashCode() {
            return childInt.get();
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
            if (childInt.get() != other.childInt.get()) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "Child [childInt=" + childInt + "]";
        }
    }
}
