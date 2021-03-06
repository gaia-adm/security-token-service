package com.hp.gaia.sts.dto;

/**
 * Created by belozovs on 6/25/2015.
 */
public class Tenant {

    private long tenantId;
    private String adminUserName;
    private long createdAt;

    public Tenant(String adminUserName) {
        this.adminUserName = adminUserName;
        this.createdAt = System.currentTimeMillis();
    }

    public Tenant(){
    }

    public long getTenantId() {
        return tenantId;
    }

    public void setTenantId(long tenantId) {
        this.tenantId = tenantId;
    }

    public String getAdminUserName() {
        return adminUserName;
    }

    public void setAdminUserName(String adminUserName) {
        this.adminUserName = adminUserName;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
