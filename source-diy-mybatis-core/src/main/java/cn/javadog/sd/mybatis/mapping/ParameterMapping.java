package cn.javadog.sd.mybatis.mapping;

import java.sql.ResultSet;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author Clinton Begin
 * 参数映射
 */
/**
 * @author: 余勇
 * @date: 2019-12-13 21:45
 *
 * 参数映射。
 * </parameterMap /> 的子标签 <parameter />
 */
public class ParameterMapping {

  /**
   * 全局配置
   */
  private Configuration configuration;

  /**
   * 字段的名字
   */
  private String property;

  /**
   * 参数类型。
   *
   * 目前只需要关注 ParameterMode.IN 的情况，另外的 OUT、INOUT 是在存储过程中使用，暂时无视
   */
  private ParameterMode mode;

  /**
   * Java 类型
   */
  private Class<?> javaType = Object.class;

  /**
   * JDBC 类型
   */
  private JdbcType jdbcType;

  /**
   * 对于数值类型，还有一个小数保留位数的设置，来确定小数点后保留的位数
   */
  private Integer numericScale;

  /**
   * TypeHandler 对象
   *
   * {@link Builder#resolveTypeHandler()}
   */
  private TypeHandler<?> typeHandler;

  /**
   * 貌似只在 ParameterMode 在 OUT、INOUT 是在存储过程中使用
   */
  private String resultMapId;

  /**
   * 貌似只在 ParameterMode 在 OUT、INOUT 是在存储过程中使用
   */
  private String jdbcTypeName;

  /**
   * 表达式。
   *
   * ps：目前暂时不支持
   */
  private String expression;

  /**
   * 构造，对外不开放，由下面的构造器使用
   */
  private ParameterMapping() {
  }

  /**
   * 内部类，ParameterMapping的构造器
   */
  public static class Builder {

    /**
     * 要被构建的parameterMapping空对象
     */
    private ParameterMapping parameterMapping = new ParameterMapping();

    /**
     * 构造函数。会去将参数值设置给parameterMapping的属性
     */
    public Builder(Configuration configuration, String property, TypeHandler<?> typeHandler) {
      parameterMapping.configuration = configuration;
      parameterMapping.property = property;
      parameterMapping.typeHandler = typeHandler;
      // mode的默认值是 IN
      parameterMapping.mode = ParameterMode.IN;
    }
    /**
     * 构造函数。会去将参数值设置给parameterMapping的属性
     */
    public Builder(Configuration configuration, String property, Class<?> javaType) {
      parameterMapping.configuration = configuration;
      parameterMapping.property = property;
      parameterMapping.javaType = javaType;
      // mode的默认值是 IN
      parameterMapping.mode = ParameterMode.IN;
    }

    /**
     * 设置mode
     */
    public Builder mode(ParameterMode mode) {
      parameterMapping.mode = mode;
      return this;
    }

    /**
     * 设置 javaType
     */
    public Builder javaType(Class<?> javaType) {
      parameterMapping.javaType = javaType;
      return this;
    }

    /**
     * 设置 jdbcType
     */
    public Builder jdbcType(JdbcType jdbcType) {
      parameterMapping.jdbcType = jdbcType;
      return this;
    }

    /**
     * 设置 numericScale
     */
    public Builder numericScale(Integer numericScale) {
      parameterMapping.numericScale = numericScale;
      return this;
    }

    /**
     * 设置 resultMapId
     */
    public Builder resultMapId(String resultMapId) {
      parameterMapping.resultMapId = resultMapId;
      return this;
    }

    /**
     * 设置 typeHandler
     */
    public Builder typeHandler(TypeHandler<?> typeHandler) {
      parameterMapping.typeHandler = typeHandler;
      return this;
    }

    /**
     * 设置 jdbcTypeName
     */
    public Builder jdbcTypeName(String jdbcTypeName) {
      parameterMapping.jdbcTypeName = jdbcTypeName;
      return this;
    }

    /**
     * 设置 expression
     */
    public Builder expression(String expression) {
      parameterMapping.expression = expression;
      return this;
    }

    /**
     * 执行构建
     */
    public ParameterMapping build() {
      // 解析 TypeHandler，因为构造函数有两个，可能传进来的只是javaType，那就要根据javaType 去找 typeHandler实例
      resolveTypeHandler();
      // 校验属性是否有缺失
      validate();
      // 返回 parameterMapping，由此看出一个构造器不可重复使用，会有属性残留
      return parameterMapping;
    }

    /**
     * 校验参数
     */
    private void validate() {
      // javaType 是 ResultSet 类型时，必须有一个对应的 resultMapId，这针对是存储过程的场景
      if (ResultSet.class.equals(parameterMapping.javaType)) {
        if (parameterMapping.resultMapId == null) { 
          throw new IllegalStateException("Missing resultmap in property '"  
              + parameterMapping.property + "'.  " 
              + "Parameters of type java.sql.ResultSet require a resultmap.");
        }            
      } else {
        // typeHandler 不能为空
        if (parameterMapping.typeHandler == null) { 
          throw new IllegalStateException("Type handler was null on parameter mapping for property '"
            + parameterMapping.property + "'. It was either not specified and/or could not be found for the javaType ("
            + parameterMapping.javaType.getName() + ") : jdbcType (" + parameterMapping.jdbcType + ") combination.");
        }
      }
    }

    /**
     * 解析typeHandler
     */
    private void resolveTypeHandler() {
      if (parameterMapping.typeHandler == null && parameterMapping.javaType != null) {
        Configuration configuration = parameterMapping.configuration;
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        parameterMapping.typeHandler = typeHandlerRegistry.getTypeHandler(parameterMapping.javaType, parameterMapping.jdbcType);
      }
    }

  }

  public String getProperty() {
    return property;
  }

  /**
   * 就是用于处理存储过程的返回参数
   */
  public ParameterMode getMode() {
    return mode;
  }

  /**
   * 获取 javaType，也是用于处理存储过程的返回参数
   */
  public Class<?> getJavaType() {
    return javaType;
  }

  /**
   * 获取 jdbcType
   * 在UnknownTypeHandler中会用到，针对于 没有要处理的字段的类型处理器
   */
  public JdbcType getJdbcType() {
    return jdbcType;
  }

  /**
   * 获取 numericScale，也是用于处理存储过程的返回参数
   */
  public Integer getNumericScale() {
    return numericScale;
  }

  /**
   * 获取typeHandler，当给 PreparedStatement 设置参数时会用到
   */
  public TypeHandler<?> getTypeHandler() {
    return typeHandler;
  }

  /**
   * 获取resultMapId，用于处理存储过程的返回参数
   */
  public String getResultMapId() {
    return resultMapId;
  }

  /**
   * 获取 jdbcTypeName，用于处理存储过程的返回参数
   */
  public String getJdbcTypeName() {
    return jdbcTypeName;
  }

  /**
   * 获取expression。没有用到的地方，note 上面的注释提到，目前没有做表达式的支持
   */
  public String getExpression() {
    return expression;
  }

  /**
   * 重写 toString 方法，回去打印主要的属性信息
   */
  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ParameterMapping{");
    //sb.append("configuration=").append(configuration); // configuration doesn't have a useful .toString()
    sb.append("property='").append(property).append('\'');
    sb.append(", mode=").append(mode);
    sb.append(", javaType=").append(javaType);
    sb.append(", jdbcType=").append(jdbcType);
    sb.append(", numericScale=").append(numericScale);
    //sb.append(", typeHandler=").append(typeHandler); // typeHandler also doesn't have a useful .toString()
    sb.append(", resultMapId='").append(resultMapId).append('\'');
    sb.append(", jdbcTypeName='").append(jdbcTypeName).append('\'');
    sb.append(", expression='").append(expression).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
