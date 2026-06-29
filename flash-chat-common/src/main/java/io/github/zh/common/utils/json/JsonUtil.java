package io.github.zh.common.utils.json;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.registerModule(new JavaTimeModule());// 解决 LocalDateTime 反序列化问题
    }

    /**
     * 将对象转换为json字符串
     *
     * @param obj
     * @return
     */
    @SneakyThrows
    public static String toJsonString(Object obj) {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    /**
     * 将对象转换为指定类型的的对象
     */
    public static <T> T toObject(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JsonUtil.toObject error:{}", e.getMessage(),e);
            return null;
        }
    }


    public static String toSafeLogString(Object obj) {
        return toSafeLogString(obj, 3, 100, 2000);
    }

    private static String toSafeLogString(Object obj, int maxDepth, int maxElements, int maxBodyChars) {
        StringBuilder sb = new StringBuilder(256);
        toSafeLogStringInternal(obj, sb, 0, maxDepth, maxElements, maxBodyChars);
        return sb.toString();
    }

    private static void toSafeLogStringInternal(Object obj, StringBuilder sb, int depth,
                                                int maxDepth, int maxElements, int maxBodyChars) {
        if (obj == null) {
            sb.append("null");
            return;
        }
        if (depth >= maxDepth) {
            sb.append("[MaxDepthReached:]").append(obj.getClass().getName()).append("]");
            return;
        }
        if (obj instanceof CharSequence) {
            String s = obj.toString();
            appendTruncated(sb, s, maxBodyChars, true);
            return;
        }
        if (obj instanceof Number || obj instanceof Boolean || obj instanceof Character || obj.getClass().isEnum()) {
            sb.append(String.valueOf(obj));
            return;
        }
        if (obj instanceof java.io.InputStream || obj instanceof java.io.OutputStream) {
            sb.append("[Stream omitted:]").append(obj.getClass().getName()).append("]");
            return;
        }
        if (obj instanceof java.io.Reader || obj instanceof java.io.Writer) {
            sb.append("[Reader/Writer omitted:]").append(obj.getClass().getName()).append("]");
            return;
        }
        if (obj instanceof byte[]) {
            byte[] bytes = (byte[]) obj;
            int show = Math.min(bytes.length, Math.max(0, maxBodyChars / 2));
            sb.append("[byte[").append(bytes.length).append("] preview=")
                    .append(java.util.Base64.getEncoder().encodeToString(java.util.Arrays.copyOf(bytes, show)));
            if (bytes.length > show) sb.append("...truncated");
            sb.append("]");
            return;
        }
        if (obj instanceof char[]) {
            char[] chars = (char[]) obj;
            int show = Math.min(chars.length, maxBodyChars);
            sb.append("[char[").append(chars.length).append("] preview=")
                    .append(new String(chars, 0, show));
            if (chars.length > show) sb.append("...truncated");
            sb.append("]");
            return;
        }
        String cn = obj.getClass().getName();
        if (startsWithAny(cn,
                "jakarta.servlet.", "javax.servlet", "org.apache.catalina", "org.springframework.web.context.redis")) {
            sb.append('[').append(simpleServletKindName(cn)).append("omitted:").append(cn).append("]");
            return;
        }
        if (obj instanceof java.util.Map) {
            java.util.Map<?, ?> map = (java.util.Map<?, ?>) obj;
            sb.append("[Map{");
            int i = 0;
            for (java.util.Map.Entry<?, ?> e : map.entrySet()) {
                if (i > 0) sb.append(',');
                if (i >= maxElements) {
                    sb.append("...").append(map.size() - i).append("more");
                    break;
                }
                toSafeLogStringInternal(e.getKey(), sb, depth + 1, maxDepth, maxElements, maxBodyChars);
                sb.append('=');
                toSafeLogStringInternal(e.getValue(), sb, depth + 1, maxDepth, maxElements, maxBodyChars);
                i++;
            }
            sb.append("}]");
            return;
        }
        if (obj instanceof java.util.Collection) {
            java.util.Collection<?> col = (java.util.Collection<?>) obj;
            sb.append('[');
            int i = 0;
            for (Object o : col) {

                if (i > 0) sb.append(',');
                if (i >= maxElements) {
                    sb.append("...").append(col.size() - i).append("more");
                    break;
                }
                toSafeLogStringInternal(o, sb, depth + 1, maxDepth, maxElements, maxBodyChars);
                i++;
            }
            sb.append(']');
            return;
        }
        if (obj.getClass().isArray()) {
            int len = java.lang.reflect.Array.getLength(obj);
            sb.append('[');
            int limit = Math.min(len, maxElements);
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append(',');
                Object o = java.lang.reflect.Array.get(obj, i);
                toSafeLogStringInternal(o, sb, depth + 1, maxDepth, maxElements, maxBodyChars);
            }
            if (len > limit) {
                sb.append("...").append(len - limit).append("more");
            }
            sb.append(']');
            return;
        }
        try {
            sb.append(toJsonString(obj));
        } catch (RuntimeException ex) {
            sb.append("[Unserializable:").append(cn).append("]");
        }
    }


    private static String simpleServletKindName(String className) {
        String lower = className.toLowerCase();
        if (lower.contains("request")) return "ServletRequest";
        if (lower.contains("response")) return "ServletResponse";
        if (lower.contains("session")) return "HttpSession";
        return "ServletObject";
    }

    private static boolean startsWithAny(String s, String... prefixes) {
        for (String p : prefixes) {
            if (s.startsWith(p)) return true;
        }
        return false;
    }

    private static void appendTruncated(StringBuilder sb, String s, int maxChars, boolean quote) {
        if (s == null) {
            sb.append("null");
            return;
        }
        if (quote) sb.append('"');
        int show = Math.min(s.length(), Math.max(0, maxChars));
        sb.append(s, 0, show);
        if (s.length() > show) sb.append("...truncated");
        if (quote) sb.append('"');
    }
}
