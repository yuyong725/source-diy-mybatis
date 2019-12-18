package cn.javadog.sd.mybatis.executor;

import java.util.ArrayList;
import java.util.List;

import cn.javadog.sd.mybatis.mapping.MappedStatement;

/**
 * @author Jeff Butler
 */
/**
 * @author 余勇
 * @date 2019-12-16 22:18
 *
 */
public class BatchResult {

  /**
   * MappedStatement 对象
   */
  private final MappedStatement mappedStatement;

  /**
   * SQL
   */
  private final String sql;

  /**
   * 参数对象集合。因为同一个SQL，可能使用不同的参数，但共用一个statement
   *
   * 每一个元素，对应一次操作的参数
   */
  private final List<Object> parameterObjects;

  /**
   * 更新数量集合
   *
   * 每一个元素，对应一次操作的更新数量
   */
  private int[] updateCounts;

  /**
   * 构造函数
   */
  public BatchResult(MappedStatement mappedStatement, String sql) {
    super();
    this.mappedStatement = mappedStatement;
    this.sql = sql;
    this.parameterObjects = new ArrayList<>();
  }

  /**
   * 构造函数
   */
  public BatchResult(MappedStatement mappedStatement, String sql, Object parameterObject) {
    this(mappedStatement, sql);
    addParameterObject(parameterObject);
  }

  /**
   * 获取 mappedStatement
   */
  public MappedStatement getMappedStatement() {
    return mappedStatement;
  }

  /**
   * 获取 sql
   */
  public String getSql() {
    return sql;
  }

  /**
   * 拿到最上面的参数值
   */
  @Deprecated
  public Object getParameterObject() {
    return parameterObjects.get(0);
  }

  /**
   * 拿到 parameterObjects
   */
  public List<Object> getParameterObjects() {
    return parameterObjects;
  }

  /**
   * 拿到 updateCounts
   */
  public int[] getUpdateCounts() {
    return updateCounts;
  }

  /**
   * 设置 updateCounts
   */
  public void setUpdateCounts(int[] updateCounts) {
    this.updateCounts = updateCounts;
  }

  /**
   * 添加参数值
   */
  public void addParameterObject(Object parameterObject) {
    this.parameterObjects.add(parameterObject);
  }

}
