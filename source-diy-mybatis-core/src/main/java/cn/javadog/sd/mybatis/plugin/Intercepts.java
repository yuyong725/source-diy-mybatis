package cn.javadog.sd.mybatis.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-14 12:32
 * 拦截器注解。
 * 使用方法如：
 * @Intercepts({@Signature(type = Map.class, method = "get", args = {Object.class})})
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Intercepts {

  /**
   * 拦截的方法签名的数组，意思可以拦截多个签名
   */
  Signature[] value();
}

