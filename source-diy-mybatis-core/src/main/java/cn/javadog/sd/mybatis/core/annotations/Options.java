package cn.javadog.sd.mybatis.support.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author: 余勇
 * @date: 2019-12-09 15:48
 *
 * 操作可选项
 * 通过 useGeneratedKeys + keyProperty + keyColumn 属性，可实现返回自增 ID
 * TODO 转移
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Options {
  /**
   * The options for the {@link Options#flushCache()}.
   * The default is {@link FlushCachePolicy#DEFAULT}
   */
  public enum FlushCachePolicy {
    /** <code>false</code> for select statement; <code>true</code> for insert/update/delete statement. */
    DEFAULT,
    /** Flushes cache regardless of the statement type. */
    TRUE,
    /** Does not flush cache regardless of the statement type. */
    FALSE
  }

  /**
   * @return 是否使用缓存
   */
  boolean useCache() default true;

  /**
   * @return 刷新缓存的策略
   */
  FlushCachePolicy flushCache() default FlushCachePolicy.DEFAULT;

  /**
   * @return 结果类型
   */
  ResultSetType resultSetType() default ResultSetType.DEFAULT;

  /**
   * @return 语句类型
   */
  StatementType statementType() default StatementType.PREPARED;

  /**
   * @return 加载数量
   */
  int fetchSize() default -1;

  /**
   * @return 超时时间
   */
  int timeout() default -1;

  /**
   * @return 是否生成主键
   */
  boolean useGeneratedKeys() default false;

  /**
   * @return 主键在 Java 类中的属性
   */
  String keyProperty() default "";

  /**
   * @return 主键在数据库中的字段
   */
  String keyColumn() default "";

  /**
   * @return 结果集
   */
  String resultSets() default "";
}
