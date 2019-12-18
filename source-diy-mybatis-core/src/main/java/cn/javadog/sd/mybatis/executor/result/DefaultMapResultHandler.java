package cn.javadog.sd.mybatis.executor.result;

import java.util.Map;

import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.factory.ReflectorFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.wrapper.ObjectWrapperFactory;

/**
 * @author 余勇
 * @date 2019-12-17 16:25
 *
 * 针对返回map类型的结果处理器
 */
public class DefaultMapResultHandler<K, V> implements ResultHandler<V> {

  /**
   * 要返回结果
   */
  private final Map<K, V> mappedResults;

  /**
   * mapKey
   */
  private final String mapKey;

  /**
   * 对象工厂
   */
  private final ObjectFactory objectFactory;

  /**
   * 包装对象工厂
   */
  private final ObjectWrapperFactory objectWrapperFactory;

  /**
   * 反射工厂
   */
  private final ReflectorFactory reflectorFactory;

  /**
   * 默认构造
   */
  @SuppressWarnings("unchecked")
  public DefaultMapResultHandler(String mapKey, ObjectFactory objectFactory, ObjectWrapperFactory objectWrapperFactory, ReflectorFactory reflectorFactory) {
    this.objectFactory = objectFactory;
    this.objectWrapperFactory = objectWrapperFactory;
    this.reflectorFactory = reflectorFactory;
    // 创建 Map 对象
    this.mappedResults = objectFactory.create(Map.class);
    this.mapKey = mapKey;
  }

  /**
   * 将结果的当前元素，聚合成 Map
   */
  @Override
  public void handleResult(ResultContext<? extends V> context) {
    // 获得 KEY 对应的属性
    final V value = context.getResultObject();
    // 获取元信息
    final MetaObject mo = MetaObject.forObject(value, objectFactory, objectWrapperFactory, reflectorFactory);
    // TODO is that assignment always true?
    final K key = (K) mo.getValue(mapKey);
    // 添加到 mappedResults 中
    mappedResults.put(key, value);
  }

  /**
   * 获取结果
   */
  public Map<K, V> getMappedResults() {
    return mappedResults;
  }
}
