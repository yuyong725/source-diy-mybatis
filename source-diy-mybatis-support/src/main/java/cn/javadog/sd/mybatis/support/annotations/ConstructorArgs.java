package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:38
 * 收集一组结果传递给一个结果对象的构造方法。属性有：value，它是形式参数数组。
 *
 * <pre>
 *  @Select("SELECT id, name FROM user where id= #{id}")
 *  @ConstructorArgs({
 *       @Arg(column = "id",javaType = Integer.class,id = true),
 *       @Arg(column = "name",javaType = String.class)})
 *  public User selectById(int id);
 * </pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConstructorArgs {
  Arg[] value() default {};
}
