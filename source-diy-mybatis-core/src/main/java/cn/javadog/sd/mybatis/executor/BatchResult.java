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
   * 参数对象集合
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

  public BatchResult(MappedStatement mappedStatement, String sql) {
    super();
    this.mappedStatement = mappedStatement;
    this.sql = sql;
    this.parameterObjects = new ArrayList<>();
  }

  public BatchResult(MappedStatement mappedStatement, String sql, Object parameterObject) {
    this(mappedStatement, sql);
    addParameterObject(parameterObject);
  }

  public MappedStatement getMappedStatement() {
    return mappedStatement;
  }

  public String getSql() {
    return sql;
  }

  @Deprecated
  public Object getParameterObject() {
    return parameterObjects.get(0);
  }

  public List<Object> getParameterObjects() {
    return parameterObjects;
  }

  public int[] getUpdateCounts() {
    return updateCounts;
  }

  public void setUpdateCounts(int[] updateCounts) {
    this.updateCounts = updateCounts;
  }

  public void addParameterObject(Object parameterObject) {
    this.parameterObjects.add(parameterObject);
  }

}
