package cn.javadog.sd.mybatis.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cn.javadog.sd.mybatis.mapping.ParameterMapping;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.scripting.xmltags.DynamicContext;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.parsing.GenericTokenParser;
import cn.javadog.sd.mybatis.support.parsing.TokenHandler;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaClass;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.type.JdbcType;

/**
 * @author 余勇
 * @date 2019-12-12 14:31
 *
 * 继承 BaseBuilder 抽象类，SqlSource 构建器。
 * 负责将 SQL 语句中的 #{} 替换成相应的 ? 占位符，并获取该 ? 占位符对应的 {@link ParameterMapping} 对象。
 */
public class SqlSourceBuilder extends BaseBuilder {

  /**
   * <parameter /> 标签所包含的属性
   */
  private static final String parameterProperties = "javaType,jdbcType,mode,numericScale,resultMap,typeHandler,jdbcTypeName";

  /**
   * 构造
   */
  public SqlSourceBuilder(Configuration configuration) {
    super(configuration);
  }

  /**
   * 执行解析原始 SQL ，成为 SqlSource 对象
   *
   * @param originalSql 原始 SQL
   * @param parameterType 参数类型
   * @param additionalParameters 附加参数集合。可能是空集合，也可能是 {@link DynamicContext#bindings} 集合
   * @return SqlSource 对象
   */
  public SqlSource parse(String originalSql, Class<?> parameterType, Map<String, Object> additionalParameters) {
    // 创建 ParameterMappingTokenHandler 对象
    ParameterMappingTokenHandler handler = new ParameterMappingTokenHandler(configuration, parameterType, additionalParameters);
    // 创建 GenericTokenParser 对象
    GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
    // 执行解析
    String sql = parser.parse(originalSql);
    // 创建 StaticSqlSource 对象
    return new StaticSqlSource(configuration, sql, handler.getParameterMappings());
  }

  /**
   *  实现 TokenHandler 接口，继承 BaseBuilder 抽象类，
   *  负责将匹配到的 #{ 和 } 对，替换成相应的 ? 占位符，并获取该 ? 占位符对应的 {@link ParameterMapping} 对象。
   */
  private static class ParameterMappingTokenHandler extends BaseBuilder implements TokenHandler {

    /**
     * ParameterMapping 数组
     */
    private List<ParameterMapping> parameterMappings = new ArrayList<>();

    /**
     * 参数类型, 对应  #{} 中的参数的类型。如果有多个参数，那这可能是 <parameterMap /> 的type属性
     */
    private Class<?> parameterType;

    /**
     * additionalParameters 参数的对应的 MetaObject 对象
     */
    private MetaObject metaParameters;

    /**
     * 构造函数
     */
    public ParameterMappingTokenHandler(Configuration configuration, Class<?> parameterType, Map<String, Object> additionalParameters) {
      super(configuration);
      this.parameterType = parameterType;
      // 创建 additionalParameters 参数的对应的 MetaObject 对象
      this.metaParameters = configuration.newMetaObject(additionalParameters);
    }

    /**
     * 获取 ParameterMapping 数组
     */
    public List<ParameterMapping> getParameterMappings() {
      return parameterMappings;
    }

    /**
     * 解析的逻辑
     * @param content 符合"#{}"的部分
     */
    @Override
    public String handleToken(String content) {
      // 构建 ParameterMapping 对象，并添加到 parameterMappings 中
      parameterMappings.add(buildParameterMapping(content));
      // 替换成 ？占位符返回
      return "?";
    }

    /**
     * 构建 ParameterMapping 对象
     */
    private ParameterMapping buildParameterMapping(String content) {
      // 解析成 Map 集合，针对是一个字段，可能是property ，jdbcType等类型
      Map<String, String> propertiesMap = parseParameterMapping(content);
      // 获得属性的名字和类型
      String property = propertiesMap.get("property");
      Class<?> propertyType;
      // 从 additional params 拿属性的 getter
      if (metaParameters.hasGetter(property)) { // issue #448 get type from additional params
        propertyType = metaParameters.getGetterType(property);
      } else if (typeHandlerRegistry.hasTypeHandler(parameterType)) {
        propertyType = parameterType;
      } else if (JdbcType.CURSOR.name().equals(propertiesMap.get("jdbcType"))) {
        // 如果jdbcType类型是 CURSOR 的话，就使用 ResultSet
        propertyType = java.sql.ResultSet.class;
      } else if (property == null || Map.class.isAssignableFrom(parameterType)) {
        // 如果parameterType 是map的话，propertyType 就使用object，这个比较合理
        propertyType = Object.class;
      } else {
        // 从parameterType的元信息中，拿到属性的getter
        MetaClass metaClass = MetaClass.forClass(parameterType, configuration.getReflectorFactory());
        if (metaClass.hasGetter(property)) {
          propertyType = metaClass.getGetterType(property);
        } else {
          propertyType = Object.class;
        }
      }
      // 创建 ParameterMapping.Builder 对象
      ParameterMapping.Builder builder = new ParameterMapping.Builder(configuration, property, propertyType);
      // 初始化 ParameterMapping.Builder 对象的属性
      Class<?> javaType = propertyType;
      String typeHandlerAlias = null;
      // propertiesMap 的属性就是靠 ParameterExpression 参与
      for (Map.Entry<String, String> entry : propertiesMap.entrySet()) {
        String name = entry.getKey();
        String value = entry.getValue();
        if ("javaType".equals(name)) {
          javaType = resolveClass(value);
          builder.javaType(javaType);
        } else if ("jdbcType".equals(name)) {
          builder.jdbcType(resolveJdbcType(value));
        } else if ("mode".equals(name)) {
          builder.mode(resolveParameterMode(value));
        } else if ("numericScale".equals(name)) {
          builder.numericScale(Integer.valueOf(value));
        } else if ("typeHandler".equals(name)) {
          typeHandlerAlias = value;
        } else if ("property".equals(name)) {
          // 最👆已经干了
        } else if ("expression".equals(name)) {
          // 不支持 expression
          throw new BuilderException("Expression based parameters are not supported yet");
        } else {
          throw new BuilderException("An invalid property '" + name + "' was found in mapping #{" + content + "}.  Valid properties are " + parameterProperties);
        }
      }
      // 如果 typeHandlerAlias 非空，则获得对应的 TypeHandler 对象，并设置到 ParameterMapping.Builder 对象中
      if (typeHandlerAlias != null) {
        builder.typeHandler(resolveTypeHandler(javaType, typeHandlerAlias));
      }
      // 创建 ParameterMapping 对象
      return builder.build();
    }

    /**
     * 将 content 解析成 Map。
     * key 为属性名，jdbcType等
     */
    private Map<String, String> parseParameterMapping(String content) {
      try {
        return new ParameterExpression(content);
      } catch (BuilderException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new BuilderException("Parsing error was found in mapping #{" + content + "}.  Check syntax #{property|(expression), var1=value1, var2=value2, ...} ", ex);
      }
    }
  }

}
