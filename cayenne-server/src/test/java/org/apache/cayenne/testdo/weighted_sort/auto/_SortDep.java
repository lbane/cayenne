package org.apache.cayenne.testdo.weighted_sort.auto;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.apache.cayenne.BaseDataObject;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.property.EntityProperty;
import org.apache.cayenne.exp.property.NumericProperty;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.testdo.weighted_sort.SortRoot;

/**
 * Class _SortDep was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _SortDep extends BaseDataObject {

    private static final long serialVersionUID = 1L; 

    public static final NumericProperty<Long> ID_PK_PROPERTY = PropertyFactory.createNumeric(ExpressionFactory.dbPathExp("ID"), Long.class);
    public static final String ID_PK_COLUMN = "ID";

    public static final EntityProperty<SortRoot> ROOT = PropertyFactory.createEntity("root", SortRoot.class);


    protected Object root;

    public void setRoot(SortRoot root) {
        setToOneTarget("root", root, true);
    }

    public SortRoot getRoot() {
        return (SortRoot)readProperty("root");
    }

    @Override
    public Object readPropertyDirectly(String propName) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch(propName) {
            case "root":
                return this.root;
            default:
                return super.readPropertyDirectly(propName);
        }
    }

    @Override
    public void writePropertyDirectly(String propName, Object val) {
        if(propName == null) {
            throw new IllegalArgumentException();
        }

        switch (propName) {
            case "root":
                this.root = val;
                break;
            default:
                super.writePropertyDirectly(propName, val);
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        writeSerialized(out);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        readSerialized(in);
    }

    @Override
    protected void writeState(ObjectOutputStream out) throws IOException {
        super.writeState(out);
        out.writeObject(this.root);
    }

    @Override
    protected void readState(ObjectInputStream in) throws IOException, ClassNotFoundException {
        super.readState(in);
        this.root = in.readObject();
    }

}
