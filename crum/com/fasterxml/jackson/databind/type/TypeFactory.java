package com.fasterxml.jackson.databind.type;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.util.ArrayBuilders;
import com.fasterxml.jackson.databind.util.LRUMap;
import java.io.Serializable;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TypeFactory implements Serializable {
    protected static final SimpleType CORE_TYPE_BOOL;
    protected static final SimpleType CORE_TYPE_INT;
    protected static final SimpleType CORE_TYPE_LONG;
    protected static final SimpleType CORE_TYPE_STRING;
    private static final JavaType[] NO_TYPES;
    protected static final TypeFactory instance;
    private static final long serialVersionUID = 1;
    protected transient HierarchicType _cachedArrayListType;
    protected transient HierarchicType _cachedHashMapType;
    protected final TypeModifier[] _modifiers;
    protected final TypeParser _parser;
    protected final LRUMap<ClassKey, JavaType> _typeCache;

    static {
        NO_TYPES = new JavaType[0];
        instance = new TypeFactory();
        CORE_TYPE_STRING = new SimpleType(String.class);
        CORE_TYPE_BOOL = new SimpleType(Boolean.TYPE);
        CORE_TYPE_INT = new SimpleType(Integer.TYPE);
        CORE_TYPE_LONG = new SimpleType(Long.TYPE);
    }

    private TypeFactory() {
        this._typeCache = new LRUMap(16, 100);
        this._parser = new TypeParser(this);
        this._modifiers = null;
    }

    protected TypeFactory(TypeParser p, TypeModifier[] mods) {
        this._typeCache = new LRUMap(16, 100);
        this._parser = p;
        this._modifiers = mods;
    }

    public TypeFactory withModifier(TypeModifier mod) {
        if (mod == null) {
            return new TypeFactory(this._parser, this._modifiers);
        }
        if (this._modifiers != null) {
            return new TypeFactory(this._parser, (TypeModifier[]) ArrayBuilders.insertInListNoDup(this._modifiers, mod));
        }
        return new TypeFactory(this._parser, new TypeModifier[]{mod});
    }

    public static TypeFactory defaultInstance() {
        return instance;
    }

    public void clearCache() {
        this._typeCache.clear();
    }

    public static JavaType unknownType() {
        return defaultInstance()._unknownType();
    }

    public static Class<?> rawClass(Type t) {
        if (t instanceof Class) {
            return (Class) t;
        }
        return defaultInstance().constructType(t).getRawClass();
    }

    public JavaType constructSpecializedType(JavaType baseType, Class<?> subclass) {
        if (baseType.getRawClass() == subclass) {
            return baseType;
        }
        if (!(baseType instanceof SimpleType) || (!subclass.isArray() && !Map.class.isAssignableFrom(subclass) && !Collection.class.isAssignableFrom(subclass))) {
            return baseType.narrowBy(subclass);
        }
        if (baseType.getRawClass().isAssignableFrom(subclass)) {
            JavaType subtype = _fromClass(subclass, new TypeBindings(this, baseType.getRawClass()));
            Object h = baseType.getValueHandler();
            if (h != null) {
                subtype = subtype.withValueHandler(h);
            }
            h = baseType.getTypeHandler();
            if (h != null) {
                subtype = subtype.withTypeHandler(h);
            }
            return subtype;
        }
        throw new IllegalArgumentException("Class " + subclass.getClass().getName() + " not subtype of " + baseType);
    }

    public JavaType constructFromCanonical(String canonical) throws IllegalArgumentException {
        return this._parser.parse(canonical);
    }

    public JavaType[] findTypeParameters(JavaType type, Class<?> expType) {
        Class<?> raw = type.getRawClass();
        if (raw != expType) {
            return findTypeParameters(raw, expType, new TypeBindings(this, type));
        }
        int count = type.containedTypeCount();
        if (count == 0) {
            return null;
        }
        JavaType[] result = new JavaType[count];
        for (int i = 0; i < count; i++) {
            result[i] = type.containedType(i);
        }
        return result;
    }

    public JavaType[] findTypeParameters(Class<?> clz, Class<?> expType) {
        return findTypeParameters(clz, expType, new TypeBindings(this, (Class) clz));
    }

    public JavaType[] findTypeParameters(Class<?> clz, Class<?> expType, TypeBindings bindings) {
        HierarchicType subType = _findSuperTypeChain(clz, expType);
        if (subType == null) {
            throw new IllegalArgumentException("Class " + clz.getName() + " is not a subtype of " + expType.getName());
        }
        HierarchicType superType = subType;
        while (superType.getSuperType() != null) {
            superType = superType.getSuperType();
            Class raw = superType.getRawClass();
            TypeBindings newBindings = new TypeBindings(this, raw);
            if (superType.isGeneric()) {
                Type[] actualTypes = superType.asGeneric().getActualTypeArguments();
                TypeVariable<?>[] vars = raw.getTypeParameters();
                int len = actualTypes.length;
                for (int i = 0; i < len; i++) {
                    newBindings.addBinding(vars[i].getName(), _constructType(actualTypes[i], bindings));
                }
            }
            bindings = newBindings;
        }
        if (superType.isGeneric()) {
            return bindings.typesAsArray();
        }
        return null;
    }

    public JavaType moreSpecificType(JavaType type1, JavaType type2) {
        if (type1 == null) {
            return type2;
        }
        if (type2 == null) {
            return type1;
        }
        Class<?> raw1 = type1.getRawClass();
        Class<?> raw2 = type2.getRawClass();
        if (raw1 == raw2 || !raw1.isAssignableFrom(raw2)) {
            return type1;
        }
        return type2;
    }

    public JavaType constructType(Type type) {
        return _constructType(type, null);
    }

    public JavaType constructType(Type type, TypeBindings bindings) {
        return _constructType(type, bindings);
    }

    public JavaType constructType(TypeReference<?> typeRef) {
        return _constructType(typeRef.getType(), null);
    }

    public JavaType constructType(Type type, Class<?> context) {
        return _constructType(type, context == null ? null : new TypeBindings(this, (Class) context));
    }

    public JavaType constructType(Type type, JavaType context) {
        return _constructType(type, context == null ? null : new TypeBindings(this, context));
    }

    protected JavaType _constructType(Type type, TypeBindings context) {
        JavaType resultType;
        if (type instanceof Class) {
            resultType = _fromClass((Class) type, context);
        } else if (type instanceof ParameterizedType) {
            resultType = _fromParamType((ParameterizedType) type, context);
        } else if (type instanceof JavaType) {
            return (JavaType) type;
        } else {
            if (type instanceof GenericArrayType) {
                resultType = _fromArrayType((GenericArrayType) type, context);
            } else if (type instanceof TypeVariable) {
                resultType = _fromVariable((TypeVariable) type, context);
            } else if (type instanceof WildcardType) {
                resultType = _fromWildcard((WildcardType) type, context);
            } else {
                throw new IllegalArgumentException("Unrecognized Type: " + (type == null ? "[null]" : type.toString()));
            }
        }
        if (!(this._modifiers == null || resultType.isContainerType())) {
            for (TypeModifier mod : this._modifiers) {
                resultType = mod.modifyType(resultType, type, context, this);
            }
        }
        return resultType;
    }

    public ArrayType constructArrayType(Class<?> elementType) {
        return ArrayType.construct(_constructType(elementType, null), null, null);
    }

    public ArrayType constructArrayType(JavaType elementType) {
        return ArrayType.construct(elementType, null, null);
    }

    public CollectionType constructCollectionType(Class<? extends Collection> collectionClass, Class<?> elementClass) {
        return CollectionType.construct(collectionClass, constructType((Type) elementClass));
    }

    public CollectionType constructCollectionType(Class<? extends Collection> collectionClass, JavaType elementType) {
        return CollectionType.construct(collectionClass, elementType);
    }

    public CollectionLikeType constructCollectionLikeType(Class<?> collectionClass, Class<?> elementClass) {
        return CollectionLikeType.construct(collectionClass, constructType((Type) elementClass));
    }

    public CollectionLikeType constructCollectionLikeType(Class<?> collectionClass, JavaType elementType) {
        return CollectionLikeType.construct(collectionClass, elementType);
    }

    public MapType constructMapType(Class<? extends Map> mapClass, JavaType keyType, JavaType valueType) {
        return MapType.construct(mapClass, keyType, valueType);
    }

    public MapType constructMapType(Class<? extends Map> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return MapType.construct(mapClass, constructType((Type) keyClass), constructType((Type) valueClass));
    }

    public MapLikeType constructMapLikeType(Class<?> mapClass, JavaType keyType, JavaType valueType) {
        return MapLikeType.construct(mapClass, keyType, valueType);
    }

    public MapLikeType constructMapLikeType(Class<?> mapClass, Class<?> keyClass, Class<?> valueClass) {
        return MapType.construct(mapClass, constructType((Type) keyClass), constructType((Type) valueClass));
    }

    public JavaType constructSimpleType(Class<?> rawType, JavaType[] parameterTypes) {
        TypeVariable<?>[] typeVars = rawType.getTypeParameters();
        if (typeVars.length != parameterTypes.length) {
            throw new IllegalArgumentException("Parameter type mismatch for " + rawType.getName() + ": expected " + typeVars.length + " parameters, was given " + parameterTypes.length);
        }
        String[] names = new String[typeVars.length];
        int len = typeVars.length;
        for (int i = 0; i < len; i++) {
            names[i] = typeVars[i].getName();
        }
        return new SimpleType(rawType, names, parameterTypes, null, null, false);
    }

    public JavaType uncheckedSimpleType(Class<?> cls) {
        return new SimpleType(cls);
    }

    public JavaType constructParametricType(Class<?> parametrized, Class<?>... parameterClasses) {
        int len = parameterClasses.length;
        JavaType[] pt = new JavaType[len];
        for (int i = 0; i < len; i++) {
            pt[i] = _fromClass(parameterClasses[i], null);
        }
        return constructParametricType((Class) parametrized, pt);
    }

    public JavaType constructParametricType(Class<?> parametrized, JavaType... parameterTypes) {
        if (parametrized.isArray()) {
            if (parameterTypes.length == 1) {
                return constructArrayType(parameterTypes[0]);
            }
            throw new IllegalArgumentException("Need exactly 1 parameter type for arrays (" + parametrized.getName() + ")");
        } else if (Map.class.isAssignableFrom(parametrized)) {
            if (parameterTypes.length == 2) {
                return constructMapType((Class) parametrized, parameterTypes[0], parameterTypes[1]);
            }
            throw new IllegalArgumentException("Need exactly 2 parameter types for Map types (" + parametrized.getName() + ")");
        } else if (!Collection.class.isAssignableFrom(parametrized)) {
            return constructSimpleType(parametrized, parameterTypes);
        } else {
            if (parameterTypes.length == 1) {
                return constructCollectionType((Class) parametrized, parameterTypes[0]);
            }
            throw new IllegalArgumentException("Need exactly 1 parameter type for Collection types (" + parametrized.getName() + ")");
        }
    }

    public CollectionType constructRawCollectionType(Class<? extends Collection> collectionClass) {
        return CollectionType.construct(collectionClass, unknownType());
    }

    public CollectionLikeType constructRawCollectionLikeType(Class<?> collectionClass) {
        return CollectionLikeType.construct(collectionClass, unknownType());
    }

    public MapType constructRawMapType(Class<? extends Map> mapClass) {
        return MapType.construct(mapClass, unknownType(), unknownType());
    }

    public MapLikeType constructRawMapLikeType(Class<?> mapClass) {
        return MapLikeType.construct(mapClass, unknownType(), unknownType());
    }

    protected JavaType _fromClass(Class<?> clz, TypeBindings context) {
        if (clz == String.class) {
            return CORE_TYPE_STRING;
        }
        if (clz == Boolean.TYPE) {
            return CORE_TYPE_BOOL;
        }
        if (clz == Integer.TYPE) {
            return CORE_TYPE_INT;
        }
        if (clz == Long.TYPE) {
            return CORE_TYPE_LONG;
        }
        ClassKey key = new ClassKey(clz);
        JavaType result = (JavaType) this._typeCache.get(key);
        if (result != null) {
            return result;
        }
        if (clz.isArray()) {
            result = ArrayType.construct(_constructType(clz.getComponentType(), null), null, null);
        } else if (clz.isEnum()) {
            result = new SimpleType(clz);
        } else if (Map.class.isAssignableFrom(clz)) {
            result = _mapType(clz);
        } else if (Collection.class.isAssignableFrom(clz)) {
            result = _collectionType(clz);
        } else {
            result = new SimpleType(clz);
        }
        this._typeCache.put(key, result);
        return result;
    }

    protected JavaType _fromParameterizedClass(Class<?> clz, List<JavaType> paramTypes) {
        if (clz.isArray()) {
            return ArrayType.construct(_constructType(clz.getComponentType(), null), null, null);
        }
        if (clz.isEnum()) {
            return new SimpleType(clz);
        }
        if (Map.class.isAssignableFrom(clz)) {
            if (paramTypes.size() <= 0) {
                return _mapType(clz);
            }
            return MapType.construct(clz, (JavaType) paramTypes.get(0), paramTypes.size() >= 2 ? (JavaType) paramTypes.get(1) : _unknownType());
        } else if (Collection.class.isAssignableFrom(clz)) {
            if (paramTypes.size() >= 1) {
                return CollectionType.construct(clz, (JavaType) paramTypes.get(0));
            }
            return _collectionType(clz);
        } else if (paramTypes.size() == 0) {
            return new SimpleType(clz);
        } else {
            return constructSimpleType(clz, (JavaType[]) paramTypes.toArray(new JavaType[paramTypes.size()]));
        }
    }

    protected JavaType _fromParamType(ParameterizedType type, TypeBindings context) {
        JavaType[] pt;
        Class<?> rawType = (Class) type.getRawType();
        Type[] args = type.getActualTypeArguments();
        int paramCount = args == null ? 0 : args.length;
        if (paramCount == 0) {
            pt = NO_TYPES;
        } else {
            pt = new JavaType[paramCount];
            for (int i = 0; i < paramCount; i++) {
                pt[i] = _constructType(args[i], context);
            }
        }
        if (Map.class.isAssignableFrom(rawType)) {
            JavaType[] mapParams = findTypeParameters(constructSimpleType(rawType, pt), Map.class);
            if (mapParams.length == 2) {
                return MapType.construct(rawType, mapParams[0], mapParams[1]);
            }
            throw new IllegalArgumentException("Could not find 2 type parameters for Map class " + rawType.getName() + " (found " + mapParams.length + ")");
        } else if (Collection.class.isAssignableFrom(rawType)) {
            JavaType[] collectionParams = findTypeParameters(constructSimpleType(rawType, pt), Collection.class);
            if (collectionParams.length == 1) {
                return CollectionType.construct(rawType, collectionParams[0]);
            }
            throw new IllegalArgumentException("Could not find 1 type parameter for Collection class " + rawType.getName() + " (found " + collectionParams.length + ")");
        } else if (paramCount == 0) {
            return new SimpleType(rawType);
        } else {
            return constructSimpleType(rawType, pt);
        }
    }

    protected JavaType _fromArrayType(GenericArrayType type, TypeBindings context) {
        return ArrayType.construct(_constructType(type.getGenericComponentType(), context), null, null);
    }

    protected JavaType _fromVariable(TypeVariable<?> type, TypeBindings context) {
        if (context == null) {
            return _unknownType();
        }
        String name = type.getName();
        JavaType actualType = context.findType(name);
        if (actualType != null) {
            return actualType;
        }
        Type[] bounds = type.getBounds();
        context._addPlaceholder(name);
        return _constructType(bounds[0], context);
    }

    protected JavaType _fromWildcard(WildcardType type, TypeBindings context) {
        return _constructType(type.getUpperBounds()[0], context);
    }

    private JavaType _mapType(Class<?> rawClass) {
        JavaType[] typeParams = findTypeParameters((Class) rawClass, Map.class);
        if (typeParams == null) {
            return MapType.construct(rawClass, _unknownType(), _unknownType());
        }
        if (typeParams.length == 2) {
            return MapType.construct(rawClass, typeParams[0], typeParams[1]);
        }
        throw new IllegalArgumentException("Strange Map type " + rawClass.getName() + ": can not determine type parameters");
    }

    private JavaType _collectionType(Class<?> rawClass) {
        JavaType[] typeParams = findTypeParameters((Class) rawClass, Collection.class);
        if (typeParams == null) {
            return CollectionType.construct(rawClass, _unknownType());
        }
        if (typeParams.length == 1) {
            return CollectionType.construct(rawClass, typeParams[0]);
        }
        throw new IllegalArgumentException("Strange Collection type " + rawClass.getName() + ": can not determine type parameters");
    }

    protected JavaType _resolveVariableViaSubTypes(HierarchicType leafType, String variableName, TypeBindings bindings) {
        if (leafType != null && leafType.isGeneric()) {
            TypeVariable<?>[] typeVariables = leafType.getRawClass().getTypeParameters();
            int len = typeVariables.length;
            for (int i = 0; i < len; i++) {
                if (variableName.equals(typeVariables[i].getName())) {
                    Type type = leafType.asGeneric().getActualTypeArguments()[i];
                    if (type instanceof TypeVariable) {
                        return _resolveVariableViaSubTypes(leafType.getSubType(), ((TypeVariable) type).getName(), bindings);
                    }
                    return _constructType(type, bindings);
                }
            }
        }
        return _unknownType();
    }

    protected JavaType _unknownType() {
        return new SimpleType(Object.class);
    }

    protected HierarchicType _findSuperTypeChain(Class<?> subtype, Class<?> supertype) {
        if (supertype.isInterface()) {
            return _findSuperInterfaceChain(subtype, supertype);
        }
        return _findSuperClassChain(subtype, supertype);
    }

    protected HierarchicType _findSuperClassChain(Type currentType, Class<?> target) {
        HierarchicType current = new HierarchicType(currentType);
        Class<?> raw = current.getRawClass();
        if (raw == target) {
            return current;
        }
        Type parent = raw.getGenericSuperclass();
        if (parent != null) {
            HierarchicType sup = _findSuperClassChain(parent, target);
            if (sup != null) {
                sup.setSubType(current);
                current.setSuperType(sup);
                return current;
            }
        }
        return null;
    }

    protected HierarchicType _findSuperInterfaceChain(Type currentType, Class<?> target) {
        HierarchicType current = new HierarchicType(currentType);
        Class<?> raw = current.getRawClass();
        if (raw == target) {
            return new HierarchicType(currentType);
        }
        if (raw == HashMap.class && target == Map.class) {
            return _hashMapSuperInterfaceChain(current);
        }
        if (raw == ArrayList.class && target == List.class) {
            return _arrayListSuperInterfaceChain(current);
        }
        return _doFindSuperInterfaceChain(current, target);
    }

    protected HierarchicType _doFindSuperInterfaceChain(HierarchicType current, Class<?> target) {
        HierarchicType sup;
        Class<?> raw = current.getRawClass();
        Type[] parents = raw.getGenericInterfaces();
        if (parents != null) {
            for (Type parent : parents) {
                sup = _findSuperInterfaceChain(parent, target);
                if (sup != null) {
                    sup.setSubType(current);
                    current.setSuperType(sup);
                    return current;
                }
            }
        }
        Type parent2 = raw.getGenericSuperclass();
        if (parent2 != null) {
            sup = _findSuperInterfaceChain(parent2, target);
            if (sup != null) {
                sup.setSubType(current);
                current.setSuperType(sup);
                return current;
            }
        }
        return null;
    }

    protected synchronized HierarchicType _hashMapSuperInterfaceChain(HierarchicType current) {
        if (this._cachedHashMapType == null) {
            HierarchicType base = current.deepCloneWithoutSubtype();
            _doFindSuperInterfaceChain(base, Map.class);
            this._cachedHashMapType = base.getSuperType();
        }
        HierarchicType t = this._cachedHashMapType.deepCloneWithoutSubtype();
        current.setSuperType(t);
        t.setSubType(current);
        return current;
    }

    protected synchronized HierarchicType _arrayListSuperInterfaceChain(HierarchicType current) {
        if (this._cachedArrayListType == null) {
            HierarchicType base = current.deepCloneWithoutSubtype();
            _doFindSuperInterfaceChain(base, List.class);
            this._cachedArrayListType = base.getSuperType();
        }
        HierarchicType t = this._cachedArrayListType.deepCloneWithoutSubtype();
        current.setSuperType(t);
        t.setSubType(current);
        return current;
    }
}
