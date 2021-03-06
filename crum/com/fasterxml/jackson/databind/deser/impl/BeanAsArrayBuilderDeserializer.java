package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.deser.BeanDeserializerBase;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.util.NameTransformer;
import java.io.IOException;
import java.util.HashSet;

public class BeanAsArrayBuilderDeserializer extends BeanDeserializerBase {
    private static final long serialVersionUID = 1;
    protected final AnnotatedMethod _buildMethod;
    protected final BeanDeserializerBase _delegate;
    protected final SettableBeanProperty[] _orderedProperties;

    public BeanAsArrayBuilderDeserializer(BeanDeserializerBase delegate, SettableBeanProperty[] ordered, AnnotatedMethod buildMethod) {
        super(delegate);
        this._delegate = delegate;
        this._orderedProperties = ordered;
        this._buildMethod = buildMethod;
    }

    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper) {
        return this._delegate.unwrappingDeserializer(unwrapper);
    }

    public BeanAsArrayBuilderDeserializer withObjectIdReader(ObjectIdReader oir) {
        return new BeanAsArrayBuilderDeserializer(this._delegate.withObjectIdReader(oir), this._orderedProperties, this._buildMethod);
    }

    public BeanAsArrayBuilderDeserializer withIgnorableProperties(HashSet<String> ignorableProps) {
        return new BeanAsArrayBuilderDeserializer(this._delegate.withIgnorableProperties(ignorableProps), this._orderedProperties, this._buildMethod);
    }

    protected BeanAsArrayBuilderDeserializer asArrayDeserializer() {
        return this;
    }

    protected final Object finishBuild(DeserializationContext ctxt, Object builder) throws IOException {
        try {
            return this._buildMethod.getMember().invoke(builder, new Object[0]);
        } catch (Exception e) {
            wrapInstantiationProblem(e, ctxt);
            return null;
        }
    }

    public Object deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            return finishBuild(ctxt, _deserializeFromNonArray(jp, ctxt));
        }
        if (!this._vanillaProcessing) {
            return finishBuild(ctxt, _deserializeNonVanilla(jp, ctxt));
        }
        Object builder = this._valueInstantiator.createUsingDefault(ctxt);
        SettableBeanProperty[] props = this._orderedProperties;
        int i = 0;
        int propCount = props.length;
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            if (i != propCount) {
                SettableBeanProperty prop = props[i];
                if (prop != null) {
                    try {
                        builder = prop.deserializeSetAndReturn(jp, ctxt, builder);
                    } catch (Exception e) {
                        wrapAndThrow((Throwable) e, builder, prop.getName(), ctxt);
                    }
                } else {
                    jp.skipChildren();
                }
                i++;
            } else if (this._ignoreAllUnknown) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    jp.skipChildren();
                }
                return finishBuild(ctxt, builder);
            } else {
                throw ctxt.mappingException("Unexpected JSON values; expected at most " + propCount + " properties (in JSON Array)");
            }
        }
        return finishBuild(ctxt, builder);
    }

    public Object deserialize(JsonParser jp, DeserializationContext ctxt, Object builder) throws IOException, JsonProcessingException {
        if (this._injectables != null) {
            injectValues(ctxt, builder);
        }
        SettableBeanProperty[] props = this._orderedProperties;
        int i = 0;
        int propCount = props.length;
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            if (i != propCount) {
                SettableBeanProperty prop = props[i];
                if (prop != null) {
                    try {
                        builder = prop.deserializeSetAndReturn(jp, ctxt, builder);
                    } catch (Exception e) {
                        wrapAndThrow((Throwable) e, builder, prop.getName(), ctxt);
                    }
                } else {
                    jp.skipChildren();
                }
                i++;
            } else if (this._ignoreAllUnknown) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    jp.skipChildren();
                }
                return finishBuild(ctxt, builder);
            } else {
                throw ctxt.mappingException("Unexpected JSON values; expected at most " + propCount + " properties (in JSON Array)");
            }
        }
        return finishBuild(ctxt, builder);
    }

    public Object deserializeFromObject(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        return _deserializeFromNonArray(jp, ctxt);
    }

    protected Object _deserializeNonVanilla(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        if (this._nonStandardCreation) {
            return _deserializeWithCreator(jp, ctxt);
        }
        Object builder = this._valueInstantiator.createUsingDefault(ctxt);
        if (this._injectables != null) {
            injectValues(ctxt, builder);
        }
        Class<?> activeView = this._needViewProcesing ? ctxt.getActiveView() : null;
        SettableBeanProperty[] props = this._orderedProperties;
        int i = 0;
        int propCount = props.length;
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            if (i != propCount) {
                SettableBeanProperty prop = props[i];
                i++;
                if (prop == null || !(activeView == null || prop.visibleInView(activeView))) {
                    jp.skipChildren();
                } else {
                    try {
                        prop.deserializeSetAndReturn(jp, ctxt, builder);
                    } catch (Exception e) {
                        wrapAndThrow((Throwable) e, builder, prop.getName(), ctxt);
                    }
                }
            } else if (this._ignoreAllUnknown) {
                while (jp.nextToken() != JsonToken.END_ARRAY) {
                    jp.skipChildren();
                }
                return builder;
            } else {
                throw ctxt.mappingException("Unexpected JSON values; expected at most " + propCount + " properties (in JSON Array)");
            }
        }
        return builder;
    }

    protected Object _deserializeWithCreator(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        if (this._delegateDeserializer != null) {
            return this._valueInstantiator.createUsingDelegate(ctxt, this._delegateDeserializer.deserialize(jp, ctxt));
        }
        if (this._propertyBasedCreator != null) {
            return _deserializeUsingPropertyBased(jp, ctxt);
        }
        if (this._beanType.isAbstract()) {
            throw JsonMappingException.from(jp, "Can not instantiate abstract type " + this._beanType + " (need to add/enable type information?)");
        }
        throw JsonMappingException.from(jp, "No suitable constructor found for type " + this._beanType + ": can not instantiate from JSON object (need to add/enable type information?)");
    }

    protected final Object _deserializeUsingPropertyBased(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        PropertyBasedCreator creator = this._propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt, this._objectIdReader);
        SettableBeanProperty[] props = this._orderedProperties;
        int propCount = props.length;
        int i = 0;
        Object obj = null;
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            SettableBeanProperty prop = i < propCount ? props[i] : null;
            if (prop == null) {
                jp.skipChildren();
            } else if (obj != null) {
                try {
                    obj = prop.deserializeSetAndReturn(jp, ctxt, obj);
                } catch (Exception e) {
                    wrapAndThrow((Throwable) e, obj, prop.getName(), ctxt);
                }
            } else {
                String propName = prop.getName();
                SettableBeanProperty creatorProp = creator.findCreatorProperty(propName);
                if (creatorProp != null) {
                    if (buffer.assignParameter(creatorProp.getCreatorIndex(), creatorProp.deserialize(jp, ctxt))) {
                        try {
                            obj = creator.build(ctxt, buffer);
                            if (obj.getClass() != this._beanType.getRawClass()) {
                                throw ctxt.mappingException("Can not support implicit polymorphic deserialization for POJOs-as-Arrays style: nominal type " + this._beanType.getRawClass().getName() + ", actual type " + obj.getClass().getName());
                            }
                        } catch (Exception e2) {
                            wrapAndThrow((Throwable) e2, (Object) this._beanType.getRawClass(), propName, ctxt);
                        }
                    } else {
                        continue;
                    }
                } else if (!buffer.readIdProperty(propName)) {
                    buffer.bufferProperty(prop, prop.deserialize(jp, ctxt));
                }
            }
            i++;
        }
        if (obj != null) {
            return obj;
        }
        try {
            return creator.build(ctxt, buffer);
        } catch (Exception e22) {
            wrapInstantiationProblem(e22, ctxt);
            return null;
        }
    }

    protected Object _deserializeFromNonArray(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        throw ctxt.mappingException("Can not deserialize a POJO (of type " + this._beanType.getRawClass().getName() + ") from non-Array representation (token: " + jp.getCurrentToken() + "): type/property designed to be serialized as JSON Array");
    }
}
