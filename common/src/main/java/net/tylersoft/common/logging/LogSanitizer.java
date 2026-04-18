package net.tylersoft.common.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Utility for producing safe log-friendly representations of objects.
 *
 * <ul>
 *   <li>{@link #toJson(Object)} — serializes to JSON; fields annotated with
 *       {@link Sensitive} or carrying a known-sensitive name are automatically masked.</li>
 *   <li>{@link #toPojo(Object)} — reflection-based key=value string; same masking
 *       rules apply without going through Jackson.</li>
 * </ul>
 *
 * The internal {@link ObjectMapper} is a <em>copy</em> of the application mapper,
 * so API response serialization is never affected.
 */
@Component
public class LogSanitizer {

    private static final SensitiveFieldMasker MASKER = new SensitiveFieldMasker();

    private final ObjectMapper maskedMapper;

    public LogSanitizer(ObjectMapper objectMapper, SensitiveJacksonModule module) {
        this.maskedMapper = objectMapper.copy().registerModule(module);
    }

    public String toJson(Object obj) {
        if (obj == null) return "null";
        try {
            return maskedMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return toPojo(obj);
        }
    }

    public String toPojo(Object obj) {
        if (obj == null) return "null";
        List<Field> fields = getAllFields(obj.getClass());
        StringJoiner joiner = new StringJoiner(", ", obj.getClass().getSimpleName() + "{", "}");
        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                Sensitive sensitive = field.getAnnotation(Sensitive.class);
                String display = (sensitive != null && value != null)
                        ? MASKER.mask(value.toString(), sensitive.strategy(), sensitive.visibleChars())
                        : String.valueOf(value);
                joiner.add(field.getName() + "=" + display);
            } catch (Exception ignored) {
                joiner.add(field.getName() + "=?");
            }
        }
        return joiner.toString();
    }

    private List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(List.of(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
