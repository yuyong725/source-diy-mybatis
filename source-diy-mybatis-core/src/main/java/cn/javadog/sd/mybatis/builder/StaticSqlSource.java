package cn.javadog.sd.mybatis.builder;

import java.util.List;

import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.ParameterMapping;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author: 余勇
 * @date: 2019-12-11 22:41
 *
 * 静态的 SqlSource 实现类。
 * StaticSqlSource 的静态，是相对于 DynamicSqlSource 和 RawSqlSource 来说呢。实际上，StaticSqlSource.sql 属性，上面还是可能包括 ? 占位符。
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
  private final Configuration configuration;

  public StaticSqlSource(Configuration configuration, String sql) {
    this(configuration, sql, null);
  }

  public StaticSqlSource(Configuration configuration, String sql, List<ParameterMapping> parameterMappings) {
    this.sql = sql;
    this.parameterMappings = parameterMappings;
    this.configuration = configuration;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // 创建 BoundSql 对象
    return new BoundSql(configuration, sql, parameterMappings, parameterObject);
  }

}
