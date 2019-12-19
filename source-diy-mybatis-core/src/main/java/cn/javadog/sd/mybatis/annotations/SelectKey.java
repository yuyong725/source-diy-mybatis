package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.mapping.StatementType;

/**
 * @author 余勇
 * @date 2019-12-19 19:28
 * 用于主键反写
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SelectKey {
  String[] statement();

  String keyProperty();

  String keyColumn() default "";

  boolean before();

  Class<?> resultType();

  StatementType statementType() default StatementType.PREPARED;
}
