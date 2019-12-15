package cn.javadog.sd.mybatis.support.logging.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;


/**
 * @author 余勇
 * @date 2019-12-15 14:09
 *
 * ResultSet 日志增强代理类
 */
public final class ResultSetLogger extends BaseJdbcLogger implements InvocationHandler {

  /**
   * BLOB 类型
   */
  private static Set<Integer> BLOB_TYPES = new HashSet<>();

  /**
   * 是否第一条记录
   */
  private boolean first = true;

  /**
   * 行数
   */
  private int rows;

  /**
   * 要被增强的 ResultSet
   */
  private final ResultSet rs;

  /**
   * 所有 blob 类型的列
   */
  private final Set<Integer> blobColumns = new HashSet<>();

  /**
   * 类加载时初始化
   */
  static {
    BLOB_TYPES.add(Types.BINARY);
    BLOB_TYPES.add(Types.BLOB);
    BLOB_TYPES.add(Types.CLOB);
    BLOB_TYPES.add(Types.LONGNVARCHAR);
    BLOB_TYPES.add(Types.LONGVARBINARY);
    BLOB_TYPES.add(Types.LONGVARCHAR);
    BLOB_TYPES.add(Types.NCLOB);
    BLOB_TYPES.add(Types.VARBINARY);
  }

  /**
   * 构造方法
   */
  private ResultSetLogger(ResultSet rs, Log statementLog, int queryStack) {
    super(statementLog, queryStack);
    this.rs = rs;
  }

  /**
   * 执行被拦截的方法
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] params) throws Throwable {
    try {
      if (Object.class.equals(method.getDeclaringClass())) {
        return method.invoke(this, params);
      }    
      Object o = method.invoke(rs, params);
      // 对 next 方法进行日志增强
      if ("next".equals(method.getName())) {
        if (((Boolean) o)) {
          rows++;
          if (isTraceEnabled()) {
            ResultSetMetaData rsmd = rs.getMetaData();
            final int columnCount = rsmd.getColumnCount();
            if (first) {
              first = false;
              printColumnHeaders(rsmd, columnCount);
            }
            printColumnValues(columnCount);
          }
        } else {
          debug("     Total: " + rows, false);
        }
      }
      // 清空列信息
      clearColumnInfo();
      return o;
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
  }

  /**
   * 打印列名，也可能是别名
   */
  private void printColumnHeaders(ResultSetMetaData rsmd, int columnCount) throws SQLException {
    StringBuilder row = new StringBuilder();
    row.append("   Columns: ");
    for (int i = 1; i <= columnCount; i++) {
      if (BLOB_TYPES.contains(rsmd.getColumnType(i))) {
        blobColumns.add(i);
      }
      String colname = rsmd.getColumnLabel(i);
      row.append(colname);
      if (i != columnCount) {
        row.append(", ");
      }
    }
    trace(row.toString(), false);
  }

  /**
   * 打印每一行的值
   */
  private void printColumnValues(int columnCount) {
    StringBuilder row = new StringBuilder();
    row.append("       Row: ");
    for (int i = 1; i <= columnCount; i++) {
      String colname;
      try {
        if (blobColumns.contains(i)) {
          colname = "<<BLOB>>";
        } else {
          colname = rs.getString(i);
        }
      } catch (SQLException e) {
        // generally can't call getString() on a BLOB column
        colname = "<<Cannot Display>>";
      }
      row.append(colname);
      if (i != columnCount) {
        row.append(", ");
      }
    }
    trace(row.toString(), false);
  }

  /**
   * 获取 ResultSet 的代理对象
   */
  public static ResultSet newInstance(ResultSet rs, Log statementLog, int queryStack) {
    InvocationHandler handler = new ResultSetLogger(rs, statementLog, queryStack);
    ClassLoader cl = ResultSet.class.getClassLoader();
    return (ResultSet) Proxy.newProxyInstance(cl, new Class[]{ResultSet.class}, handler);
  }

  /**
   * 获取 rs
   */
  public ResultSet getRs() {
    return rs;
  }

}
