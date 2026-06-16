package com.everycent.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(name = "behavior_tag")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class BehaviorTag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @NotNull
    @Size(max = 50)
    @Column(name = "name", length = 50, nullable = false)
    private String name;

    @NotNull
    @Column(name = "system_default", nullable = false)
    private Boolean systemDefault = false;

    @ManyToOne
    private User creator;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return this.code;
    }

    public BehaviorTag code(String code) {
        this.setCode(code);
        return this;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return this.name;
    }

    public BehaviorTag name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getSystemDefault() {
        return this.systemDefault;
    }

    public BehaviorTag systemDefault(Boolean systemDefault) {
        this.setSystemDefault(systemDefault);
        return this;
    }

    public void setSystemDefault(Boolean systemDefault) {
        this.systemDefault = systemDefault;
    }

    public User getCreator() {
        return this.creator;
    }

    public BehaviorTag creator(User user) {
        this.setCreator(user);
        return this;
    }

    public void setCreator(User user) {
        this.creator = user;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BehaviorTag)) {
            return false;
        }
        return getId() != null && getId().equals(((BehaviorTag) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BehaviorTag{" +
            "id=" + getId() +
            ", code='" + getCode() + "'" +
            ", name='" + getName() + "'" +
            ", systemDefault='" + getSystemDefault() + "'" +
            "}";
    }
}
