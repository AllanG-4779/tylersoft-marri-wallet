package net.tylersoft.common.logging;

import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;

import java.util.Set;

/**
 * Jackson {@link NopAnnotationIntrospector} that applies masking in two ways:
 * <ol>
 *   <li>Fields annotated with {@link Sensitive} use the declared strategy.</li>
 *   <li>Fields whose lowercased name appears in {@code alwaysSensitiveNames}
 *       are fully masked even without the annotation.</li>
 * </ol>
 */
class SensitiveAnnotationIntrospector extends NopAnnotationIntrospector {

    private final Set<String> alwaysSensitiveNames;

    SensitiveAnnotationIntrospector(Set<String> alwaysSensitiveNames) {
        this.alwaysSensitiveNames = alwaysSensitiveNames;
    }

    @Override
    public Object findSerializer(Annotated am) {
        Sensitive annotation = am.getAnnotation(Sensitive.class);
        if (annotation != null) {
            return new SensitiveJsonSerializer(annotation.strategy(), annotation.visibleChars());
        }
        String name = am.getName();
        if (name != null && alwaysSensitiveNames.contains(name.toLowerCase())) {
            return new SensitiveJsonSerializer(MaskingStrategy.FULL, 0);
        }
        return null;
    }
}
