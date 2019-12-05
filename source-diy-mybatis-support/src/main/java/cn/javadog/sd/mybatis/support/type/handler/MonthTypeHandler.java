package cn.javadog.sd.mybatis.support.type.handler;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Month;

import cn.javadog.sd.mybatis.support.type.BaseTypeHandler;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author: 余勇
 * @date: 2019-12-05 21:19
 * Month(java) <=> int(jdbc)
 */
public class MonthTypeHandler extends BaseTypeHandler<Month> {
    
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Month month, JdbcType type) throws SQLException {
        ps.setInt(i, month.getValue());
    }

    @Override
    public Month getNullableResult(ResultSet rs, String columnName) throws SQLException {
        int month = rs.getInt(columnName);
        return (month == 0 && rs.wasNull()) ? null : Month.of(month);
    }

    @Override
    public Month getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        int month = rs.getInt(columnIndex);
        return (month == 0 && rs.wasNull()) ? null : Month.of(month);
    }

}
