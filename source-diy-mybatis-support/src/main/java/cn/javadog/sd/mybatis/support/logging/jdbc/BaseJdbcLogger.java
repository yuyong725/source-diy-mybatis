package cn.javadog.sd.mybatis.support.logging.jdbc;

import java.sql.Array;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.util.ArrayUtil;

/**
 * Base class for proxies to do logging
 * 
 * @author Clinton Begin
 * @author Eduardo Macarron
 */
/**
 * @author 余勇
 * @date 2019-12-15 13:18
 * jdbc操作日志的基础类
 */
public abstract class BaseJdbcLogger {

  /**
   * 所有set方法，如 PreparedStatement 很多 set 操作去设置占位符的值。
   * 该集合会在类加载时添加值
   */
  protected static final Set<String> SET_METHODS = new HashSet<>();

  /**
   * 所有执行方法，增删改查都算执行方法，该集合会在类加载时添加值
   */
  protected static final Set<String> EXECUTE_METHODS = new HashSet<>();

  /**
   * 列名与值的对应
   */
  private final Map<Object, Object> columnMap = new HashMap<>();

  /**
   * 所有列名
   */
  private final List<Object> columnNames = new ArrayList<>();

  /**
   * 所有列的值
   */
  private final List<Object> columnValues = new ArrayList<>();

  /**
   * statement 日志打印器
   */
  protected Log statementLog;

  /**
   * 查询栈深
   */
  protected int queryStack;

  /**
   * 构造方法
   */
  public BaseJdbcLogger(Log log, int queryStack) {
    this.statementLog = log;
    if (queryStack == 0) {
      this.queryStack = 1;
    } else {
      this.queryStack = queryStack;
    }
  }

  /**
   * 初始化 SET_METHODS 和 EXECUTE_METHODS
   */
  static {
    SET_METHODS.add("setString");
    SET_METHODS.add("setNString");
    SET_METHODS.add("setInt");
    SET_METHODS.add("setByte");
    SET_METHODS.add("setShort");
    SET_METHODS.add("setLong");
    SET_METHODS.add("setDouble");
    SET_METHODS.add("setFloat");
    SET_METHODS.add("setTimestamp");
    SET_METHODS.add("setDate");
    SET_METHODS.add("setTime");
    SET_METHODS.add("setArray");
    SET_METHODS.add("setBigDecimal");
    SET_METHODS.add("setAsciiStream");
    SET_METHODS.add("setBinaryStream");
    SET_METHODS.add("setBlob");
    SET_METHODS.add("setBoolean");
    SET_METHODS.add("setBytes");
    SET_METHODS.add("setCharacterStream");
    SET_METHODS.add("setNCharacterStream");
    SET_METHODS.add("setClob");
    SET_METHODS.add("setNClob");
    SET_METHODS.add("setObject");
    SET_METHODS.add("setNull");

    EXECUTE_METHODS.add("execute");
    EXECUTE_METHODS.add("executeUpdate");
    EXECUTE_METHODS.add("executeQuery");
    EXECUTE_METHODS.add("addBatch");
  }

  /**
   * 添加列名，列值
   */
  protected void setColumn(Object key, Object value) {
    columnMap.put(key, value);
    columnNames.add(key);
    columnValues.add(value);
  }

  /**
   * 获取指定列的值
   */
  protected Object getColumn(Object key) {
    return columnMap.get(key);
  }

  /**
   * 获取所有列值转成的string
   */
  protected String getParameterValueString() {
    List<Object> typeList = new ArrayList<>(columnValues.size());
    for (Object value : columnValues) {
      if (value == null) {
        typeList.add("null");
      } else {
        // 添加值到typeList，格式为 value(className)
        typeList.add(objectValueString(value) + "(" + value.getClass().getSimpleName() + ")");
      }
    }
    final String parameters = typeList.toString();
    // 去掉首尾，TODO 为何
    return parameters.substring(1, parameters.length() - 1);
  }

  /**
   * 将列值转换成string
   */
  protected String objectValueString(Object value) {
    if (value instanceof Array) {
      try {
        // 主要就是数组类型的toString进行了重写
        return ArrayUtil.toString(((Array) value).getArray());
      } catch (SQLException e) {
        return value.toString();
      }
    }
    return value.toString();
  }

  /**
   * 获取所有列名
   */
  protected String getColumnString() {
    return columnNames.toString();
  }

  /**
   * 清空所有列信息
   */
  protected void clearColumnInfo() {
    columnMap.clear();
    columnNames.clear();
    columnValues.clear();
  }

  /**
   * 将所有分隔符使用" "代理
   */
  protected String removeBreakingWhitespace(String original) {
    // 使用 StringTokenizer 的默认分割，包括空格，换行等
    StringTokenizer whitespaceStripper = new StringTokenizer(original);
    StringBuilder builder = new StringBuilder();
    while (whitespaceStripper.hasMoreTokens()) {
      builder.append(whitespaceStripper.nextToken());
      builder.append(" ");
    }
    return builder.toString();
  }

  /**
   * 是否支持 debug 级别日志
   */
  protected boolean isDebugEnabled() {
    return statementLog.isDebugEnabled();
  }

  /**
   * 是否支持 trace 级别日志
   */
  protected boolean isTraceEnabled() {
    return statementLog.isTraceEnabled();
  }

  /**
   * 打印 debug 级别日志
   */
  protected void debug(String text, boolean input) {
    if (statementLog.isDebugEnabled()) {
      statementLog.debug(prefix(input) + text);
    }
  }

  /**
   * 打印 trace 级别日志
   */
  protected void trace(String text, boolean input) {
    if (statementLog.isTraceEnabled()) {
      statementLog.trace(prefix(input) + text);
    }
  }

  /**
   * 返回指定格式的前缀
   * @param isInput 是否是输入，如输入的参数值
   */
  private String prefix(boolean isInput) {
    // 根据查询的栈深，作为 = 的长度
    char[] buffer = new char[queryStack * 2 + 2];
    Arrays.fill(buffer, '=');
    buffer[queryStack * 2 + 1] = ' ';
    if (isInput) {
      // 输入的话，以 '>' 结尾，最终格式为：======>
      buffer[queryStack * 2] = '>';
    } else {
      // 其他的，以 '<' 结尾，最终格式为：<======
      buffer[0] = '<';
    }
    return new String(buffer);
  }

}
