package cn.javadog.sd.mybatis.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-14 12:34
 *
 * 方法签名的注解。
 * 如 @Signature(type = Map.class, method = "get", args = {Object.class}
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Signature {

  /**
   * 拦截的类
   */
  Class<?> type();

  /**
   * 要拦截的类的方法名
   */
  String method();

  /**
   * 拦截方法的参数类型，因为方法会有重载
   */
  Class<?>[] args();
}