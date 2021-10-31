package org.opennms.plugins.zabbix.model;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;

public class Item {
    private String name;
    private String key;
    private List<PreprocessingRule> preprocessing = new LinkedList<>();
    private List<Tag> tags = new LinkedList<>();
    @JsonBackReference
    public Template template;
    @JsonManagedReference("item")
    private List<Trigger> triggers = new LinkedList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<PreprocessingRule> getPreprocessing() {
        return preprocessing;
    }

    public void setPreprocessing(List<PreprocessingRule> preprocessing) {
        this.preprocessing = preprocessing;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    public List<Trigger> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    public Template getTemplate() {
        return template;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }
}
