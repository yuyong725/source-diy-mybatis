package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Map;

import cn.javadog.sd.mybatis.builder.SqlSourceBuilder;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.session.Configuration;


/**
 * @author Clinton Begin
 *
 * 动态的 SqlSource 实现类
 */
public class DynamicSqlSource implements SqlSource {

  private final Configuration configuration;
  /**
   * 根 SqlNode 对象
   */
  private final SqlNode rootSqlNode;

  public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
    this.configuration = configuration;
    this.rootSqlNode = rootSqlNode;
  }

  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // <1> 应用 rootSqlNode
    DynamicContext context = new DynamicContext(configuration, parameterObject);
    rootSqlNode.apply(context);
    // <2> 创建 SqlSourceBuilder 对象
    SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
    // <2> 解析出 SqlSource 对象
    Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
    SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
    // <3> 获得 BoundSql 对象
    BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
    // <4> 添加附加参数到 BoundSql 对象中
    for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
      boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
    }
    // <5> 返回 BoundSql 对象
    return boundSql;
  }
}
