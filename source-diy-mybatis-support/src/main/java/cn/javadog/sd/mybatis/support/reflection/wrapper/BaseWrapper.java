package cn.javadog.sd.mybatis.support.reflection.wrapper;

import java.util.List;
import java.util.Map;

import cn.javadog.sd.mybatis.support.exceptions.ReflectionException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.property.PropertyTokenizer;

/**
 * @author 余勇
 * @date 2019-12-01 17:40
 *
 * 实现 ObjectWrapper 接口，ObjectWrapper 抽象类，
 * 为子类 BeanWrapper 和 MapWrapper 提供属性值的获取和设置的公用方法
 *
 */
public abstract class BaseWrapper implements ObjectWrapper {

  /**
   * 没有参数的数组，用于 Invoker.invoke() 方法
   */
  protected static final Object[] NO_ARGUMENTS = new Object[0];

  /**
   * MetaObject 对象
   */
  protected final MetaObject metaObject;

  /**
   * 构造函数，protected类型
   */
  protected BaseWrapper(MetaObject metaObject) {
    this.metaObject = metaObject;
  }

  /**
   * 获得指定的集合属性属性的值，标题带有Collection，但看起来和Collection关系不大，实际上真有关系！！！
   * @param prop PropertyTokenizer 对象
   * @param object 指定 Object 对象
   * @return 值
   */
  protected Object resolveCollection(PropertyTokenizer prop, Object object) {
    if ("".equals(prop.getName())) {
      return object;
    } else {
      // 核心读取 Object 对象 PropertyTokenizer 位置的属性的值 的逻辑在 metaObject类里面；
      // note 注意这里是prop.getName()，而不是 prop.getIndexedName()，也就是只获取到 name，对 index 的获取调用下面的 getCollectionValue处理的
      return metaObject.getValue(prop.getName());
    }
  }

  /**
   * 获得集合中指定位置的值
   *
   * @param prop PropertyTokenizer 对象
   * @param collection 集合
   * @return 值
   */
  protected Object getCollectionValue(PropertyTokenizer prop, Object collection) {
    if (collection instanceof Map) {
      // map 类型，根据key查找，index不一定是数字的，当是map类型时，就是key
      return ((Map) collection).get(prop.getIndex());
    } else {
      // collection或数组类型类型，取角标
      int i = Integer.parseInt(prop.getIndex());
      // 根据角标取取就好，分情况强转一下，注意set是不可以读角标的
      if (collection instanceof List) {
        return ((List) collection).get(i);
      } else if (collection instanceof Object[]) {
        return ((Object[]) collection)[i];
      } else if (collection instanceof char[]) {
        return ((char[]) collection)[i];
      } else if (collection instanceof boolean[]) {
        return ((boolean[]) collection)[i];
      } else if (collection instanceof byte[]) {
        return ((byte[]) collection)[i];
      } else if (collection instanceof double[]) {
        return ((double[]) collection)[i];
      } else if (collection instanceof float[]) {
        return ((float[]) collection)[i];
      } else if (collection instanceof int[]) {
        return ((int[]) collection)[i];
      } else if (collection instanceof long[]) {
        return ((long[]) collection)[i];
      } else if (collection instanceof short[]) {
        return ((short[]) collection)[i];
      } else {
        throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
      }
    }
  }

  /**
   * 设置集合中指定位置的值，和getCollectionValue的逻辑差不多，很简单
   *
   * @param prop PropertyTokenizer 对象
   * @param collection 集合
   * @param value 值
   */
  protected void setCollectionValue(PropertyTokenizer prop, Object collection, Object value) {
    if (collection instanceof Map) {
      ((Map) collection).put(prop.getIndex(), value);
    } else {
      int i = Integer.parseInt(prop.getIndex());
      if (collection instanceof List) {
        ((List) collection).set(i, value);
      } else if (collection instanceof Object[]) {
        ((Object[]) collection)[i] = value;
      } else if (collection instanceof char[]) {
        ((char[]) collection)[i] = (Character) value;
      } else if (collection instanceof boolean[]) {
        ((boolean[]) collection)[i] = (Boolean) value;
      } else if (collection instanceof byte[]) {
        ((byte[]) collection)[i] = (Byte) value;
      } else if (collection instanceof double[]) {
        ((double[]) collection)[i] = (Double) value;
      } else if (collection instanceof float[]) {
        ((float[]) collection)[i] = (Float) value;
      } else if (collection instanceof int[]) {
        ((int[]) collection)[i] = (Integer) value;
      } else if (collection instanceof long[]) {
        ((long[]) collection)[i] = (Long) value;
      } else if (collection instanceof short[]) {
        ((short[]) collection)[i] = (Short) value;
      } else {
        throw new ReflectionException("The '" + prop.getName() + "' property of " + collection + " is not a List or Array.");
      }
    }
  }

}