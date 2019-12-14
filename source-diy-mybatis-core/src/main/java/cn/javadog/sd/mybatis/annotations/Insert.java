package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-13 11:52
 *
 * 新增语句 注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Insert {

  /**
   * SQL 语句数组
   * note 支持多条语句一起插入，就像可以在 <insert /> 写多条sql一样，不过注意以';'分割
   */
  String[] value();
}
