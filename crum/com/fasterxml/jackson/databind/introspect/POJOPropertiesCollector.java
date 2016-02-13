package com.fasterxml.jackson.databind.introspect;

import com.crumby.impl.crumby.UnsupportedUrlFragment;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.util.BeanUtil;
import com.fasterxml.jackson.databind.util.ClassUtil;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class POJOPropertiesCollector {
    protected final AnnotationIntrospector _annotationIntrospector;
    protected LinkedList<AnnotatedMember> _anyGetters;
    protected LinkedList<AnnotatedMethod> _anySetters;
    protected final AnnotatedClass _classDef;
    protected final MapperConfig<?> _config;
    protected LinkedList<POJOPropertyBuilder> _creatorProperties;
    protected final boolean _forSerialization;
    protected HashSet<String> _ignoredPropertyNames;
    protected LinkedHashMap<Object, AnnotatedMember> _injectables;
    protected LinkedList<AnnotatedMethod> _jsonValueGetters;
    protected final String _mutatorPrefix;
    protected final LinkedHashMap<String, POJOPropertyBuilder> _properties;
    protected final JavaType _type;
    protected final VisibilityChecker<?> _visibilityChecker;

    protected POJOPropertiesCollector(MapperConfig<?> config, boolean forSerialization, JavaType type, AnnotatedClass classDef, String mutatorPrefix) {
        AnnotationIntrospector annotationIntrospector = null;
        this._properties = new LinkedHashMap();
        this._creatorProperties = null;
        this._anyGetters = null;
        this._anySetters = null;
        this._jsonValueGetters = null;
        this._config = config;
        this._forSerialization = forSerialization;
        this._type = type;
        this._classDef = classDef;
        if (mutatorPrefix == null) {
            mutatorPrefix = "set";
        }
        this._mutatorPrefix = mutatorPrefix;
        if (config.isAnnotationProcessingEnabled()) {
            annotationIntrospector = this._config.getAnnotationIntrospector();
        }
        this._annotationIntrospector = annotationIntrospector;
        if (this._annotationIntrospector == null) {
            this._visibilityChecker = this._config.getDefaultVisibilityChecker();
        } else {
            this._visibilityChecker = this._annotationIntrospector.findAutoDetectVisibility(classDef, this._config.getDefaultVisibilityChecker());
        }
    }

    public MapperConfig<?> getConfig() {
        return this._config;
    }

    public JavaType getType() {
        return this._type;
    }

    public AnnotatedClass getClassDef() {
        return this._classDef;
    }

    public AnnotationIntrospector getAnnotationIntrospector() {
        return this._annotationIntrospector;
    }

    public List<BeanPropertyDefinition> getProperties() {
        return new ArrayList(this._properties.values());
    }

    public Map<Object, AnnotatedMember> getInjectables() {
        return this._injectables;
    }

    public AnnotatedMethod getJsonValueMethod() {
        if (this._jsonValueGetters == null) {
            return null;
        }
        if (this._jsonValueGetters.size() > 1) {
            reportProblem("Multiple value properties defined (" + this._jsonValueGetters.get(0) + " vs " + this._jsonValueGetters.get(1) + ")");
        }
        return (AnnotatedMethod) this._jsonValueGetters.get(0);
    }

    public AnnotatedMember getAnyGetter() {
        if (this._anyGetters == null) {
            return null;
        }
        if (this._anyGetters.size() > 1) {
            reportProblem("Multiple 'any-getters' defined (" + this._anyGetters.get(0) + " vs " + this._anyGetters.get(1) + ")");
        }
        return (AnnotatedMember) this._anyGetters.getFirst();
    }

    public AnnotatedMethod getAnySetterMethod() {
        if (this._anySetters == null) {
            return null;
        }
        if (this._anySetters.size() > 1) {
            reportProblem("Multiple 'any-setters' defined (" + this._anySetters.get(0) + " vs " + this._anySetters.get(1) + ")");
        }
        return (AnnotatedMethod) this._anySetters.getFirst();
    }

    public Set<String> getIgnoredPropertyNames() {
        return this._ignoredPropertyNames;
    }

    public ObjectIdInfo getObjectIdInfo() {
        if (this._annotationIntrospector == null) {
            return null;
        }
        ObjectIdInfo info = this._annotationIntrospector.findObjectIdInfo(this._classDef);
        if (info != null) {
            return this._annotationIntrospector.findObjectReferenceInfo(this._classDef, info);
        }
        return info;
    }

    public Class<?> findPOJOBuilderClass() {
        return this._annotationIntrospector.findPOJOBuilder(this._classDef);
    }

    protected Map<String, POJOPropertyBuilder> getPropertyMap() {
        return this._properties;
    }

    public POJOPropertiesCollector collect() {
        this._properties.clear();
        _addFields();
        _addMethods();
        _addCreators();
        _addInjectables();
        _removeUnwantedProperties();
        _renameProperties();
        PropertyNamingStrategy naming = _findNamingStrategy();
        if (naming != null) {
            _renameUsing(naming);
        }
        for (POJOPropertyBuilder property : this._properties.values()) {
            property.trimByVisibility();
        }
        for (POJOPropertyBuilder property2 : this._properties.values()) {
            property2.mergeAnnotations(this._forSerialization);
        }
        if (this._config.isEnabled(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)) {
            _renameWithWrappers();
        }
        _sortProperties();
        return this;
    }

    protected void _sortProperties() {
        Boolean alpha;
        boolean sort;
        String[] propertyOrder;
        AnnotationIntrospector intr = this._annotationIntrospector;
        if (intr == null) {
            alpha = null;
        } else {
            alpha = intr.findSerializationSortAlphabetically(this._classDef);
        }
        if (alpha == null) {
            sort = this._config.shouldSortPropertiesAlphabetically();
        } else {
            sort = alpha.booleanValue();
        }
        if (intr == null) {
            propertyOrder = null;
        } else {
            propertyOrder = intr.findSerializationPropertyOrder(this._classDef);
        }
        if (sort || this._creatorProperties != null || propertyOrder != null) {
            Map<String, POJOPropertyBuilder> all;
            Iterator i$;
            POJOPropertyBuilder prop;
            int size = this._properties.size();
            if (sort) {
                all = new TreeMap();
            } else {
                all = new LinkedHashMap(size + size);
            }
            for (POJOPropertyBuilder prop2 : this._properties.values()) {
                all.put(prop2.getName(), prop2);
            }
            Map<String, POJOPropertyBuilder> ordered = new LinkedHashMap(size + size);
            if (propertyOrder != null) {
                for (String name : propertyOrder) {
                    String name2;
                    POJOPropertyBuilder w = (POJOPropertyBuilder) all.get(name2);
                    if (w == null) {
                        for (POJOPropertyBuilder prop22 : this._properties.values()) {
                            if (name2.equals(prop22.getInternalName())) {
                                w = prop22;
                                name2 = prop22.getName();
                                break;
                            }
                        }
                    }
                    if (w != null) {
                        ordered.put(name2, w);
                    }
                }
            }
            if (this._creatorProperties != null) {
                Collection<POJOPropertyBuilder> cr;
                if (sort) {
                    TreeMap<String, POJOPropertyBuilder> sorted = new TreeMap();
                    i$ = this._creatorProperties.iterator();
                    while (i$.hasNext()) {
                        prop22 = (POJOPropertyBuilder) i$.next();
                        sorted.put(prop22.getName(), prop22);
                    }
                    cr = sorted.values();
                } else {
                    cr = this._creatorProperties;
                }
                for (POJOPropertyBuilder prop222 : cr) {
                    ordered.put(prop222.getName(), prop222);
                }
            }
            ordered.putAll(all);
            this._properties.clear();
            this._properties.putAll(ordered);
        }
    }

    protected void _addFields() {
        boolean pruneFinalFields;
        AnnotationIntrospector ai = this._annotationIntrospector;
        if (this._forSerialization || this._config.isEnabled(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS)) {
            pruneFinalFields = false;
        } else {
            pruneFinalFields = true;
        }
        for (AnnotatedField f : this._classDef.fields()) {
            PropertyName pn;
            boolean nameExplicit;
            boolean visible;
            boolean ignored;
            String implName = ai == null ? null : ai.findImplicitPropertyName(f);
            if (implName == null) {
                implName = f.getName();
            }
            if (ai == null) {
                pn = null;
            } else if (this._forSerialization) {
                pn = ai.findNameForSerialization(f);
            } else {
                pn = ai.findNameForDeserialization(f);
            }
            if (pn != null) {
                nameExplicit = true;
            } else {
                nameExplicit = false;
            }
            if (nameExplicit && pn.isEmpty()) {
                pn = _propNameFromSimple(implName);
                nameExplicit = false;
            }
            if (pn != null) {
                visible = true;
            } else {
                visible = false;
            }
            if (!visible) {
                visible = this._visibilityChecker.isFieldVisible(f);
            }
            if (ai == null || !ai.hasIgnoreMarker(f)) {
                ignored = false;
            } else {
                ignored = true;
            }
            if (!pruneFinalFields || pn != null || ignored || !Modifier.isFinal(f.getModifiers())) {
                _property(implName).addField(f, pn, nameExplicit, visible, ignored);
            }
        }
    }

    protected void _addCreators() {
        if (this._annotationIntrospector != null) {
            int len;
            int i;
            for (AnnotatedConstructor ctor : this._classDef.getConstructors()) {
                if (this._creatorProperties == null) {
                    this._creatorProperties = new LinkedList();
                }
                len = ctor.getParameterCount();
                for (i = 0; i < len; i++) {
                    _addCreatorParam(ctor.getParameter(i));
                }
            }
            for (AnnotatedMethod factory : this._classDef.getStaticMethods()) {
                if (this._creatorProperties == null) {
                    this._creatorProperties = new LinkedList();
                }
                len = factory.getParameterCount();
                for (i = 0; i < len; i++) {
                    _addCreatorParam(factory.getParameter(i));
                }
            }
        }
    }

    protected void _addCreatorParam(AnnotatedParameter param) {
        boolean expl;
        String impl = this._annotationIntrospector.findImplicitPropertyName(param);
        if (impl == null) {
            impl = UnsupportedUrlFragment.DISPLAY_NAME;
        }
        PropertyName pn = this._annotationIntrospector.findNameForDeserialization(param);
        if (pn == null || pn.isEmpty()) {
            expl = false;
        } else {
            expl = true;
        }
        if (!expl) {
            if (!impl.isEmpty()) {
                pn = new PropertyName(impl);
            } else {
                return;
            }
        }
        POJOPropertyBuilder prop = expl ? _property(pn) : _property(impl);
        prop.addCtor(param, pn, expl, true, false);
        this._creatorProperties.add(prop);
    }

    protected void _addMethods() {
        AnnotationIntrospector ai = this._annotationIntrospector;
        for (AnnotatedMethod m : this._classDef.memberMethods()) {
            int argCount = m.getParameterCount();
            if (argCount == 0) {
                _addGetterMethod(m, ai);
            } else if (argCount == 1) {
                _addSetterMethod(m, ai);
            } else if (argCount == 2 && ai != null && ai.hasAnySetterAnnotation(m)) {
                if (this._anySetters == null) {
                    this._anySetters = new LinkedList();
                }
                this._anySetters.add(m);
            }
        }
    }

    protected void _addGetterMethod(AnnotatedMethod m, AnnotationIntrospector ai) {
        boolean ignore = false;
        String implName = null;
        if (m.hasReturnType()) {
            boolean nameExplicit;
            boolean visible;
            if (ai != null) {
                if (ai.hasAnyGetterAnnotation(m)) {
                    if (this._anyGetters == null) {
                        this._anyGetters = new LinkedList();
                    }
                    this._anyGetters.add(m);
                    return;
                } else if (ai.hasAsValueAnnotation(m)) {
                    if (this._jsonValueGetters == null) {
                        this._jsonValueGetters = new LinkedList();
                    }
                    this._jsonValueGetters.add(m);
                    return;
                }
            }
            PropertyName pn = ai == null ? null : ai.findNameForSerialization(m);
            if (pn != null) {
                nameExplicit = true;
            } else {
                nameExplicit = false;
            }
            if (nameExplicit) {
                if (ai != null) {
                    implName = ai.findImplicitPropertyName(m);
                }
                if (implName == null) {
                    implName = BeanUtil.okNameForGetter(m);
                }
                if (implName == null) {
                    implName = m.getName();
                }
                if (pn.isEmpty()) {
                    pn = _propNameFromSimple(implName);
                    nameExplicit = false;
                }
                visible = true;
            } else {
                if (ai != null) {
                    implName = ai.findImplicitPropertyName(m);
                }
                if (implName == null) {
                    implName = BeanUtil.okNameForRegularGetter(m, m.getName());
                }
                if (implName == null) {
                    implName = BeanUtil.okNameForIsGetter(m, m.getName());
                    if (implName != null) {
                        visible = this._visibilityChecker.isIsGetterVisible(m);
                    } else {
                        return;
                    }
                }
                visible = this._visibilityChecker.isGetterVisible(m);
            }
            if (ai != null) {
                ignore = ai.hasIgnoreMarker(m);
            }
            _property(implName).addGetter(m, pn, nameExplicit, visible, ignore);
        }
    }

    protected void _addSetterMethod(AnnotatedMethod m, AnnotationIntrospector ai) {
        boolean nameExplicit;
        boolean visible;
        boolean ignore = false;
        String implName = null;
        PropertyName pn = ai == null ? null : ai.findNameForDeserialization(m);
        if (pn != null) {
            nameExplicit = true;
        } else {
            nameExplicit = false;
        }
        if (nameExplicit) {
            if (ai != null) {
                implName = ai.findImplicitPropertyName(m);
            }
            if (implName == null) {
                implName = BeanUtil.okNameForMutator(m, this._mutatorPrefix);
            }
            if (implName == null) {
                implName = m.getName();
            }
            if (pn.isEmpty()) {
                pn = _propNameFromSimple(implName);
                nameExplicit = false;
            }
            visible = true;
        } else {
            if (ai != null) {
                implName = ai.findImplicitPropertyName(m);
            }
            if (implName == null) {
                implName = BeanUtil.okNameForMutator(m, this._mutatorPrefix);
            }
            if (implName != null) {
                visible = this._visibilityChecker.isSetterVisible(m);
            } else {
                return;
            }
        }
        if (ai != null) {
            ignore = ai.hasIgnoreMarker(m);
        }
        _property(implName).addSetter(m, pn, nameExplicit, visible, ignore);
    }

    protected void _addInjectables() {
        AnnotationIntrospector ai = this._annotationIntrospector;
        if (ai != null) {
            for (AnnotatedField f : this._classDef.fields()) {
                _doAddInjectable(ai.findInjectableValueId(f), f);
            }
            for (AnnotatedMethod m : this._classDef.memberMethods()) {
                if (m.getParameterCount() == 1) {
                    _doAddInjectable(ai.findInjectableValueId(m), m);
                }
            }
        }
    }

    protected void _doAddInjectable(Object id, AnnotatedMember m) {
        if (id != null) {
            if (this._injectables == null) {
                this._injectables = new LinkedHashMap();
            }
            if (((AnnotatedMember) this._injectables.put(id, m)) != null) {
                throw new IllegalArgumentException("Duplicate injectable value with id '" + String.valueOf(id) + "' (of type " + id.getClass().getName() + ")");
            }
        }
    }

    private PropertyName _propNameFromSimple(String simpleName) {
        return PropertyName.construct(simpleName, null);
    }

    protected void _removeUnwantedProperties() {
        Iterator<Entry<String, POJOPropertyBuilder>> it = this._properties.entrySet().iterator();
        boolean forceNonVisibleRemoval = !this._config.isEnabled(MapperFeature.INFER_PROPERTY_MUTATORS);
        while (it.hasNext()) {
            POJOPropertyBuilder prop = (POJOPropertyBuilder) ((Entry) it.next()).getValue();
            if (prop.anyVisible()) {
                if (prop.anyIgnorals()) {
                    if (prop.isExplicitlyIncluded()) {
                        prop.removeIgnored();
                        if (!(this._forSerialization || prop.couldDeserialize())) {
                            _addIgnored(prop.getName());
                        }
                    } else {
                        it.remove();
                        _addIgnored(prop.getName());
                    }
                }
                prop.removeNonVisible(forceNonVisibleRemoval);
            } else {
                it.remove();
            }
        }
    }

    private void _addIgnored(String name) {
        if (!this._forSerialization) {
            if (this._ignoredPropertyNames == null) {
                this._ignoredPropertyNames = new HashSet();
            }
            this._ignoredPropertyNames.add(name);
        }
    }

    protected void _renameProperties() {
        Iterator<Entry<String, POJOPropertyBuilder>> it = this._properties.entrySet().iterator();
        LinkedList<POJOPropertyBuilder> renamed = null;
        while (it.hasNext()) {
            POJOPropertyBuilder prop = (POJOPropertyBuilder) ((Entry) it.next()).getValue();
            Collection<PropertyName> l = prop.findExplicitNames();
            if (!l.isEmpty()) {
                it.remove();
                if (renamed == null) {
                    renamed = new LinkedList();
                }
                if (l.size() == 1) {
                    renamed.add(prop.withName((PropertyName) l.iterator().next()));
                } else {
                    renamed.addAll(prop.explode(l));
                }
            }
        }
        if (renamed != null) {
            Iterator i$ = renamed.iterator();
            while (i$.hasNext()) {
                prop = (POJOPropertyBuilder) i$.next();
                String name = prop.getName();
                POJOPropertyBuilder old = (POJOPropertyBuilder) this._properties.get(name);
                if (old == null) {
                    this._properties.put(name, prop);
                } else {
                    old.addAll(prop);
                }
                _updateCreatorProperty(prop, this._creatorProperties);
            }
        }
    }

    protected void _renameUsing(PropertyNamingStrategy naming) {
        POJOPropertyBuilder[] props = (POJOPropertyBuilder[]) this._properties.values().toArray(new POJOPropertyBuilder[this._properties.size()]);
        this._properties.clear();
        for (POJOPropertyBuilder prop : props) {
            POJOPropertyBuilder prop2;
            String simpleName;
            PropertyName fullName = prop2.getFullName();
            String rename = null;
            if (!prop2.isExplicitlyNamed()) {
                if (this._forSerialization) {
                    if (prop2.hasGetter()) {
                        rename = naming.nameForGetterMethod(this._config, prop2.getGetter(), fullName.getSimpleName());
                    } else if (prop2.hasField()) {
                        rename = naming.nameForField(this._config, prop2.getField(), fullName.getSimpleName());
                    }
                } else if (prop2.hasSetter()) {
                    rename = naming.nameForSetterMethod(this._config, prop2.getSetter(), fullName.getSimpleName());
                } else if (prop2.hasConstructorParameter()) {
                    rename = naming.nameForConstructorParameter(this._config, prop2.getConstructorParameter(), fullName.getSimpleName());
                } else if (prop2.hasField()) {
                    rename = naming.nameForField(this._config, prop2.getField(), fullName.getSimpleName());
                } else if (prop2.hasGetter()) {
                    rename = naming.nameForGetterMethod(this._config, prop2.getGetter(), fullName.getSimpleName());
                }
            }
            if (rename == null || fullName.hasSimpleName(rename)) {
                simpleName = fullName.getSimpleName();
            } else {
                prop2 = prop2.withSimpleName(rename);
                simpleName = rename;
            }
            POJOPropertyBuilder old = (POJOPropertyBuilder) this._properties.get(simpleName);
            if (old == null) {
                this._properties.put(simpleName, prop2);
            } else {
                old.addAll(prop2);
            }
            _updateCreatorProperty(prop2, this._creatorProperties);
        }
    }

    protected void _renameWithWrappers() {
        Iterator<Entry<String, POJOPropertyBuilder>> it = this._properties.entrySet().iterator();
        LinkedList<POJOPropertyBuilder> renamed = null;
        while (it.hasNext()) {
            POJOPropertyBuilder prop = (POJOPropertyBuilder) ((Entry) it.next()).getValue();
            AnnotatedMember member = prop.getPrimaryMember();
            if (member != null) {
                PropertyName wrapperName = this._annotationIntrospector.findWrapperName(member);
                if (!(wrapperName == null || !wrapperName.hasSimpleName() || wrapperName.equals(prop.getFullName()))) {
                    if (renamed == null) {
                        renamed = new LinkedList();
                    }
                    renamed.add(prop.withName(wrapperName));
                    it.remove();
                }
            }
        }
        if (renamed != null) {
            Iterator i$ = renamed.iterator();
            while (i$.hasNext()) {
                prop = (POJOPropertyBuilder) i$.next();
                String name = prop.getName();
                POJOPropertyBuilder old = (POJOPropertyBuilder) this._properties.get(name);
                if (old == null) {
                    this._properties.put(name, prop);
                } else {
                    old.addAll(prop);
                }
            }
        }
    }

    protected void reportProblem(String msg) {
        throw new IllegalArgumentException("Problem with definition of " + this._classDef + ": " + msg);
    }

    protected POJOPropertyBuilder _property(PropertyName name) {
        return _property(name.getSimpleName());
    }

    protected POJOPropertyBuilder _property(String implName) {
        POJOPropertyBuilder prop = (POJOPropertyBuilder) this._properties.get(implName);
        if (prop != null) {
            return prop;
        }
        prop = new POJOPropertyBuilder(new PropertyName(implName), this._annotationIntrospector, this._forSerialization);
        this._properties.put(implName, prop);
        return prop;
    }

    private PropertyNamingStrategy _findNamingStrategy() {
        Class<?> namingDef = this._annotationIntrospector == null ? null : this._annotationIntrospector.findNamingStrategy(this._classDef);
        if (namingDef == null) {
            return this._config.getPropertyNamingStrategy();
        }
        if (namingDef instanceof PropertyNamingStrategy) {
            return (PropertyNamingStrategy) namingDef;
        }
        if (namingDef instanceof Class) {
            Class<?> namingClass = namingDef;
            if (PropertyNamingStrategy.class.isAssignableFrom(namingClass)) {
                HandlerInstantiator hi = this._config.getHandlerInstantiator();
                if (hi != null) {
                    PropertyNamingStrategy pns = hi.namingStrategyInstance(this._config, this._classDef, namingClass);
                    if (pns != null) {
                        return pns;
                    }
                }
                return (PropertyNamingStrategy) ClassUtil.createInstance(namingClass, this._config.canOverrideAccessModifiers());
            }
            throw new IllegalStateException("AnnotationIntrospector returned Class " + namingClass.getName() + "; expected Class<PropertyNamingStrategy>");
        }
        throw new IllegalStateException("AnnotationIntrospector returned PropertyNamingStrategy definition of type " + namingDef.getClass().getName() + "; expected type PropertyNamingStrategy or Class<PropertyNamingStrategy> instead");
    }

    protected void _updateCreatorProperty(POJOPropertyBuilder prop, List<POJOPropertyBuilder> creatorProperties) {
        if (creatorProperties != null) {
            int len = creatorProperties.size();
            for (int i = 0; i < len; i++) {
                if (((POJOPropertyBuilder) creatorProperties.get(i)).getInternalName().equals(prop.getInternalName())) {
                    creatorProperties.set(i, prop);
                    return;
                }
            }
        }
    }
}
