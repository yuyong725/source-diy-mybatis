package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.scripting.LanguageDriver;

/**
 * @author 余勇
 * @date 2019-12-19 19:26
 * 指定方法所用的语言驱动
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Lang {
  Class<? extends LanguageDriver> value();
}
