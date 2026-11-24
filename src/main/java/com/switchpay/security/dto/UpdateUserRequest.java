package com.switchpay.security.dto;

import com.switchpay.common.enums.RoleName;
import java.util.Set;

public class UpdateUserRequest {

    private String fullName;
    private Boolean active;
    private Set<RoleName> roles;

    public UpdateUserRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }
}
