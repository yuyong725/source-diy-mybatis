package cn.javadog.sd.mybatis.scripting.defaults;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import cn.javadog.sd.mybatis.support.exceptions.ErrorContext;
import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ParameterMode;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.TypeException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author 余勇
 * @date 2019-12-14 13:36
 * ParameterHandler 接口的默认实现
 */
public class DefaultParameterHandler implements ParameterHandler {

  /**
   * 类型注册表
   */
  private final TypeHandlerRegistry typeHandlerRegistry;

  /**
   * MappedStatement 对象
   */
  private final MappedStatement mappedStatement;

  /**
   * 参数对象
   */
  private final Object parameterObject;

  /**
   * BoundSql 对象
   */
  private final BoundSql boundSql;

  /**
   * 全局配置对象
   */
  private final Configuration configuration;

  /**
   * 构造函数
   */
  public DefaultParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    this.mappedStatement = mappedStatement;
    this.configuration = mappedStatement.getConfiguration();
    this.typeHandlerRegistry = mappedStatement.getConfiguration().getTypeHandlerRegistry();
    this.parameterObject = parameterObject;
    this.boundSql = boundSql;
  }

  /**
   * 获取 parameterObject
   */
  @Override
  public Object getParameterObject() {
    return parameterObject;
  }

  /**
   * 将 parameterObject 参数值填充到 PreparedStatement
   */
  @Override
  public void setParameters(PreparedStatement ps) {
    ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
    // 从 boundSql 中拿到 ParameterMapping 数组
    List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
    if (parameterMappings != null) {
      // 遍历 ParameterMapping 数组
      for (int i = 0; i < parameterMappings.size(); i++) {
        // 获得 ParameterMapping 对象
        ParameterMapping parameterMapping = parameterMappings.get(i);
        // 排除 存储过程 的情况
        if (parameterMapping.getMode() != ParameterMode.OUT) {
          Object value;
          // 先拿到字段名
          String propertyName = parameterMapping.getProperty();
          // 首先从 additional params 中拿，可以看看 issue #448
          if (boundSql.hasAdditionalParameter(propertyName)) {
            value = boundSql.getAdditionalParameter(propertyName);
          }
          // 如果 parameterObject 为空的话，将字段值设置为null
          else if (parameterObject == null) {
            value = null;
          }
          // parameterObject 不为null ，且 typeHandlerRegistry 有该类型的处理器。一般是针对 只有一个参数的情况
          else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            // 直降将 parameterObject 赋值给 value
            value = parameterObject;
          }
          // parameterObject 不为null，typeHandlerRegistry 又没有该类型的处理器。一般是针对多个参数而成的 ParamMap
          else {
            // 拿到 parameterObject 的元信息
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            // 从 parameterObject 拿到 指定属性的 值
            value = metaObject.getValue(propertyName);
          }
          // 从 parameterMapping 中拿到 typeHandler属性
          TypeHandler typeHandler = parameterMapping.getTypeHandler();
          // 从 parameterMapping 中拿到 jdbcType 属性
          JdbcType jdbcType = parameterMapping.getJdbcType();
          if (value == null && jdbcType == null) {
            // 没设置jdbcType，且 value 的值也是 null 的话，就使用 处理null类型的 jdbcType
            jdbcType = configuration.getJdbcTypeForNull();
          }
          // 设置 ? 占位符的参数
          try {
            typeHandler.setParameter(ps, i + 1, value, jdbcType);
          } catch (TypeException | SQLException e) {
            throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
          }
        }
      }
    }
  }

}
