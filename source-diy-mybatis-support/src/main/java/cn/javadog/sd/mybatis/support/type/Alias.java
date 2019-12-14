package cn.javadog.sd.mybatis.support.type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-04 22:09
 * 别名注解。用于xml resultType，比如返回类型是java.lang.Integer, 实际 写int就可以。当然这只是应用之一
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Alias {

  /**
   * 别名的注解
   */
  String value();
}
