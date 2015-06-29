package com.hp.gaia.sts.dao;

import com.hp.gaia.sts.dto.Tenant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;

import javax.sql.DataSource;
import java.sql.*;
import java.text.DateFormat;
import java.util.List;

/**
 * Created by belozovs on 6/25/2015.
 */

public class TenantDaoImpl implements TenantDao {

    private JdbcTemplate jdbcTemplate;

    private final String DEFAULT_INSERT_TENANT_STATEMENT = "insert into TENANT (admin_user_name, created_at) values (?, ?)";
    private final String DEFAULT_SELECT_TENANTS_STATEMENT = "select tenant_id, admin_user_name, created_at from TENANT";
    private final String DEFAULT_DELETE_TENANT__BY_ID_STATEMENT = "delete from TENANT where tenant_id = ?";

    private String insertTenantSql = DEFAULT_INSERT_TENANT_STATEMENT;
    private String selectTenantSql = DEFAULT_SELECT_TENANTS_STATEMENT;
    private String deleteTenantSql = DEFAULT_DELETE_TENANT__BY_ID_STATEMENT;

    public TenantDaoImpl(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public void save(Tenant tenant) {
        jdbcTemplate.update(insertTenantSql, new Object[]{tenant.getAdminUserName(), System.currentTimeMillis()});
    }

    @Override
    public void deleteById(int tenantId) {
        jdbcTemplate.update(deleteTenantSql, new Object[]{tenantId});
    }

    @Override
    public Tenant getTenantById(int tenantId) {

        String selectTenantSqlById = selectTenantSql + " where tenant_id = ?";
        return jdbcTemplate.queryForObject(selectTenantSqlById, new Object[]{tenantId}, new TenantRowMapper());

    }

    @Override
    public Tenant getTenantByAdminName(String name) {

        String selectTenantSqlByName = selectTenantSql + " where admin_user_name = ?";
        return jdbcTemplate.queryForObject(selectTenantSqlByName, new Object[]{name}, new TenantRowMapper());

    }

    class TenantRowMapper implements RowMapper<Tenant> {

        @Override
        public Tenant mapRow(ResultSet rs, int rowNum) throws SQLException {

            Tenant tenant = new Tenant();
            tenant.setTenantId(rs.getInt("tenant_id"));
            tenant.setAdminUserName(rs.getString("admin_user_name"));
            tenant.setCreatedAt(rs.getLong("created_at"));

            return tenant;
        }
    }

}
