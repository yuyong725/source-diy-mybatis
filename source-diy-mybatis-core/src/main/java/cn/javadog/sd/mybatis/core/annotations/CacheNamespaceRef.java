package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:23
 *
 * 指向指定命名空间的注解，对应 XML 标签为 <cache-ref />
 * 参照另外一个命名空间的缓存来使用。
 * 属性有：value, name。如果你使用了这个注解，你应设置 value 或者 name 属性的其中一个。
 * value 属性用于，name 属性（这个属性仅在MyBatis 3.4.2以上版本生效）直接指定了命名空间的名字。
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespaceRef {

  /**
   * 指定 Java 类型而指定命名空间（命名空间名就是指定的 Java 类型的全限定名）
   */
  Class<?> value() default void.class;

  /**
   * 指向的命名空间
   * @since 3.4.2
   */
  String name() default "";
}
