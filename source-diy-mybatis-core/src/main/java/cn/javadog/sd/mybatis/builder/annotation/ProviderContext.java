package cn.javadog.sd.mybatis.builder.annotation;

import java.lang.reflect.Method;

/**
 * ProviderSqlSource 的上下文
 * The context object for sql provider method.
 *
 * @author Kazuki Shimizu
 * @since 3.4.5
 */
public final class ProviderContext {

  /**
   * Mapper 接口
   */
  private final Class<?> mapperType;
  /**
   * Mapper 的方法
   */
  private final Method mapperMethod;

  /**
   * Constructor.
   *
   * @param mapperType A mapper interface type that specified provider
   * @param mapperMethod A mapper method that specified provider
   */
  ProviderContext(Class<?> mapperType, Method mapperMethod) {
    this.mapperType = mapperType;
    this.mapperMethod = mapperMethod;
  }

  /**
   * Get a mapper interface type that specified provider.
   *
   * @return A mapper interface type that specified provider
   */
  public Class<?> getMapperType() {
    return mapperType;
  }

  /**
   * Get a mapper method that specified provider.
   *
   * @return A mapper method that specified provider
   */
  public Method getMapperMethod() {
    return mapperMethod;
  }

}
