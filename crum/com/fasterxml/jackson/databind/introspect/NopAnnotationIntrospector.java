package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.cfg.PackageVersion;
import java.io.Serializable;

public abstract class NopAnnotationIntrospector extends AnnotationIntrospector implements Serializable {
    public static final NopAnnotationIntrospector instance;
    private static final long serialVersionUID = 1;

    /* renamed from: com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector.1 */
    static class C14691 extends NopAnnotationIntrospector {
        private static final long serialVersionUID = 1;

        C14691() {
        }

        public Version version() {
            return PackageVersion.VERSION;
        }
    }

    static {
        instance = new C14691();
    }

    public Version version() {
        return Version.unknownVersion();
    }
}
