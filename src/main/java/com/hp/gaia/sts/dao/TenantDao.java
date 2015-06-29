package com.hp.gaia.sts.dao;

import com.hp.gaia.sts.dto.Tenant;

import java.util.List;

/**
 * Created by belozovs on 6/25/2015.
 */

public interface TenantDao {

    public void save(Tenant tenant);
    public void deleteById(int tenantId);

    public Tenant getTenantById(int tenantId);
    public Tenant getTenantByAdminName(String name);

}
