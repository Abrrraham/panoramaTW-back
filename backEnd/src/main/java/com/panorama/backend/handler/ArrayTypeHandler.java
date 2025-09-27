package com.panorama.backend.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.*;
import java.util.List;

/**
 * @author: DMK
 * @description:
 * @date: 2024-09-25 17:21:58
 * @version: 1.0
 */
public class ArrayTypeHandler<T> extends BaseTypeHandler<List<T>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<T> parameter, JdbcType jdbcType) throws SQLException {
        // 将Java类型转换为数据库类型
        Array array = ps.getConnection().createArrayOf(getType(parameter), parameter.toArray());
        ps.setArray(i, array);
    }

    @Override
    public List<T> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        // 从数据库类型转换为Java类型
        Array array = rs.getArray(columnName);
        return (List<T>) array.getArray();
    }

    @Override
    public List<T> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Array array = rs.getArray(columnIndex);
        return (List<T>) array.getArray();
    }

    @Override
    public List<T> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Array array = cs.getArray(columnIndex);
        return (List<T>) array.getArray();
    }

    public String getType(List<?> list) {
        if (!list.isEmpty()) {
            String type = list.get(0).getClass().getSimpleName();

            return switch (type) {
                case "Integer" -> "integer";
                case "Long" -> "bigint";
                case "String" -> "varchar"; // or "text" depending on your use case

                case "Double" -> "float8"; // DOUBLE PRECISION

                case "Float" -> "float4"; // REAL

                case "Boolean" -> "boolean";
                case "java.sql.Timestamp" -> "timestamp";
                default -> throw new IllegalArgumentException("Unsupported type: " + type);
            };
        }
        throw new IllegalArgumentException("The list is empty, cannot determine type.");
    }

}
