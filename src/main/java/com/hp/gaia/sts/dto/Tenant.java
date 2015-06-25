package com.hp.gaia.sts.dto;

/**
 * Created by belozovs on 6/25/2015.
 */
public class Tenant {

    private int tenantId;
    private String tenantName;
    private String tenantDbName;

    public Tenant(String tenantName, String tenantDbName) {
        this.tenantName = tenantName;
        this.tenantDbName = tenantDbName;
    }

    public Tenant(){
    }

    public int getTenantId() {
        return tenantId;
    }

    public void setTenantId(int tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantName() {
        return tenantName;
    }

    public void setTenantName(String tenantName) {
        this.tenantName = tenantName;
    }

    public String getTenantDbName() {
        return tenantDbName;
    }

    public void setTenantDbName(String tenantDbName) {
        this.tenantDbName = tenantDbName;
    }

}
