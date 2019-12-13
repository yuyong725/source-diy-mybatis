package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-13 13:28
 *
 * 返回结果的构造函数。类比 xml 中的 <constructor /> 标签
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConstructorArgs {

  /**
   * 字段列表
   */
  Arg[] value() default {};
}
