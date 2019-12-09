package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:51
 * @since 3.4.2
 *
 * 属性的注解
 * TODO 作用
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Property {
  /**
   * 属性名
   *
   * A target property name
   */
  String name();

  /**
   * 属性值
   *
   * A property value or placeholder
   */
  String value();
}