package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:32
 *
 * 单独实例的值和它对应的映射。
 * 因此这个注解和实际的 ResultMap 很相似，由下面的 Results 注解指定。
 * 基本没用？TODO 是否移掉
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Case {
  String value();

  Class<?> type();

  Result[] results() default {};

  Arg[] constructArgs() default {};
}
