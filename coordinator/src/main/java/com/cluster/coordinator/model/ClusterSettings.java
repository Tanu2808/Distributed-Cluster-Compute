package com.cluster.coordinator.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cluster_settings")
public class ClusterSettings {

    @Id
    @Column(name = "setting_key", nullable = false)
    private String key;

    @Column(name = "setting_value", nullable = false)
    private String value;

    public ClusterSettings() {}

    public ClusterSettings(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
