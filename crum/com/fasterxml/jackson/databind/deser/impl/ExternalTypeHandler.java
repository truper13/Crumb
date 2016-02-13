package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.SettableBeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class ExternalTypeHandler {
    private final HashMap<String, Integer> _nameToPropertyIndex;
    private final ExtTypedProperty[] _properties;
    private final TokenBuffer[] _tokens;
    private final String[] _typeIds;

    public static class Builder {
        private final HashMap<String, Integer> _nameToPropertyIndex;
        private final ArrayList<ExtTypedProperty> _properties;

        public Builder() {
            this._properties = new ArrayList();
            this._nameToPropertyIndex = new HashMap();
        }

        public void addExternal(SettableBeanProperty property, TypeDeserializer typeDeser) {
            Integer index = Integer.valueOf(this._properties.size());
            this._properties.add(new ExtTypedProperty(property, typeDeser));
            this._nameToPropertyIndex.put(property.getName(), index);
            this._nameToPropertyIndex.put(typeDeser.getPropertyName(), index);
        }

        public ExternalTypeHandler build() {
            return new ExternalTypeHandler((ExtTypedProperty[]) this._properties.toArray(new ExtTypedProperty[this._properties.size()]), this._nameToPropertyIndex, null, null);
        }
    }

    private static final class ExtTypedProperty {
        private final SettableBeanProperty _property;
        private final TypeDeserializer _typeDeserializer;
        private final String _typePropertyName;

        public ExtTypedProperty(SettableBeanProperty property, TypeDeserializer typeDeser) {
            this._property = property;
            this._typeDeserializer = typeDeser;
            this._typePropertyName = typeDeser.getPropertyName();
        }

        public boolean hasTypePropertyName(String n) {
            return n.equals(this._typePropertyName);
        }

        public boolean hasDefaultType() {
            return this._typeDeserializer.getDefaultImpl() != null;
        }

        public String getDefaultTypeId() {
            Class<?> defaultType = this._typeDeserializer.getDefaultImpl();
            if (defaultType == null) {
                return null;
            }
            return this._typeDeserializer.getTypeIdResolver().idFromValueAndType(null, defaultType);
        }

        public String getTypePropertyName() {
            return this._typePropertyName;
        }

        public SettableBeanProperty getProperty() {
            return this._property;
        }
    }

    protected ExternalTypeHandler(ExtTypedProperty[] properties, HashMap<String, Integer> nameToPropertyIndex, String[] typeIds, TokenBuffer[] tokens) {
        this._properties = properties;
        this._nameToPropertyIndex = nameToPropertyIndex;
        this._typeIds = typeIds;
        this._tokens = tokens;
    }

    protected ExternalTypeHandler(ExternalTypeHandler h) {
        this._properties = h._properties;
        this._nameToPropertyIndex = h._nameToPropertyIndex;
        int len = this._properties.length;
        this._typeIds = new String[len];
        this._tokens = new TokenBuffer[len];
    }

    public ExternalTypeHandler start() {
        return new ExternalTypeHandler(this);
    }

    public boolean handleTypePropertyValue(JsonParser jp, DeserializationContext ctxt, String propName, Object bean) throws IOException, JsonProcessingException {
        Integer I = (Integer) this._nameToPropertyIndex.get(propName);
        if (I == null) {
            return false;
        }
        int index = I.intValue();
        if (!this._properties[index].hasTypePropertyName(propName)) {
            return false;
        }
        boolean canDeserialize;
        String typeId = jp.getText();
        if (bean == null || this._tokens[index] == null) {
            canDeserialize = false;
        } else {
            canDeserialize = true;
        }
        if (canDeserialize) {
            _deserializeAndSet(jp, ctxt, bean, index, typeId);
            this._tokens[index] = null;
        } else {
            this._typeIds[index] = typeId;
        }
        return true;
    }

    public boolean handlePropertyValue(JsonParser jp, DeserializationContext ctxt, String propName, Object bean) throws IOException, JsonProcessingException {
        Integer I = (Integer) this._nameToPropertyIndex.get(propName);
        if (I == null) {
            return false;
        }
        boolean canDeserialize;
        int index = I.intValue();
        if (this._properties[index].hasTypePropertyName(propName)) {
            this._typeIds[index] = jp.getText();
            jp.skipChildren();
            if (bean == null || this._tokens[index] == null) {
                canDeserialize = false;
            } else {
                canDeserialize = true;
            }
        } else {
            TokenBuffer tokens = new TokenBuffer(jp);
            tokens.copyCurrentStructure(jp);
            this._tokens[index] = tokens;
            canDeserialize = (bean == null || this._typeIds[index] == null) ? false : true;
        }
        if (canDeserialize) {
            String typeId = this._typeIds[index];
            this._typeIds[index] = null;
            _deserializeAndSet(jp, ctxt, bean, index, typeId);
            this._tokens[index] = null;
        }
        return true;
    }

    public Object complete(JsonParser jp, DeserializationContext ctxt, Object bean) throws IOException, JsonProcessingException {
        int len = this._properties.length;
        for (int i = 0; i < len; i++) {
            String typeId = this._typeIds[i];
            if (typeId == null) {
                TokenBuffer tokens = this._tokens[i];
                if (tokens == null) {
                    continue;
                } else {
                    JsonToken t = tokens.firstToken();
                    if (t != null && t.isScalarValue()) {
                        JsonParser buffered = tokens.asParser(jp);
                        buffered.nextToken();
                        SettableBeanProperty extProp = this._properties[i].getProperty();
                        Object result = TypeDeserializer.deserializeIfNatural(buffered, ctxt, extProp.getType());
                        if (result != null) {
                            extProp.set(bean, result);
                        } else if (this._properties[i].hasDefaultType()) {
                            typeId = this._properties[i].getDefaultTypeId();
                        } else {
                            throw ctxt.mappingException("Missing external type id property '" + this._properties[i].getTypePropertyName() + "'");
                        }
                    }
                    _deserializeAndSet(jp, ctxt, bean, i, typeId);
                }
            } else {
                if (this._tokens[i] == null) {
                    throw ctxt.mappingException("Missing property '" + this._properties[i].getProperty().getName() + "' for external type id '" + this._properties[i].getTypePropertyName());
                }
                _deserializeAndSet(jp, ctxt, bean, i, typeId);
            }
        }
        return bean;
    }

    public Object complete(JsonParser jp, DeserializationContext ctxt, PropertyValueBuffer buffer, PropertyBasedCreator creator) throws IOException, JsonProcessingException {
        int i;
        int len = this._properties.length;
        Object[] values = new Object[len];
        for (i = 0; i < len; i++) {
            String typeId = this._typeIds[i];
            if (typeId != null) {
                if (this._tokens[i] == null) {
                    throw ctxt.mappingException("Missing property '" + this._properties[i].getProperty().getName() + "' for external type id '" + this._properties[i].getTypePropertyName());
                }
                values[i] = _deserialize(jp, ctxt, i, typeId);
            } else if (this._tokens[i] == null) {
                continue;
            } else if (this._properties[i].hasDefaultType()) {
                typeId = this._properties[i].getDefaultTypeId();
                values[i] = _deserialize(jp, ctxt, i, typeId);
            } else {
                throw ctxt.mappingException("Missing external type id property '" + this._properties[i].getTypePropertyName() + "'");
            }
        }
        for (i = 0; i < len; i++) {
            SettableBeanProperty prop = this._properties[i].getProperty();
            if (creator.findCreatorProperty(prop.getName()) != null) {
                buffer.assignParameter(prop.getCreatorIndex(), values[i]);
            }
        }
        Object bean = creator.build(ctxt, buffer);
        for (i = 0; i < len; i++) {
            prop = this._properties[i].getProperty();
            if (creator.findCreatorProperty(prop.getName()) == null) {
                prop.set(bean, values[i]);
            }
        }
        return bean;
    }

    protected final Object _deserialize(JsonParser jp, DeserializationContext ctxt, int index, String typeId) throws IOException, JsonProcessingException {
        TokenBuffer merged = new TokenBuffer(jp);
        merged.writeStartArray();
        merged.writeString(typeId);
        JsonParser p2 = this._tokens[index].asParser(jp);
        p2.nextToken();
        merged.copyCurrentStructure(p2);
        merged.writeEndArray();
        p2 = merged.asParser(jp);
        p2.nextToken();
        return this._properties[index].getProperty().deserialize(p2, ctxt);
    }

    protected final void _deserializeAndSet(JsonParser jp, DeserializationContext ctxt, Object bean, int index, String typeId) throws IOException, JsonProcessingException {
        TokenBuffer merged = new TokenBuffer(jp);
        merged.writeStartArray();
        merged.writeString(typeId);
        JsonParser p2 = this._tokens[index].asParser(jp);
        p2.nextToken();
        merged.copyCurrentStructure(p2);
        merged.writeEndArray();
        p2 = merged.asParser(jp);
        p2.nextToken();
        this._properties[index].getProperty().deserializeAndSet(p2, ctxt, bean);
    }
}
