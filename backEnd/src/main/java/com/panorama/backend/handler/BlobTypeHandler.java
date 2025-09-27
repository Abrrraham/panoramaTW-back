package com.panorama.backend.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 *
 * @Author: Steven Da
 * @Date: 2024/08/14/16:21
 * @Description:
 */
public class BlobTypeHandler extends BaseTypeHandler<byte[]> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, byte[] parameter, JdbcType jdbcType) throws SQLException, SQLException {
        ps.setBytes(i, parameter);
    }
    @Override
    public byte[] getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getBytes(columnName);
    }

    @Override
    public byte[] getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }

    @Override
    public byte[] getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getBytes(columnIndex);
    }
}