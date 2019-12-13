package cn.javadog.sd.mybatis.scripting.defaults;

import java.util.HashMap;

import cn.javadog.sd.mybatis.builder.SqlSourceBuilder;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.scripting.xmltags.DynamicContext;
import cn.javadog.sd.mybatis.scripting.xmltags.SqlNode;
import cn.javadog.sd.mybatis.session.Configuration;


/**
 * 原始的 SqlSource 实现类
 *
 * Static SqlSource. It is faster than {@link DynamicSqlSource} because mappings are 
 * calculated during startup.
 * 
 * @since 3.2.0
 * @author Eduardo Macarron
 */
public class RawSqlSource implements SqlSource {

  /**
   * SqlSource 对象
   */
  private final SqlSource sqlSource;

  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
    // <1> 获得 Sql
    this(configuration, getSql(configuration, rootSqlNode), parameterType);
  }

  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
    // <2> 创建 SqlSourceBuilder 对象
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> clazz = parameterType == null ? Object.class : parameterType;
    // <2> 获得 SqlSource 对象
    sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<>());
  }

  private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
    // 创建 DynamicContext 对象
    DynamicContext context = new DynamicContext(configuration, null);
    // 解析出 SqlSource 对象
    rootSqlNode.apply(context);
    // 获得 sql
    return context.getSql();
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // 获得 BoundSql 对象
    return sqlSource.getBoundSql(parameterObject);
  }

}
