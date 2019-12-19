package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.handler.UnknownTypeHandler;

/**
 * @author 余勇
 * @date 2019-12-19 19:29
 * TODo
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TypeDiscriminator {
  String column();

  Class<?> javaType() default void.class;

  JdbcType jdbcType() default JdbcType.UNDEFINED;

  Class<? extends TypeHandler> typeHandler() default UnknownTypeHandler.class;

  Case[] cases();
}
