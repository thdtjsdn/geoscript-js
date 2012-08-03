package org.geoscript.js.feature;

import java.util.List;

import org.geoscript.js.GeoObject;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.annotations.JSConstructor;
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSGetter;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class Schema extends GeoObject implements Wrapper {

    /** serialVersionUID */
    private static final long serialVersionUID = -1823488566532338763L;
    

    static Scriptable prototype;
    
    private SimpleFeatureType featureType;
    
    /**
     * Prototype constructor.
     */
    public Schema() {
    }

    private Schema(NativeObject config) {
        Object fieldsObj = config.get("fields");
        if (!(fieldsObj instanceof NativeArray)) {
            throw ScriptRuntime.constructError("Error", "Schema config must have a fields array.");
        }
        NativeArray fields = (NativeArray) fieldsObj;
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        Object nameObj = config.get("name");
        String name = "feature";
        if (nameObj instanceof String) {
            name = (String) nameObj;
        }
        builder.setName(new NameImpl(name));
        for (int i=0; i<fields.size(); ++i) {
            Object fieldObj = fields.get(i);
            AttributeDescriptor descriptor = null;
            if (fieldObj instanceof NativeObject) {
                Field field = new Field(getParentScope(), (NativeObject) fieldObj);
                descriptor = (AttributeDescriptor) field.unwrap();
            } else if (fieldObj instanceof AttributeDescriptor) {
                descriptor = (AttributeDescriptor) fieldObj;
            } else {
                throw ScriptRuntime.constructError("Error", "Provided fields must be Field instances or config objects.");
            }
            if (descriptor instanceof GeometryDescriptor) {
                CoordinateReferenceSystem crs = ((GeometryDescriptor) descriptor).getCoordinateReferenceSystem();
                builder.setCRS(crs);
            }
            builder.add(descriptor);
        }
        featureType = builder.buildFeatureType();
    }
    
    @JSGetter
    public String getName() {
        return featureType.getName().getLocalPart();
    }

    @JSGetter
    public Field getGeometry() {
        Field field = null;
        GeometryDescriptor descriptor = featureType.getGeometryDescriptor();
        if (descriptor != null) {
            field = new Field(getParentScope(), descriptor);
        }
        return field;
    }

    @JSGetter
    public NativeArray getFields() {
        Scriptable scope = getParentScope();
        Context cx = Context.getCurrentContext();
        if (cx == null) {
            throw new RuntimeException("No context associated with current thread.");
        }
        List<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();
        int length = descriptors.size();
        NativeArray array = (NativeArray) cx.newArray(scope, length);
        for (int i=0; i<length; ++i) {
            array.put(i, array, new Field(scope, descriptors.get(i)));
        }
        return array;
    }

    @JSGetter
    public NativeArray getFieldNames() {
        Scriptable scope = getParentScope();
        Context cx = Context.getCurrentContext();
        if (cx == null) {
            throw new RuntimeException("No context associated with current thread.");
        }
        List<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();
        int length = descriptors.size();
        NativeArray array = (NativeArray) cx.newArray(scope, length);
        for (int i=0; i<length; ++i) {
            array.put(i, array, descriptors.get(i).getLocalName());
        }
        return array;
    }

    @JSFunction
    public Field get(String name) {
        Field field = null;
        AttributeDescriptor descriptor = featureType.getDescriptor(name);
        if (descriptor != null) {
            field = new Field(getParentScope(), descriptor);
        }
        return field;
    }

    @JSGetter
    public Scriptable getConfig() {
        Scriptable config = super.getConfig();
        Scriptable scope = getParentScope();
        Context cx = Context.getCurrentContext();
        if (cx == null) {
            throw new RuntimeException("No context associated with current thread.");
        }
        List<AttributeDescriptor> descriptors = featureType.getAttributeDescriptors();
        int length = descriptors.size();
        NativeArray array = (NativeArray) cx.newArray(scope, length);
        for (int i=0; i<length; ++i) {
            Field field = new Field(scope, descriptors.get(i));
            field.getConfig();
            array.put(i, array, field.getConfig());
        }
        config.put("fields", config, array);
        config.put("name", config, getName());
        return config;
    }

    @JSConstructor
    public static Object constructor(Context cx, Object[] args, Function ctorObj, boolean inNewExpr) {
        if (!inNewExpr) {
            throw ScriptRuntime.constructError("Error", "Call constructor with new keyword.");
        }
        Schema schema = null;
        Object arg = args[0];
        if (arg instanceof NativeObject) {
            schema = new Schema((NativeObject) arg);
        }
        return schema;
    }

    public Object unwrap() {
        return featureType;
    }
    

    /**
     * Finishes JavaScript constructor initialization.
     * Sets up the prototype chain using superclass.
     * 
     * @param scope
     * @param ctor
     * @param prototype
     */
    public static void finishInit(Scriptable scope, FunctionObject ctor, Scriptable prototype) {
        Schema.prototype = prototype;
    }

}
