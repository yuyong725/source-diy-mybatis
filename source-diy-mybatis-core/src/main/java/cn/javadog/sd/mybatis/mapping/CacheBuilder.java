package cn.javadog.sd.mybatis.mapping;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import cn.javadog.sd.mybatis.builder.InitializingObject;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.cache.decorators.BlockingCache;
import cn.javadog.sd.mybatis.support.cache.decorators.LoggingCache;
import cn.javadog.sd.mybatis.support.cache.decorators.LruCache;
import cn.javadog.sd.mybatis.support.cache.decorators.ScheduledCache;
import cn.javadog.sd.mybatis.support.cache.decorators.SynchronizedCache;
import cn.javadog.sd.mybatis.support.cache.impl.PerpetualCache;
import cn.javadog.sd.mybatis.support.exceptions.CacheException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.meta.SystemMetaObject;

/**
 * @author: 余勇
 * @date: 2019-12-13 18:28
 *
 * Cache 构造器。基于装饰者设计模式，进行 Cache 对象的构造
 */
public class CacheBuilder {

  /**
   * 缓存ID，一般就是命名空间
   */
  private final String id;

  /**
   * 缓存实现类
   */
  private Class<? extends Cache> implementation;

  /**
   * 缓存装饰类列表
   */
  private final List<Class<? extends Cache>> decorators;

  /**
   * size属性，某些缓存装饰类或者实现类，可能需要size属性
   */
  private Integer size;

  /**
   * 刷新缓存的间隔，可能为null，就是不刷新
   */
  private Long clearInterval;

  /**
   * 是否可读写，就是序列化
   */
  private boolean readWrite;

  /**
   * properties属性，有些缓存实现类或者装饰类，有一些自定义的属性，就通过该属性传递过来
   */
  private Properties properties;

  /**
   * 缓存读取是否阻塞
   */
  private boolean blocking;

  /**
   * 构造函数
   */
  public CacheBuilder(String id) {
    this.id = id;
    this.decorators = new ArrayList<>();
  }

  /**
   * 设置实现类
   */
  public CacheBuilder implementation(Class<? extends Cache> implementation) {
    this.implementation = implementation;
    return this;
  }

  /**
   * 添加装饰器
   */
  public CacheBuilder addDecorator(Class<? extends Cache> decorator) {
    if (decorator != null) {
      this.decorators.add(decorator);
    }
    return this;
  }

  /**
   * 设置 size
   */
  public CacheBuilder size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * 设置缓存刷新时间
   */
  public CacheBuilder clearInterval(Long clearInterval) {
    this.clearInterval = clearInterval;
    return this;
  }

  /**
   * 设置读写属性
   */
  public CacheBuilder readWrite(boolean readWrite) {
    this.readWrite = readWrite;
    return this;
  }

  /**
   * 设置是否阻塞
   */
  public CacheBuilder blocking(boolean blocking) {
    this.blocking = blocking;
    return this;
  }

  /**
   * 设置 properties
   */
  public CacheBuilder properties(Properties properties) {
    this.properties = properties;
    return this;
  }

  /**
   * 执行构建
   */
  public Cache build() {
    // 设置默认实现
    setDefaultImplementations();
    // 拿到缓存实现类的实例
    Cache cache = newBaseCacheInstance(implementation, id);
    // 将 Properties 设置到 cache 对象
    setCacheProperties(cache);
    // 如果实现类是 PerpetualCache，才进行装饰，原因可以看看 issue #352
    if (PerpetualCache.class.equals(cache.getClass())) {
      // 遍历装饰类
      for (Class<? extends Cache> decorator : decorators) {
        // 进行装饰，装饰后设置给 cache
        cache = newCacheDecoratorInstance(decorator, cache);
        // 将 Properties 设置到 cache 对象
        setCacheProperties(cache);
      }
      // 根据传进来的属性，包装一些标准的装饰器
      cache = setStandardDecorators(cache);
    } else if (!LoggingCache.class.isAssignableFrom(cache.getClass())) {
      // 其他类型的实现，用 LoggingCache 包一层
      cache = new LoggingCache(cache);
    }
    return cache;
  }

  /**
   * 设置默认的实现
   */
  private void setDefaultImplementations() {
    if (implementation == null) {
      // 默认实现类为 PerpetualCache
      implementation = PerpetualCache.class;
      if (decorators.isEmpty()) {
        // 默认装饰类就一个 LruCache
        decorators.add(LruCache.class);
      }
    }
  }

  /**
   * 根据传进来的属性，包装一些标准的装饰器
   */
  private Cache setStandardDecorators(Cache cache) {
    try {
      // 拿到对象的元信息
      MetaObject metaCache = SystemMetaObject.forObject(cache);
      if (size != null && metaCache.hasSetter("size")) {
        // 如果包了多层的 cache 有size属性，就将size属性赋值给它
        metaCache.setValue("size", size);
      }
      // clearInterval不为空的话，包一层 ScheduledCache，就是定时刷新
      if (clearInterval != null) {
        cache = new ScheduledCache(cache);
        ((ScheduledCache) cache).setClearInterval(clearInterval);
      }
      // 可读写的话，包一层SerializedCache， note 删除了，不做可序列化的实现
      if (readWrite) {
        // cache = new SerializedCache(cache);
      }
      // 包一层日志 LoggingCache
      cache = new LoggingCache(cache);
      // 包一层同步 SynchronizedCache
      cache = new SynchronizedCache(cache);
      // 读取阻塞的话，包一层BlockingCache
      if (blocking) {
        cache = new BlockingCache(cache);
      }
      return cache;
    } catch (Exception e) {
      throw new CacheException("Error building standard cache decorators.  Cause: " + e, e);
    }
  }

  /**
   * 将 properties 相关属性设置到 Cache 里面
   */
  private void setCacheProperties(Cache cache) {
    if (properties != null) {
      // 拿到缓存实例的元对象
      MetaObject metaCache = SystemMetaObject.forObject(cache);
      // 遍历
      for (Map.Entry<Object, Object> entry : properties.entrySet()) {
        // 拿到属性名和属性值
        String name = (String) entry.getKey();
        String value = (String) entry.getValue();
        // 如果实现类有相应的属性的话，就去设置
        if (metaCache.hasSetter(name)) {
          Class<?> type = metaCache.getSetterType(name);
          // 只支持基础类型，其他类型会报错
          if (String.class == type) {
            metaCache.setValue(name, value);
          } else if (int.class == type || Integer.class == type) {
            metaCache.setValue(name, Integer.valueOf(value));
          } else if (long.class == type || Long.class == type) {
            metaCache.setValue(name, Long.valueOf(value));
          } else if (short.class == type || Short.class == type) {
            metaCache.setValue(name, Short.valueOf(value));
          } else if (byte.class == type || Byte.class == type) {
            metaCache.setValue(name, Byte.valueOf(value));
          } else if (float.class == type || Float.class == type) {
            metaCache.setValue(name, Float.valueOf(value));
          } else if (boolean.class == type || Boolean.class == type) {
            metaCache.setValue(name, Boolean.valueOf(value));
          } else if (double.class == type || Double.class == type) {
            metaCache.setValue(name, Double.valueOf(value));
          } else {
            throw new CacheException("Unsupported property type for cache: '" + name + "' of type " + type);
          }
        }
      }
    }
    if (InitializingObject.class.isAssignableFrom(cache.getClass())){
      try {
        ((InitializingObject) cache).initialize();
      } catch (Exception e) {
        throw new CacheException("Failed cache initialization for '" +
            cache.getId() + "' on '" + cache.getClass().getName() + "'", e);
      }
    }
  }

  /**
   * 创建缓存实现类的实例
   */
  private Cache newBaseCacheInstance(Class<? extends Cache> cacheClass, String id) {
    // 拿到实现类的构造，该构造有且只能有一个参数，类型为string
    Constructor<? extends Cache> cacheConstructor = getBaseCacheConstructor(cacheClass);
    try {
      // 创建实例
      return cacheConstructor.newInstance(id);
    } catch (Exception e) {
      throw new CacheException("Could not instantiate cache implementation (" + cacheClass + "). Cause: " + e, e);
    }
  }

  /**
   * 拿到缓存实现类的构造。
   * {@link Cache} 的注释提到，构造函数必须有一个string属性，记录缓存的namespace
   */
  private Constructor<? extends Cache> getBaseCacheConstructor(Class<? extends Cache> cacheClass) {
    try {
      return cacheClass.getConstructor(String.class);
    } catch (Exception e) {
      throw new CacheException("Invalid base cache implementation (" + cacheClass + ").  " +
          "Base cache implementations must have a constructor that takes a String id as a parameter.  Cause: " + e, e);
    }
  }

  /**
   * 装饰缓存对象
   */
  private Cache newCacheDecoratorInstance(Class<? extends Cache> cacheClass, Cache base) {
    // 获取装饰类的构造函数
    Constructor<? extends Cache> cacheConstructor = getCacheDecoratorConstructor(cacheClass);
    try {
      // 获取装饰类的实例，将当前缓存对象包一层
      return cacheConstructor.newInstance(base);
    } catch (Exception e) {
      throw new CacheException("Could not instantiate cache decorator (" + cacheClass + "). Cause: " + e, e);
    }
  }

  /**
   * 获取装饰缓存类的构造。因此拿的构造有有且仅有一个参数，类型就是 class
   */
  private Constructor<? extends Cache> getCacheDecoratorConstructor(Class<? extends Cache> cacheClass) {
    try {
      return cacheClass.getConstructor(Cache.class);
    } catch (Exception e) {
      throw new CacheException("Invalid cache decorator (" + cacheClass + ").  " +
          "Cache decorators must have a constructor that takes a Cache instance as a parameter.  Cause: " + e, e);
    }
  }
}
