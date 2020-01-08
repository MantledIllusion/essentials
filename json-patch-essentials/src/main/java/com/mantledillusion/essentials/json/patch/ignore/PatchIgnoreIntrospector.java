package com.mantledillusion.essentials.json.patch.ignore;

import com.fasterxml.jackson.databind.introspect.AnnotatedField;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

import java.lang.reflect.Field;

/**
 * Extended {@link JacksonAnnotationIntrospector} that ignores {@link Field}s annotated with @{@link NoPatch}.
 */
public class PatchIgnoreIntrospector extends JacksonAnnotationIntrospector {

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return super.hasIgnoreMarker(m) || (m instanceof AnnotatedField && _findAnnotation(m, NoPatch.class) != null);
    }
}
