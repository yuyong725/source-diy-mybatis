package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.UnknownTypeHandler;

/**
 * @author Clinton Begin
 *
 *
 */
/**
 * @author: 余勇
 * @date: 2019-12-13 12:54
 *
 * 结果字段的注解，对应 <result /> 标签
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Result {

  /**
   * @return 是否是 ID 字段
   */
  boolean id() default false;


  /**
   * @return 数据库的字段
   */
  String column() default "";

  /**
   * @return Java 类中的属性
   */
  String property() default "";

  /**
   * @return Java Type
   */
  Class<?> javaType() default void.class;

  /**
   * @return JDBC Type
   */
  JdbcType jdbcType() default JdbcType.UNDEFINED;

  /**
   * @return 使用的 TypeHandler 处理器
   */
  Class<? extends TypeHandler> typeHandler() default UnknownTypeHandler.class;

  /**
   * @return {@link One} 注解
   */
  One one() default @One;

  /**
   * @return {@link Many} 注解
   */
  Many many() default @Many;
}
