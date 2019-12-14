package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.builder.annotation.MapperAnnotationBuilder;

/**
 * @author 余勇
 * @date 2019-12-12 23:12
 *
 * 指向指定命名空间的注解
 * 对应 XML 标签为 <cache-ref />
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespaceRef {

  /**
   * 命名空间(也就是mapper接口)的全类名，这个命名空间关联着一个缓存类
   * 见 {@link MapperAnnotationBuilder #parseCacheRef()} 方法
   */
  Class<?> value() default void.class;

  /**
   * 指向的命名空间
   * @since 3.4.2
   */
  String name() default "";
}
