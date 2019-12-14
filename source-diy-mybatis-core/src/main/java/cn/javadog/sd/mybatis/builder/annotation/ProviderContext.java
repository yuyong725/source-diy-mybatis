package cn.javadog.sd.mybatis.builder.annotation;

import java.lang.reflect.Method;

/**
 * @author 余勇
 * @date 2019-12-13 13:43
 *
 * ProviderSqlSource 的上下文
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
   * 构造函数
   */
  ProviderContext(Class<?> mapperType, Method mapperMethod) {
    this.mapperType = mapperType;
    this.mapperMethod = mapperMethod;
  }

  /**
   * 获取Provider所在的接口
   */
  public Class<?> getMapperType() {
    return mapperType;
  }

  /**
   * 获取Provider所加的方法上面
   */
  public Method getMapperMethod() {
    return mapperMethod;
  }

}
