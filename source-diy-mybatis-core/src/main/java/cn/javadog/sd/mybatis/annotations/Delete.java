package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author 余勇
 * @date 2019-12-19 19:25
 * delete 语句
 */
/**
 * @author 余勇
 * @date 2019-12-19 16:58
 * 标记 delete 语句
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Delete {
  String[] value();
}
