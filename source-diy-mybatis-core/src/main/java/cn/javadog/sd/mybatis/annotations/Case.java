package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-19 16:58
 * 对应 case 标签
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
