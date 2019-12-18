package cn.javadog.sd.mybatis.test.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author 余勇
 * @date 2019-12-18 14:03
 * sql执行器，执行SQL语句
 */
public class SqlRunner {

  /**
   * 当插入语句不使用自增主键，就返回该值
   */
  public static final int NO_GENERATED_KEY = Integer.MIN_VALUE + 1001;

  /**
   * 连接对象
   */
  private final Connection connection;

  /**
   * 类型转换器注册表
   */
  private final TypeHandlerRegistry typeHandlerRegistry;

  /**
   * 是否使用主键生成
   */
  private boolean useGeneratedKeySupport;

  /**
   * 构造
   */
  public SqlRunner(Connection connection) {
    this.connection = connection;
    this.typeHandlerRegistry = new TypeHandlerRegistry();
  }

  /**
   * 设置 useGeneratedKeySupport
   */
  public void setUseGeneratedKeySupport(boolean useGeneratedKeySupport) {
    this.useGeneratedKeySupport = useGeneratedKeySupport;
  }

  /**
   * 执行插入语句，返回一行结果
   */
  public Map<String, Object> selectOne(String sql, Object... args) throws SQLException {
    List<Map<String, Object>> results = selectAll(sql, args);
    if (results.size() != 1) {
      throw new SQLException("Statement returned " + results.size() + " results where exactly one (1) was expected.");
    }
    return results.get(0);
  }

  /**
   * 查多条记录
   */
  public List<Map<String, Object>> selectAll(String sql, Object... args) throws SQLException {
    PreparedStatement ps = connection.prepareStatement(sql);
    try {
      setParameters(ps, args);
      ResultSet rs = ps.executeQuery();
      return getResults(rs);
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  /**
   * 执行插入语句
   */
  public int insert(String sql, Object... args) throws SQLException {
    PreparedStatement ps;
    if (useGeneratedKeySupport) {
      ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
    } else {
      ps = connection.prepareStatement(sql);
    }

    try {
      setParameters(ps, args);
      ps.executeUpdate();
      if (useGeneratedKeySupport) {
        List<Map<String, Object>> keys = getResults(ps.getGeneratedKeys());
        if (keys.size() == 1) {
          Map<String, Object> key = keys.get(0);
          Iterator<Object> i = key.values().iterator();
          if (i.hasNext()) {
            Object genkey = i.next();
            if (genkey != null) {
              try {
                return Integer.parseInt(genkey.toString());
              } catch (NumberFormatException e) {
                //ignore, no numeric key support
              }
            }
          }
        }
      }
      return NO_GENERATED_KEY;
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  /**
   * 执行更新语句
   */
  public int update(String sql, Object... args) throws SQLException {
    PreparedStatement ps = connection.prepareStatement(sql);
    try {
      setParameters(ps, args);
      return ps.executeUpdate();
    } finally {
      try {
        ps.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  /**
   * 执行删除语句
   */
  public int delete(String sql, Object... args) throws SQLException {
    return update(sql, args);
  }

  /**
   * 执行语句，不管类型
   */
  public void run(String sql) throws SQLException {
    Statement stmt = connection.createStatement();
    try {
      stmt.execute(sql);
    } finally {
      try {
        stmt.close();
      } catch (SQLException e) {
        //ignore
      }
    }
  }

  /**
   * 关闭连接
   */
  public void closeConnection() {
    try {
      connection.close();
    } catch (SQLException e) {
      //ignore
    }
  }

  /**
   * 设置参数
   */
  private void setParameters(PreparedStatement ps, Object... args) throws SQLException {
    for (int i = 0, n = args.length; i < n; i++) {
      if (args[i] == null) {
        throw new SQLException("SqlRunner requires an instance of Null to represent typed null values for JDBC compatibility");
      }
      // 这个 null 是自定义的null，是个枚举对象，其实存到数据库里的值依然是null，但必须指定类型
      else if (args[i] instanceof Null) {
        ((Null) args[i]).getTypeHandler().setParameter(ps, i + 1, null, ((Null) args[i]).getJdbcType());
      }
      else {
        TypeHandler typeHandler = typeHandlerRegistry.getTypeHandler(args[i].getClass());
        if (typeHandler == null) {
          throw new SQLException("SqlRunner could not find a TypeHandler instance for " + args[i].getClass());
        } else {
          typeHandler.setParameter(ps, i + 1, args[i], null);
        }
      }
    }
  }

  /**
   * 从结果集中读取数据
   */
  private List<Map<String, Object>> getResults(ResultSet rs) throws SQLException {
    try {
      List<Map<String, Object>> list = new ArrayList<>();
      List<String> columns = new ArrayList<>();
      List<TypeHandler<?>> typeHandlers = new ArrayList<>();
      ResultSetMetaData rsmd = rs.getMetaData();
      for (int i = 0, n = rsmd.getColumnCount(); i < n; i++) {
        columns.add(rsmd.getColumnLabel(i + 1));
        try {
          Class<?> type = Resources.classForName(rsmd.getColumnClassName(i + 1));
          TypeHandler<?> typeHandler = typeHandlerRegistry.getTypeHandler(type);
          if (typeHandler == null) {
            typeHandler = typeHandlerRegistry.getTypeHandler(Object.class);
          }
          typeHandlers.add(typeHandler);
        } catch (Exception e) {
          typeHandlers.add(typeHandlerRegistry.getTypeHandler(Object.class));
        }
      }
      while (rs.next()) {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0, n = columns.size(); i < n; i++) {
          String name = columns.get(i);
          TypeHandler<?> handler = typeHandlers.get(i);
          row.put(name.toUpperCase(Locale.ENGLISH), handler.getResult(rs, name));
        }
        list.add(row);
      }
      return list;
    } finally {
      if (rs != null) {
        try {
            rs.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

}
