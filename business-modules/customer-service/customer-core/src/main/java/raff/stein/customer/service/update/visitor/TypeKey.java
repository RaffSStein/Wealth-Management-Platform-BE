package raff.stein.customer.service.update.visitor;

import lombok.AllArgsConstructor;

import java.util.Objects;

/**
 * Represents a key for a type, consisting of a raw type and a parameter type.
 * This is useful for generic type handling, especially in scenarios where
 * the raw type and its parameterized type need to be distinguished.
 */
@AllArgsConstructor
public class TypeKey {

    private final Class<?> rawType;
    private final Class<?> keyOrParameterType;
    private final Class<?> valueType;


    public static TypeKey of(Class<?> rawType, Class<?> keyOrParameterType, Class<?> valueType) {
        return new TypeKey(rawType, keyOrParameterType, valueType);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TypeKey that)) return false;
        return Objects.equals(rawType, that.rawType) &&
                Objects.equals(keyOrParameterType, that.keyOrParameterType) &&
                Objects.equals(valueType, that.valueType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawType, keyOrParameterType, valueType);
    }

    @Override
    public String toString() {
        String valueTypeName = valueType != null ? valueType.getSimpleName() : "";
        String keyOrParameterTypeName = keyOrParameterType != null ? keyOrParameterType.getSimpleName() : "";
        String rawTypeName = rawType != null ? rawType.getSimpleName() : "";
        return rawTypeName + "<"  +
                "<" + keyOrParameterTypeName + ">" + "," +
                "<" + valueTypeName + ">";
    }
}
