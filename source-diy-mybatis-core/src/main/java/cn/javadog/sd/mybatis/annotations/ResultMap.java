package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-13 12:35
 * 使用的结果集的注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ResultMap {

  /**
   * @return 结果集
   * Mapper XML 中有相关的定义
   */
  String[] value();
}
