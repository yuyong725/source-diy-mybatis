package cn.javadog.sd.mybatis.builder.annotation;

import java.lang.reflect.Method;

/**
 * @author Eduardo Macarron
 * 注解方法的处理器
 */
public class MethodResolver {

  /**
   * MapperAnnotationBuilder 对象
   */
  private final MapperAnnotationBuilder annotationBuilder;

  /**
   * Method 方法
   */
  private final Method method;

  public MethodResolver(MapperAnnotationBuilder annotationBuilder, Method method) {
    this.annotationBuilder = annotationBuilder;
    this.method = method;
  }

  public void resolve() {
    // 执行注解方法的解析
    annotationBuilder.parseStatement(method);
  }

}