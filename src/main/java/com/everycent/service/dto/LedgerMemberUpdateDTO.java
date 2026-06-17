package com.everycent.service.dto;

import com.everycent.domain.enumeration.PermissionLevel;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class LedgerMemberUpdateDTO {

    @NotNull
    @JsonProperty("permissionType")
    @JsonAlias("permissionLevel")
    private PermissionLevel permissionLevel;

    public PermissionLevel getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(PermissionLevel permissionLevel) {
        this.permissionLevel = permissionLevel;
    }
}
