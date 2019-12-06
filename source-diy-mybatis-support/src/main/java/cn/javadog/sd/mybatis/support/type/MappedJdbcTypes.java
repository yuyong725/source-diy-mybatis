package cn.javadog.sd.mybatis.support.type;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-06 16:58
 * 匹配的 JDBC Type 类型的注解
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedJdbcTypes {

  /**
   * @return 匹配的 JDBC Type 类型的注解
   */
  JdbcType[] value();

  /**
   * @return 是否包含 {@link java.sql.JDBCType#NULL}
   */
  boolean includeNullJdbcType() default false;
}
