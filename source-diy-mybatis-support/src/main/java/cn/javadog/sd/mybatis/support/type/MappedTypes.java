package cn.javadog.sd.mybatis.support.type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Eduardo Macarron
 *
 * 匹配的 Java Type 类型的注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedTypes {

  /**
   * @return 匹配的 Java Type 类型的数组
   */
  Class<?>[] value();
}
