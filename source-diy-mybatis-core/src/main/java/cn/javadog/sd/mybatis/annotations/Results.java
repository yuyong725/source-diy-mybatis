package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-19 17:01
 * 对应 XML 标签为 <resultMap />
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Results {

  /**
   * resultMap 的标示 ,对应的 <resultMap /> 的ID 属性
   */
  String id() default "";

  /**
   * 每一个映射，对应 <result />
   */
  Result[] value() default {};
}
