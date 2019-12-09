package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:47
 *
 * 复杂类型的单独属性值的注解
 * TODO 转移
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface One {

  /**
   * @return 已映射语句（也就是映射器方法）的全限定名
   */
  String select() default "";

  /**
   * @return 加载类型
   */
  FetchType fetchType() default FetchType.DEFAULT;

}
