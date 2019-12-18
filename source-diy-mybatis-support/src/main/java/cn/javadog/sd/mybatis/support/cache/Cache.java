package cn.javadog.sd.mybatis.support.cache;

import java.util.concurrent.locks.ReadWriteLock;

/**
 * @author 余勇
 * @date 2019-12-04 14:37
 *
 * 缓存容器接口。注意，它是一个容器，有点类似 HashMap ，可以往其中添加各种缓存。
 * 为缓存生产方提供的SPI，关于SPI可以看看：https://juejin.im/post/5af952fdf265da0b9e652de3，就是一种规范
 *
 * 注意这个SPI，mybatis只提供了{@link cn.javadog.sd.mybatis.support.cache.impl.PerpetualCache} 这一种实现，decorators包下的
 * 都是装饰器，核心逻辑是impl包下的实现，装饰器只是做一些增强。
 * TODO https://tech.meituan.com/2018/01/19/mybatis-cache.html 说
 *  具体的装饰链是：SynchronizedCache -> LoggingCache -> SerializedCache -> LruCache -> PerpetualCache？这个顺序是调用者组装的
 *
 * 我们要求每一个命名空间(mapper.xml的namespace)都对应一个缓存实例，mybatis的二级缓存就是这么干的。
 * 这些缓存的实现类必须要有一个构造函数，用于接受string类型的缓存的ID，注意这个ID不是缓存的key，这是这个命名空间的缓存的标示；
 * mybatis是使用命名空间(mapper.xml的namespace)作为ID传给构造器
 * <pre>
 * public MyCache(final String id) {
 *  if (id == null) {
 *    throw new IllegalArgumentException("Cache instances require an ID");
 *  }
 *  this.id = id;
 *  initialize();
 * }
 * </pre>
 */

public interface Cache {

  /**
   * 获取缓存的标识符
   */
  String getId();

  /**
   * 添加指定键的值
   *
   * @param key 可以是任意类型，但通常是 {@link CacheKey}
   * @param value 查询的结果，因为缓存放的实际是查询语句的结果
   */
  void putObject(Object key, Object value);

  /**
   * 获得指定键的值
   */
  Object getObject(Object key);

  /**
   * 移除指定键的值
   * 在3.3.0版本之后，这个方法只会在回滚的时候调用，为了得到缓存中失效的值！！！
   * 这时候如果有其他的线程，因为通过 {@link #getObject(Object)} 获取这个键的缓存而被阻塞了，会将这个锁释放掉
   *
   * 这里之所以这么说，因为 {@link cn.javadog.sd.mybatis.support.cache.decorators.BlockingCache} 对键的增删是会加锁的。
   * BlockingCache 通过getObject获取到的值是null时，会锁掉这个键，直到putObject给这个键赋值了才释放。其他的线程这个时候查询就会被阻塞，
   * 避免直接去撞击数据库
   *
   * TODO 这个BlockingCache很奇怪，找不到就一直阻塞，超时了还没有就报错？如果查一条数据库不存在的记录，会不会放一个空进去？
   *
   * @param key 键
   * @return 会返回失效的值
   */
  Object removeObject(Object key);

  /**
   * 清空缓存
   */
  void clear();

  /**
   * 获得容器中缓存的数量，并不是容器的大小。这个方法不强求子类实现，不会被核心逻辑调用
   */
  int getSize();
  
  /**
   * 获得读取写锁。该方法可以忽略了已经。
   * 这个方法在3.2.6版本后就不会被核心逻辑调用
   *
   * Mybatis要求如果缓存需要阻塞机制，必须自己内部实现
   */
  ReadWriteLock getReadWriteLock();

}