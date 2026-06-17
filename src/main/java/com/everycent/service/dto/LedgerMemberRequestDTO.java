package com.everycent.service.dto;

import com.everycent.domain.enumeration.PermissionLevel;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class LedgerMemberRequestDTO {

    @NotNull
    private Long userId;

    @NotNull
    @JsonProperty("permissionType")
    @JsonAlias("permissionLevel")
    private PermissionLevel permissionLevel;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
