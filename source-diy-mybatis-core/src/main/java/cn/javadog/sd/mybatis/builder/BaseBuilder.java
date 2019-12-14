package cn.javadog.sd.mybatis.builder;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import cn.javadog.sd.mybatis.mapping.ParameterMode;
import cn.javadog.sd.mybatis.mapping.ResultSetType;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeAliasRegistry;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author 余勇
 * @date 2019-12-10 21:43
 * 基础构造器抽象类，为子类提供通用的工具类。
 */
public abstract class BaseBuilder {

  /**
   * 全局配置
   */
  protected final Configuration configuration;

  /**
   * 类型别名注册表，来自于configuration，单独拿出来为了后面使用方便
   */
  protected final TypeAliasRegistry typeAliasRegistry;

  /**
   * 类型处理器注册表，来自于configuration，单独拿出来为了后面使用方便
   */
  protected final TypeHandlerRegistry typeHandlerRegistry;

  /**
   * 构造
   */
  public BaseBuilder(Configuration configuration) {
    this.configuration = configuration;
    this.typeAliasRegistry = this.configuration.getTypeAliasRegistry();
    this.typeHandlerRegistry = this.configuration.getTypeHandlerRegistry();
  }

  /**
   * 获取configuration
   */
  public Configuration getConfiguration() {
    return configuration;
  }

  /**
   * 创建正则表达式
   * TODO 用处？
   *
   * @param regex 指定表达式
   * @param defaultValue 默认表达式
   * @return 正则表达式
   */
  protected Pattern parseExpression(String regex, String defaultValue) {
    return Pattern.compile(regex == null ? defaultValue : regex);
  }

  /**
   * 将字符串转换成boolean类型
   */
  protected Boolean booleanValueOf(String value, Boolean defaultValue) {
    return value == null ? defaultValue : Boolean.valueOf(value);
  }

  /**
   * 将字符串转换成Integer类型
   */
  protected Integer integerValueOf(String value, Integer defaultValue) {
    return value == null ? defaultValue : Integer.valueOf(value);
  }

  /**
   * 将字符串转换成Set<String>类型，以 ',' 分割
   */
  protected Set<String> stringSetValueOf(String value, String defaultValue) {
    value = (value == null ? defaultValue : value);
    return new HashSet<>(Arrays.asList(value.split(",")));
  }

  /**
   * 根据别名解析对应的 JdbcType 类型
   */
  protected JdbcType resolveJdbcType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return JdbcType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving JdbcType. Cause: " + e, e);
    }
  }

  /**
   * 解析别名对应的 ResultSetType 类型
   */
  protected ResultSetType resolveResultSetType(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ResultSetType.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ResultSetType. Cause: " + e, e);
    }
  }

  /**
   * 解析别名对应的 ParameterMode 类型
   */
  protected ParameterMode resolveParameterMode(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return ParameterMode.valueOf(alias);
    } catch (IllegalArgumentException e) {
      throw new BuilderException("Error resolving ParameterMode. Cause: " + e, e);
    }
  }

  /**
   * 创建指定对象
   */
  protected Object createInstance(String alias) {
    // 获得对应的类型
    Class<?> clazz = resolveClass(alias);
    if (clazz == null) {
      return null;
    }
    try {
      // 创建对象
      return resolveClass(alias).newInstance();
    } catch (Exception e) {
      throw new BuilderException("Error creating instance. Cause: " + e, e);
    }
  }

  /**
   * 获取指定别名对应的类型
   */
  protected <T> Class<? extends T> resolveClass(String alias) {
    if (alias == null) {
      return null;
    }
    try {
      return resolveAlias(alias);
    } catch (Exception e) {
      throw new BuilderException("Error resolving class. Cause: " + e, e);
    }
  }

  /**
   * 获取指定的javaType，和指定类型处理器别名，对应的类型处理器
   */
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, String typeHandlerAlias) {
    if (typeHandlerAlias == null) {
      // 别名为空的话直接返回null
      return null;
    }
    // 获取类型处理器别名对应的类型
    Class<?> type = resolveClass(typeHandlerAlias);
    // 如果类型既不是null，又不是 TypeHandler 的子类，直接呵呵
    if (type != null && !TypeHandler.class.isAssignableFrom(type)) {
      throw new BuilderException("Type " + type.getName() + " is not a valid TypeHandler because it does not implement TypeHandler interface");
    }
    // 强转成类型处理器的class，👆已经验证过类型肯定是 TypeHandler
    @SuppressWarnings( "unchecked" )
    Class<? extends TypeHandler<?>> typeHandlerType = (Class<? extends TypeHandler<?>>) type;
    // 获取对应的TypeHandler对象
    return resolveTypeHandler(javaType, typeHandlerType);
  }

  /**
   * 解析javaType对应的typeHandlerType的实例
   * 从 typeHandlerRegistry 中获得或创建对应的 TypeHandler 对象
   */
  protected TypeHandler<?> resolveTypeHandler(Class<?> javaType, Class<? extends TypeHandler<?>> typeHandlerType) {
    // typeHandlerType为空的话直接返回null
    if (typeHandlerType == null) {
      return null;
    }
    // 先获得已经缓存的 TypeHandler 对象(对于已经注册了的TypeHandler，javaType用不上)
    TypeHandler<?> handler = typeHandlerRegistry.getMappingTypeHandler(typeHandlerType);
    if (handler == null) {
      // 如果不存在，进行创建 TypeHandler 对象
      handler = typeHandlerRegistry.getInstance(javaType, typeHandlerType);
    }
    return handler;
  }

  /**
   * 从typeAliasRegistry中获取指定别名对应的类型
   */
  protected <T> Class<? extends T> resolveAlias(String alias) {
    return typeAliasRegistry.resolveAlias(alias);
  }
}
