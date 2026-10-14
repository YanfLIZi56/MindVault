package com.yanglizi.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * PostgreSQL 数组类型 <-> Java List<String> 的 TypeHandler
 * <p>
 * 规则：
 * - 数据库为 NULL 时，返回空 List（而非 null）
 * - 数据库为空数组 {} 时，返回空 List
 * - 数据库为 {a,b,c} 时，返回 ["a", "b", "c"]
 * - 写入时，Java List 转为 PostgreSQL 数组
 * - 写入时，null 或空 List 转为空数组 {}
 */
@MappedTypes(List.class)
@MappedJdbcTypes(JdbcType.ARRAY)
public class ArrayTypeHandler extends BaseTypeHandler<List<String>> {

    /**
     * 写入数据库：List<String> -> PostgreSQL 数组
     * 处理 null 和空 List：转为空数组
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        // 过滤 null 元素，避免数组中出现 null 导致数据库异常
        String[] arr;
        if (parameter == null || parameter.isEmpty()) {
            arr = new String[0];
        } else {
            arr = parameter.stream()
                    .filter(Objects::nonNull)
                    .toArray(String[]::new);
        }
        Array array = ps.getConnection().createArrayOf("varchar", arr);
        ps.setArray(i, array);
    }

    /**
     * 读取数据库：ResultSet 按列名
     */
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    /**
     * 读取数据库：ResultSet 按列索引
     */
    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    /**
     * 读取数据库：CallableStatement（存储过程）
     */
    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    /**
     * 核心转换方法：java.sql.Array -> List<String>
     * <p>
     * 始终返回非 null 的 List，保证下游代码无需做 null 检查
     */
    private List<String> toList(Array array) throws SQLException {
        // 情况 1：数据库为 NULL
        if (array == null) {
            return new ArrayList<>();
        }

        try {
            Object obj = array.getArray();

            // 情况 2：数组内容为 NULL（极少见）
            switch (obj) {
                case null -> {
                    return new ArrayList<>();
                }


                // 情况 3：标准 String[]（最常见的场景）
                case String[] strArr -> {
                    if (strArr.length == 0) {
                        return new ArrayList<>();
                    }
                    List<String> result = new ArrayList<>(strArr.length);
                    Collections.addAll(result, strArr);
                    return result;
                }


                // 情况 4：Object[]（兜底，某些 JDBC 驱动可能返回这个）
                case Object[] objArr -> {
                    if (objArr.length == 0) {
                        return new ArrayList<>();
                    }
                    List<String> result = new ArrayList<>(objArr.length);
                    for (Object o : objArr) {
                        result.add(o == null ? null : o.toString());
                    }
                    return result;
                }
                default -> {
                }
            }

            // 情况 5：其他未知类型（极端兜底）
            return Collections.singletonList(obj.toString());
        } finally {
            // 释放数据库 Array 资源，避免内存泄漏（重要！）
            try {
                array.free();
            } catch (Exception ignored) {
                // 部分驱动不支持 free，忽略
            }
        }
    }
}