package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:21
 * @since 3.4.3
 *
 * 加在resulttype返回的实体类上，表示MyBatis 查询后使用该构造方法来创建
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.CONSTRUCTOR})
public @interface AutomapConstructor {
}
