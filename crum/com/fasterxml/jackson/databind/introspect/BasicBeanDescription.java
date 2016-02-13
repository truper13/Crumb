package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonFormat.Value;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.AnnotationIntrospector.ReferenceProperty;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.type.TypeBindings;
import com.fasterxml.jackson.databind.util.Annotations;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.Converter.None;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BasicBeanDescription extends BeanDescription {
    protected final AnnotationIntrospector _annotationIntrospector;
    protected AnnotatedMember _anyGetter;
    protected AnnotatedMethod _anySetterMethod;
    protected TypeBindings _bindings;
    protected final AnnotatedClass _classInfo;
    protected final MapperConfig<?> _config;
    protected Set<String> _ignoredPropertyNames;
    protected Map<Object, AnnotatedMember> _injectables;
    protected AnnotatedMethod _jsonValueMethod;
    protected ObjectIdInfo _objectIdInfo;
    protected final List<BeanPropertyDefinition> _properties;

    protected BasicBeanDescription(MapperConfig<?> config, JavaType type, AnnotatedClass classDef, List<BeanPropertyDefinition> props) {
        super(type);
        this._config = config;
        this._annotationIntrospector = config == null ? null : config.getAnnotationIntrospector();
        this._classInfo = classDef;
        this._properties = props;
    }

    protected BasicBeanDescription(POJOPropertiesCollector coll) {
        this(coll.getConfig(), coll.getType(), coll.getClassDef(), coll.getProperties());
        this._objectIdInfo = coll.getObjectIdInfo();
    }

    public static BasicBeanDescription forDeserialization(POJOPropertiesCollector coll) {
        BasicBeanDescription desc = new BasicBeanDescription(coll);
        desc._anySetterMethod = coll.getAnySetterMethod();
        desc._ignoredPropertyNames = coll.getIgnoredPropertyNames();
        desc._injectables = coll.getInjectables();
        desc._jsonValueMethod = coll.getJsonValueMethod();
        return desc;
    }

    public static BasicBeanDescription forSerialization(POJOPropertiesCollector coll) {
        BasicBeanDescription desc = new BasicBeanDescription(coll);
        desc._jsonValueMethod = coll.getJsonValueMethod();
        desc._anyGetter = coll.getAnyGetter();
        return desc;
    }

    public static BasicBeanDescription forOtherUse(MapperConfig<?> config, JavaType type, AnnotatedClass ac) {
        return new BasicBeanDescription(config, type, ac, Collections.emptyList());
    }

    public boolean removeProperty(String propName) {
        Iterator<BeanPropertyDefinition> it = this._properties.iterator();
        while (it.hasNext()) {
            if (((BeanPropertyDefinition) it.next()).getName().equals(propName)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public AnnotatedClass getClassInfo() {
        return this._classInfo;
    }

    public ObjectIdInfo getObjectIdInfo() {
        return this._objectIdInfo;
    }

    public List<BeanPropertyDefinition> findProperties() {
        return this._properties;
    }

    public AnnotatedMethod findJsonValueMethod() {
        return this._jsonValueMethod;
    }

    public Set<String> getIgnoredPropertyNames() {
        if (this._ignoredPropertyNames == null) {
            return Collections.emptySet();
        }
        return this._ignoredPropertyNames;
    }

    public boolean hasKnownClassAnnotations() {
        return this._classInfo.hasAnnotations();
    }

    public Annotations getClassAnnotations() {
        return this._classInfo.getAnnotations();
    }

    public TypeBindings bindingsForBeanType() {
        if (this._bindings == null) {
            this._bindings = new TypeBindings(this._config.getTypeFactory(), this._type);
        }
        return this._bindings;
    }

    public JavaType resolveType(Type jdkType) {
        if (jdkType == null) {
            return null;
        }
        return bindingsForBeanType().resolveType(jdkType);
    }

    public AnnotatedConstructor findDefaultConstructor() {
        return this._classInfo.getDefaultConstructor();
    }

    public AnnotatedMethod findAnySetter() throws IllegalArgumentException {
        if (this._anySetterMethod != null) {
            Class<?> type = this._anySetterMethod.getRawParameterType(0);
            if (!(type == String.class || type == Object.class)) {
                throw new IllegalArgumentException("Invalid 'any-setter' annotation on method " + this._anySetterMethod.getName() + "(): first argument not of type String or Object, but " + type.getName());
            }
        }
        return this._anySetterMethod;
    }

    public Map<Object, AnnotatedMember> findInjectables() {
        return this._injectables;
    }

    public List<AnnotatedConstructor> getConstructors() {
        return this._classInfo.getConstructors();
    }

    public Object instantiateBean(boolean fixAccess) {
        AnnotatedConstructor ac = this._classInfo.getDefaultConstructor();
        if (ac == null) {
            return null;
        }
        if (fixAccess) {
            ac.fixAccess();
        }
        try {
            return ac.getAnnotated().newInstance(new Object[0]);
        } catch (Throwable e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof Error) {
                throw ((Error) t);
            } else if (t instanceof RuntimeException) {
                throw ((RuntimeException) t);
            } else {
                throw new IllegalArgumentException("Failed to instantiate bean of type " + this._classInfo.getAnnotated().getName() + ": (" + t.getClass().getName() + ") " + t.getMessage(), t);
            }
        }
    }

    public AnnotatedMethod findMethod(String name, Class<?>[] paramTypes) {
        return this._classInfo.findMethod(name, paramTypes);
    }

    public Value findExpectedFormat(Value defValue) {
        if (this._annotationIntrospector != null) {
            Value v = this._annotationIntrospector.findFormat(this._classInfo);
            if (v != null) {
                return v;
            }
        }
        return defValue;
    }

    public Converter<Object, Object> findSerializationConverter() {
        if (this._annotationIntrospector == null) {
            return null;
        }
        return _createConverter(this._annotationIntrospector.findSerializationConverter(this._classInfo));
    }

    public Include findSerializationInclusion(Include defValue) {
        return this._annotationIntrospector == null ? defValue : this._annotationIntrospector.findSerializationInclusion(this._classInfo, defValue);
    }

    public AnnotatedMember findAnyGetter() throws IllegalArgumentException {
        if (this._anyGetter != null) {
            if (!Map.class.isAssignableFrom(this._anyGetter.getRawType())) {
                throw new IllegalArgumentException("Invalid 'any-getter' annotation on method " + this._anyGetter.getName() + "(): return type is not instance of java.util.Map");
            }
        }
        return this._anyGetter;
    }

    public Map<String, AnnotatedMember> findBackReferenceProperties() {
        HashMap<String, AnnotatedMember> result = null;
        for (BeanPropertyDefinition property : this._properties) {
            AnnotatedMember am = property.getMutator();
            if (am != null) {
                ReferenceProperty refDef = this._annotationIntrospector.findReferenceType(am);
                if (refDef != null && refDef.isBackReference()) {
                    if (result == null) {
                        result = new HashMap();
                    }
                    String refName = refDef.getName();
                    if (result.put(refName, am) != null) {
                        throw new IllegalArgumentException("Multiple back-reference properties with name '" + refName + "'");
                    }
                }
            }
        }
        return result;
    }

    public List<AnnotatedMethod> getFactoryMethods() {
        List<AnnotatedMethod> candidates = this._classInfo.getStaticMethods();
        if (candidates.isEmpty()) {
            return candidates;
        }
        ArrayList<AnnotatedMethod> result = new ArrayList();
        for (AnnotatedMethod am : candidates) {
            if (isFactoryMethod(am)) {
                result.add(am);
            }
        }
        return result;
    }

    public Constructor<?> findSingleArgConstructor(Class<?>... argTypes) {
        for (AnnotatedConstructor ac : this._classInfo.getConstructors()) {
            if (ac.getParameterCount() == 1) {
                Class<?> actArg = ac.getRawParameterType(0);
                for (Class<?> expArg : argTypes) {
                    if (expArg == actArg) {
                        return ac.getAnnotated();
                    }
                }
                continue;
            }
        }
        return null;
    }

    public Method findFactoryMethod(Class<?>... expArgTypes) {
        for (AnnotatedMethod am : this._classInfo.getStaticMethods()) {
            if (isFactoryMethod(am)) {
                Class<?> actualArgType = am.getRawParameterType(0);
                for (Class<?> expArgType : expArgTypes) {
                    if (actualArgType.isAssignableFrom(expArgType)) {
                        return am.getAnnotated();
                    }
                }
                continue;
            }
        }
        return null;
    }

    protected boolean isFactoryMethod(AnnotatedMethod am) {
        if (!getBeanClass().isAssignableFrom(am.getRawReturnType())) {
            return false;
        }
        if (this._annotationIntrospector.hasCreatorAnnotation(am)) {
            return true;
        }
        String name = am.getName();
        if ("valueOf".equals(name)) {
            return true;
        }
        if (!"fromString".equals(name) || 1 != am.getParameterCount()) {
            return false;
        }
        Class<?> cls = am.getRawParameterType(0);
        if (cls == String.class || CharSequence.class.isAssignableFrom(cls)) {
            return true;
        }
        return false;
    }

    @Deprecated
    public List<String> findCreatorPropertyNames() {
        List<PropertyName> params = findCreatorParameterNames();
        if (params.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList(params.size());
        for (PropertyName name : params) {
            result.add(name.getSimpleName());
        }
        return result;
    }

    public List<PropertyName> findCreatorParameterNames() {
        int i = 0;
        while (i < 2) {
            for (AnnotatedWithParams creator : i == 0 ? getConstructors() : getFactoryMethods()) {
                int argCount = creator.getParameterCount();
                if (argCount >= 1) {
                    PropertyName name = _findCreatorPropertyName(creator.getParameter(0));
                    if (!(name == null || name.isEmpty())) {
                        List<PropertyName> arrayList = new ArrayList();
                        arrayList.add(name);
                        for (int p = 1; p < argCount; p++) {
                            arrayList.add(_findCreatorPropertyName(creator.getParameter(p)));
                        }
                        return arrayList;
                    }
                }
            }
            i++;
        }
        return Collections.emptyList();
    }

    protected PropertyName _findCreatorPropertyName(AnnotatedParameter param) {
        PropertyName name = this._annotationIntrospector.findNameForDeserialization(param);
        if (name != null && !name.isEmpty()) {
            return name;
        }
        String str = this._annotationIntrospector.findImplicitPropertyName(param);
        if (str == null || str.isEmpty()) {
            return name;
        }
        return new PropertyName(str);
    }

    public Class<?> findPOJOBuilder() {
        return this._annotationIntrospector == null ? null : this._annotationIntrospector.findPOJOBuilder(this._classInfo);
    }

    public JsonPOJOBuilder.Value findPOJOBuilderConfig() {
        return this._annotationIntrospector == null ? null : this._annotationIntrospector.findPOJOBuilderConfig(this._classInfo);
    }

    public Converter<Object, Object> findDeserializationConverter() {
        if (this._annotationIntrospector == null) {
            return null;
        }
        return _createConverter(this._annotationIntrospector.findDeserializationConverter(this._classInfo));
    }

    public LinkedHashMap<String, AnnotatedField> _findPropertyFields(Collection<String> ignoredProperties, boolean forSerialization) {
        LinkedHashMap<String, AnnotatedField> results = new LinkedHashMap();
        for (BeanPropertyDefinition property : this._properties) {
            AnnotatedField f = property.getField();
            if (f != null) {
                String name = property.getName();
                if (ignoredProperties == null || !ignoredProperties.contains(name)) {
                    results.put(name, f);
                }
            }
        }
        return results;
    }

    public Converter<Object, Object> _createConverter(Object converterDef) {
        Converter<?, ?> conv = null;
        if (converterDef == null) {
            return null;
        }
        if (converterDef instanceof Converter) {
            return (Converter) converterDef;
        }
        if (converterDef instanceof Class) {
            Class<?> converterClass = (Class) converterDef;
            if (converterClass == None.class || ClassUtil.isBogusClass(converterClass)) {
                return null;
            }
            if (Converter.class.isAssignableFrom(converterClass)) {
                HandlerInstantiator hi = this._config.getHandlerInstantiator();
                if (hi != null) {
                    conv = hi.converterInstance(this._config, this._classInfo, converterClass);
                }
                if (conv == null) {
                    conv = (Converter) ClassUtil.createInstance(converterClass, this._config.canOverrideAccessModifiers());
                }
                return conv;
            }
            throw new IllegalStateException("AnnotationIntrospector returned Class " + converterClass.getName() + "; expected Class<Converter>");
        }
        throw new IllegalStateException("AnnotationIntrospector returned Converter definition of type " + converterDef.getClass().getName() + "; expected type Converter or Class<Converter> instead");
    }
}
