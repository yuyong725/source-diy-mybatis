package cn.javadog.sd.mybatis.mapping;

import java.util.HashMap;
import java.util.Map;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.property.PropertyTokenizer;

/**
 * @author 余勇
 * @date 2019-12-13 18:02
 *
 * 封装所有信息的SQL
 *
 * 通过 {@link SqlSource} 处理掉所有动态内容(替换掉 ${})后的完整 SQL 语句，
 * 这个 SQL 依然可能有 '?' 占位符号，与之对应的自然还有 参数映射关系(参数名，参数值怎么取)。
 * 当然也有一些全局的参数，用于替换占位符。存在于 {@link #additionalParameters}
 *
 */
public class BoundSql {

  /**
   * SQL 语句
   */
  private final String sql;

  /**
   * 参数对象
   */
  private final Object parameterObject;

  /**
   * 附加的参数集合
   */
  private final Map<String, Object> additionalParameters;

  /**
   * {@link #additionalParameters} 的 MetaObject 对象
   */
  private final MetaObject metaParameters;

  /**
   * 构造函数
   */
  public BoundSql(Configuration configuration, String sql, Object parameterObject) {
    this.sql = sql;
    this.parameterObject = parameterObject;
    // 默认是空
    this.additionalParameters = new HashMap<>();
    // 初始化
    this.metaParameters = configuration.newMetaObject(additionalParameters);
  }

  /**
   * 获取 SQL 语句，带占位符的
   */
  public String getSql() {
    return sql;
  }

  /**
   * 获取参数对象
   */
  public Object getParameterObject() {
    return parameterObject;
  }

  /**
   * 补充参数里，是否包含 指定字段
   */
  public boolean hasAdditionalParameter(String name) {
    // 解析 name，因为这个name一般就是 #{name} ，可能名称符合特定的语法，如'list[1].name' ，这里解析后就是list
    String paramName = new PropertyTokenizer(name).getName();
    return additionalParameters.containsKey(paramName);
  }

  /**
   * 添加补充参数。使用元对象
   * TODO 为鸡儿要使用元对象，还涉及到反射，直接 put 不好吗
   */
  public void setAdditionalParameter(String name, Object value) {
    metaParameters.setValue(name, value);
  }

  /**
   * 获取指定 名称 在 additionalParameters 中对应的值
   */
  public Object getAdditionalParameter(String name) {
    return metaParameters.getValue(name);
  }
}
