package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.UnknownTypeHandler;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:17
 *
 * 单参数构造方法，是 ConstructorArgs 集合的一部分。
 * 属性有：id, column, javaType, jdbcType, typeHandler, select和 resultMap。
 * id 属性是布尔值，来标识用于比较的属性，和<idArg> XML 元素相似。如下
 *
 * <pre>
 *  @Select("SELECT id, name FROM user where id= #{id}")
 *  @ConstructorArgs({
 *       @Arg(column = "id",javaType = Integer.class,id = true),
 *       @Arg(column = "name",javaType = String.class)})
 *  public User selectById(int id);
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Arg {
  boolean id() default false;

  String column() default "";

  Class<?> javaType() default void.class;

  JdbcType jdbcType() default JdbcType.UNDEFINED;

  Class<? extends TypeHandler> typeHandler() default UnknownTypeHandler.class;

  String select() default "";

  String resultMap() default "";

  String name() default "";

  /**
   * @since 3.5.0
   */
  String columnPrefix() default "";
}
