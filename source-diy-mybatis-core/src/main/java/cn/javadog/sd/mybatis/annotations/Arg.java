package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.UnknownTypeHandler;

/**
 * @author 余勇
 * @date 2019-12-13 13:29
 *
 * @ConstructorArgs 的参数，sql查询结果的构造函数的字段。类比 xml 中的 <idArg /> 或 <arg />
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Arg {

  /**
   * 是否是ID列
   */
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
