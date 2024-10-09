package org.fenixedu.academic.domain.dml;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.fenixedu.commons.i18n.LocalizedString;

import com.google.common.base.Strings;
import com.google.gson.JsonParser;

public class DynamicFieldValueConverter {

    private static Map<Class, Function<Object, String>> SERIALIZERS = new HashMap<>();

    private static Map<Class, Function<String, Object>> DESERIALIZERS = new HashMap<>();

    static {

        SERIALIZERS.put(LocalizedString.class, value -> value == null ? null : ((LocalizedString) value).json().toString());
        DESERIALIZERS.put(LocalizedString.class,
                (String value) -> Strings.isNullOrEmpty(value) ? null : LocalizedString.fromJson(new JsonParser().parse(value)));

        SERIALIZERS.put(String.class, value -> (String) value);
        DESERIALIZERS.put(String.class, (String value) -> Strings.isNullOrEmpty(value) ? null : value);

        SERIALIZERS.put(Boolean.class, value -> value == null ? null : value.toString());
        DESERIALIZERS.put(Boolean.class, (String value) -> Strings.isNullOrEmpty(value) ? null : Boolean.valueOf(value));

        SERIALIZERS.put(Long.class, value -> value == null ? null : value.toString());
        DESERIALIZERS.put(Long.class, (String value) -> Strings.isNullOrEmpty(value) ? null : Long.valueOf(value));

        SERIALIZERS.put(Integer.class, value -> value == null ? null : value.toString());
        DESERIALIZERS.put(Integer.class, (String value) -> Strings.isNullOrEmpty(value) ? null : Integer.valueOf(value));

        SERIALIZERS.put(BigDecimal.class, value -> value == null ? null : ((BigDecimal) value).toString());
        DESERIALIZERS.put(BigDecimal.class, (String value) -> Strings.isNullOrEmpty(value) ? null : new BigDecimal(value));

    }

    public static Set<Class> getSupportedTypes() {
        return SERIALIZERS.keySet();
    }

    public static boolean isSupported(final Class input) {
        return getSupportedTypes().contains(input);
    }

    public static String serialize(Class type, Object value) {
        return SERIALIZERS.get(type).apply(value);
    }

    public static <T> Object deserialize(Class<T> type, String value) {
        return DESERIALIZERS.get(type).apply(value);
    }

}
