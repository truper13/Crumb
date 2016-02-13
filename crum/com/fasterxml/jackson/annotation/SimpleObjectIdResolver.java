package com.fasterxml.jackson.annotation;

import com.fasterxml.jackson.annotation.ObjectIdGenerator.IdKey;
import java.util.HashMap;
import java.util.Map;

public class SimpleObjectIdResolver implements ObjectIdResolver {
    private Map<IdKey, Object> _items;

    public SimpleObjectIdResolver() {
        this._items = new HashMap();
    }

    public void bindItem(IdKey id, Object ob) {
        if (this._items.containsKey(id)) {
            throw new IllegalStateException("Already had POJO for id (" + id.key.getClass().getName() + ") [" + id + "]");
        }
        this._items.put(id, ob);
    }

    public Object resolveId(IdKey id) {
        return this._items.get(id);
    }

    public boolean canUseFor(ObjectIdResolver resolverType) {
        return resolverType.getClass() == getClass();
    }

    public ObjectIdResolver newForDeserialization(Object context) {
        return this;
    }
}
