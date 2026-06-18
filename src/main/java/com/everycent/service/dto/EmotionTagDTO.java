package com.everycent.service.dto;

import com.everycent.domain.enumeration.EmotionValence;

public class EmotionTagDTO {

    private Long id;

    private String code;

    private String name;

    private EmotionValence valence;

    private Boolean systemDefault;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EmotionValence getValence() {
        return valence;
    }

    public void setValence(EmotionValence valence) {
        this.valence = valence;
    }

    public Boolean getSystemDefault() {
        return systemDefault;
    }

    public void setSystemDefault(Boolean systemDefault) {
        this.systemDefault = systemDefault;
    }
}
