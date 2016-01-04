package com.hp.gaia.sts.dao;

import com.hp.gaia.sts.dto.Tenant;

/**
 * Created by belozovs on 6/25/2015.
 *
 */

public interface TenantDao {

    void save(Tenant tenant);
    void deleteById(long tenantId);

    Tenant getTenantById(long tenantId);
    Tenant getTenantByAdminName(String name);

}
