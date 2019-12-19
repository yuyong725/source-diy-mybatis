package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.mapping.FetchType;

/**
 * @author 余勇
 * @date 2019-12-13 12:58
 *
 * 关联查询，一对一的结果。功能类似 xml 中的 <association />
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface One {

  /**
   * @return 已映射语句（也就是映射器方法）的全限定名
   */
  String select() default "";

  /**
   * @return 加载类型
   */
  FetchType fetchType() default FetchType.DEFAULT;

}
