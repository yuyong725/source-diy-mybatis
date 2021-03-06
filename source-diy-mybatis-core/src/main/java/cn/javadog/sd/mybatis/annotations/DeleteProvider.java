package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-19 16:58
 * 删除语句提供器
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DeleteProvider {

  /**
   * @return 提供的类
   */
  Class<?> type();

  /**
   * @return 提供的方法
   */
  String method();
}
