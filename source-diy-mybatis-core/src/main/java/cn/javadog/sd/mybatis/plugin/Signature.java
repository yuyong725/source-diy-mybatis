package cn.javadog.sd.mybatis.plugin;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Clinton Begin
 *
 * 方法签名的注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Signature {
  /**
   * @return 类
   */
  Class<?> type();

  /**
   * @return 方法名
   */
  String method();

  /**
   * @return 参数类型
   */
  Class<?>[] args();
}