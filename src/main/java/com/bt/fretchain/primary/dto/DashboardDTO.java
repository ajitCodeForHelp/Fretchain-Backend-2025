package com.bt.fretchain.primary.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-driven dashboard: the server decides which sections / widgets a login may see
 * (by permission) and sends the data; the web panel just renders them.
 *
 * Widget types and their data:
 *   STAT       value, format (NUMBER | MONEY | PERCENT), hint?, tone?, link?, icon
 *   TREND      points [{label, value}], format
 *   BREAKDOWN  items [{key, value}], format
 *   LIST       rows [{title, subtitle, badge, tone, value, link}]
 *   ALERT      count, text, tone, link
 *   CHECKLIST  percent, items [{key, done, link}]
 * "title" and keys are translation keys (the panel translates them).
 */
public class DashboardDTO {

    @Getter
    @Setter
    public static class Response {
        private String scope;
        private int days;
        private java.time.LocalDateTime generatedAt = java.time.LocalDateTime.now();
        private List<Section> sections = new ArrayList<>();

        public Section section(String key) {
            Section s = new Section();
            s.setKey(key);
            sections.add(s);
            return s;
        }

        /** Sections without widgets are not sent. */
        public Response prune() {
            sections.removeIf(s -> s.getWidgets().isEmpty());
            return this;
        }
    }

    @Getter
    @Setter
    public static class Section {
        private String key;
        private List<Widget> widgets = new ArrayList<>();

        public Section add(Widget w) {
            if (w != null) {
                widgets.add(w);
            }
            return this;
        }
    }

    @Getter
    @Setter
    public static class Widget {
        private String id;
        private String type;
        private String title;
        /** sm (1/4 row), md (1/2 row), lg (full row) */
        private String size = "sm";
        private Map<String, Object> data = new LinkedHashMap<>();

        public Widget() {
        }

        public Widget(String id, String type, String title) {
            this.id = id;
            this.type = type;
            this.title = title;
        }

        public Widget put(String key, Object value) {
            data.put(key, value);
            return this;
        }

        public Widget size(String size) {
            this.size = size;
            return this;
        }
    }

    // ---------------------------------------------------------------- factories

    public static Widget stat(String id, Number value, String format, String icon, String tone, String link) {
        return new Widget(id, "STAT", id).put("value", value).put("format", format).put("icon", icon).put("tone", tone).put("link", link);
    }

    public static Widget trend(String id, List<Map<String, Object>> points, String format) {
        return new Widget(id, "TREND", id).size("md").put("points", points).put("format", format);
    }

    public static Widget breakdown(String id, List<Map<String, Object>> items, String format) {
        return new Widget(id, "BREAKDOWN", id).size("md").put("items", items).put("format", format);
    }

    public static Widget list(String id, List<Map<String, Object>> rows, String link) {
        return new Widget(id, "LIST", id).size("md").put("rows", rows).put("link", link);
    }

    public static Widget alert(String id, long count, String tone, String link) {
        return count <= 0 ? null : new Widget(id, "ALERT", id).size("md").put("count", count).put("tone", tone).put("link", link);
    }

    public static Widget checklist(String id, List<Map<String, Object>> items) {
        long done = items.stream().filter(i -> Boolean.TRUE.equals(i.get("done"))).count();
        return new Widget(id, "CHECKLIST", id).size("md")
                .put("percent", items.isEmpty() ? 100 : Math.round(done * 100.0 / items.size()))
                .put("items", items);
    }

    public static Map<String, Object> point(String label, Number value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("label", label);
        m.put("value", value);
        return m;
    }

    public static Map<String, Object> item(String key, Number value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("value", value);
        return m;
    }

    public static Map<String, Object> row(String title, String subtitle, String badge, String tone, Object value, String link) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("subtitle", subtitle);
        m.put("badge", badge);
        m.put("tone", tone);
        m.put("value", value);
        m.put("link", link);
        return m;
    }

    public static Map<String, Object> check(String key, boolean done, String link) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", key);
        m.put("done", done);
        m.put("link", link);
        return m;
    }
}
