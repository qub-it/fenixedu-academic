package org.fenixedu.academic.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DomainObjectDeletionBean {
    private Map<String, String> attributes = new HashMap<>();

    public DomainObjectDeletionBean() {

    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(this.attributes);
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addAttribute(String key, String value) {
        this.attributes.put(key, value);
    }

    public String getAttributeValue(String key) {
        return this.attributes.get(key);
    }

}
