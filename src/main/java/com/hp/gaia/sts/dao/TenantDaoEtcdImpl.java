package com.hp.gaia.sts.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.gaia.sts.dto.Tenant;
import com.hp.gaia.sts.util.EtcdClientCreator;
import com.hp.gaia.sts.util.EtcdPaths;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.responses.EtcdException;
import mousio.etcd4j.responses.EtcdKeysResponse;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeoutException;

/**
 * Created by belozovs on 9/16/2015.
 *
 */
public class TenantDaoEtcdImpl implements TenantDao {

    private ObjectMapper objectMapper = new ObjectMapper();
    private EtcdClient etcdClient = EtcdClientCreator.getInstance().getEtcdClient();

    @Override
    public void save(Tenant tenant) {

        if (getTenantByAdminName(tenant.getAdminUserName()) != null) {
            throw new DuplicateKeyException("Provided user already administrates a tenant");
        }

        tenant.setTenantId(generateTenantId(tenant.getAdminUserName()));
        tenant.setCreatedAt(System.currentTimeMillis());

        try {
            etcdClient.put(EtcdPaths.T_PATH + tenant.getTenantId(), objectMapper.writeValueAsString(tenant)).prevExist(false).send().get();
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
            throw new DataAccessResourceFailureException(e.getMessage());
        }

    }

    @Override
    public void deleteById(long tenantId) {
        try {
            etcdClient.delete(EtcdPaths.T_PATH + tenantId).send().get();
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
            throw new DataAccessResourceFailureException(e.getMessage());
        }

    }

    @Override
    public Tenant getTenantById(long tenantId) {

        Tenant tenant = null;
        try {
            EtcdKeysResponse response = etcdClient.get(EtcdPaths.T_PATH + tenantId).send().get();
            tenant = new ObjectMapper().readValue(response.node.value, Tenant.class);
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
        }

        return tenant;
    }

    @Override
    public Tenant getTenantByAdminName(String name) {

        try {
            EtcdKeysResponse response = etcdClient.getDir(EtcdPaths.T_PATH).recursive().send().get();
            List<EtcdKeysResponse.EtcdNode> allTenants = response.node.nodes;
            for (EtcdKeysResponse.EtcdNode tenant : allTenants) {
                Tenant t = objectMapper.readValue(tenant.value, Tenant.class);
                if (t.getAdminUserName().equals(name)) {
                    return t;
                }
            }
        } catch (IOException | TimeoutException | EtcdException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Long generateTenantId(String adminName) {
        Integer random = Math.abs(adminName.hashCode() + ThreadLocalRandom.current().nextInt());
        StringBuffer sb = new StringBuffer().append(random);

        for (int i = 0; i < String.valueOf(Integer.MAX_VALUE).length() - String.valueOf(random).length(); i++) {
            sb.append(0);
        }

        return Long.parseLong(sb.toString());
    }

}
