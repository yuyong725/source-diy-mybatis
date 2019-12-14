package cn.javadog.sd.mybatis.builder;

import java.util.List;

import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.ParameterMapping;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author 余勇
 * @date 2019-12-11 22:41
 *
 * 静态的 SqlSource 实现类。
 * StaticSqlSource 的静态，是相对于 DynamicSqlSource 和 RawSqlSource 来说呢。
 * 实际上，StaticSqlSource.sql 属性，上面还是可能包括 ? 占位符。
 * note 我理解的 DynamicSqlSource 有 "${}" 和 "#{}", RawSqlSource 有 #{},
 *  StaticSqlSource 啥也没有，但是可能有已经将 #{} 转换而成的 ?。可以认为，DynamicSqlSource 和 RawSqlSource 的 sqlSource 属性值
 *  的类型实际是 StaticSqlSource
 */
public class StaticSqlSource implements SqlSource {

  /**
   * 静态的 SQL
   */
  private final String sql;
  /**
   * ParameterMapping 集合
   */
  private final List<ParameterMapping> parameterMappings;

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * 构造函数
   */
  public StaticSqlSource(Configuration configuration, String sql) {
    this(configuration, sql, null);
  }

  /**
   * 构造函数
   */
  public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.configuration = configuration;
  }

  /**
   * 获取BoundSql
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // 创建 BoundSql 对象
    return new BoundSql(configuration, sql, parameterMappings, parameterObject);
  }

}
