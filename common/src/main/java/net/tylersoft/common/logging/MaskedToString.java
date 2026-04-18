package net.tylersoft.common.logging;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Static utility for producing masked {@code toString()} output from any POJO.
 * Fields annotated with {@link Sensitive} are masked according to their strategy.
 * Fields not annotated are printed as-is.
 *
 * <p>Intended use — override {@code toString()} in any class or record:
 * <pre>{@code
 * @Override
 * public String toString() {
 *     return MaskedToString.of(this);
 * }
 * }</pre>
 *
 * This makes {@code log.info("Request: {}", request)} safe with zero extra effort.
 */
public final class MaskedToString {

    private static final SensitiveFieldMasker MASKER = new SensitiveFieldMasker();

    private MaskedToString() {}

    public static String of(Object obj) {
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

    private static List<Field> getAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && clazz != Object.class) {
            fields.addAll(List.of(clazz.getDeclaredFields()));
            clazz = clazz.getSuperclass();
        }
        return fields;
    }
}
