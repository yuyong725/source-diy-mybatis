package cn.javadog.sd.mybatis.scripting.defaults;

import java.util.HashMap;

import cn.javadog.sd.mybatis.builder.SqlSourceBuilder;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.scripting.xmltags.DynamicContext;
import cn.javadog.sd.mybatis.scripting.xmltags.SqlNode;
import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author 余勇
 * @date 2019-12-14 21:44
 *
 * 原始的 SqlSource 实现类。这种比{@link DynamicSqlSource}要快，因为启动服务的时候，就已经加载好了
 */
public class RawSqlSource implements SqlSource {

  /**
   * SqlSource 对象
   */
  private final SqlSource sqlSource;

  /**
   * 构造函数
   */
  public RawSqlSource(Configuration configuration, SqlNode rootSqlNode, Class<?> parameterType) {
    // 获得 Sql
    this(configuration, getSql(configuration, rootSqlNode), parameterType);
  }

  /**
   * 构造函数
   */
  public RawSqlSource(Configuration configuration, String sql, Class<?> parameterType) {
    // 创建 SqlSourceBuilder 对象
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    Class<?> clazz = parameterType == null ? Object.class : parameterType;
    // 获得 SqlSource 对象
    sqlSource = sqlSourceParser.parse(sql, clazz, new HashMap<>());
  }

  /**
   * 获取sql，这种 RawSqlSource 的节点应该是 StaticTextSqlNode，注解读取就行
   */
  private static String getSql(Configuration configuration, SqlNode rootSqlNode) {
    // 创建 DynamicContext 对象
    DynamicContext context = new DynamicContext(configuration, null);
    // 解析出 SqlSource 对象
    rootSqlNode.apply(context);
    // 获得 sql
    return context.getSql();
  }

  /**
   * 获取 BoundSql
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // 获得 BoundSql 对象
    return sqlSource.getBoundSql(parameterObject);
  }

}
