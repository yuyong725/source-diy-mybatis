package cn.javadog.sd.mybatis.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.cache.decorators.BlockingCache;
import cn.javadog.sd.mybatis.support.cache.decorators.LruCache;
import cn.javadog.sd.mybatis.support.cache.impl.PerpetualCache;


/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 *
 * 缓存空间配置的注解
 * 对应 XML 标签为 <cache />
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CacheNamespace {

  /**
   * @return 负责存储的 Cache 实现类
   */
  Class<? extends Cache> implementation() default PerpetualCache.class;

  /**
   * @return 负责过期的 Cache 实现类
   */
  Class<? extends Cache> eviction() default LruCache.class;

  /**
   * @return 清空缓存的频率。0 代表不清空
   */
  long flushInterval() default 0;

  /**
   * @return 缓存容器大小
   */
  int size() default 1024;

  boolean readWrite() default true;

  /**
   * @return 是否阻塞。{@link BlockingCache}
   */
  boolean blocking() default false;

  /**
   * Property values for a implementation object.
   * @since 3.4.2
   *
   * {@link Property} 数组
   */
  Property[] properties() default {};
  
}
