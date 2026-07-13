package tong.statmod.dungeon.template;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TemplateProperty {
    public static final TemplateProperty EMPTY = new TemplateProperty(Collections.emptyMap());
    private final Map<String, String> values;

    public TemplateProperty(Map<String, String> values) {
        this.values = new HashMap<>(values);
    }

    public String get(String key, String defaultValue) {
        return values.getOrDefault(key, defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(values.getOrDefault(key, String.valueOf(defaultValue))); }
        catch (NumberFormatException e) { return defaultValue; }
    }

    public static TemplateProperty merge(TemplateProperty parent, TemplateProperty child) {
        Map<String, String> merged = new HashMap<>(parent.values);
        merged.putAll(child.values);
        return new TemplateProperty(merged);
    }
}
