package cn.javadog.sd.mybatis.builder.annotation;

import java.lang.reflect.Method;

/**
 * @author 余勇
 * @date 2019-12-13 13:32
 *
 * 注解方法的处理器。用于记录中间处理失败的方法，最后再统计处理
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

  /**
   * 构造函数
   */
  public MethodResolver(MapperAnnotationBuilder annotationBuilder, Method method) {
    this.annotationBuilder = annotationBuilder;
    this.method = method;
  }

  /**
   * 执行注解方法的解析
   */
  public void resolve() {
    // 交给 annotationBuilder 去处理
    annotationBuilder.parseStatement(method);
  }

}