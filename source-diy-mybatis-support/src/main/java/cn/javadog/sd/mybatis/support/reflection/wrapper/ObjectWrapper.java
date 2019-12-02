package cn.javadog.sd.mybatis.support.reflection.wrapper;

import java.util.List;

import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.property.PropertyTokenizer;

/**
 * @author Clinton Begin
 *
 * 对象包装器接口
 */
public interface ObjectWrapper {

  /**
   * 获得值
   *
   * @param prop PropertyTokenizer 对象，相当于键
   * @return 值
   */
  Object get(PropertyTokenizer prop);

  /**
   * 设置值
   *
   * @param prop PropertyTokenizer 对象，相当于键
   * @param value 值
   */
  void set(PropertyTokenizer prop, Object value);

  String findProperty(String name, boolean useCamelCaseMapping);

  String[] getGetterNames();

  String[] getSetterNames();

  Class<?> getSetterType(String name);

  Class<?> getGetterType(String name);

  boolean hasSetter(String name);

  boolean hasGetter(String name);

  MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory);

  /**
   * 是否为集合
   */
  boolean isCollection();

  /**
   * 添加元素到集合
   */
  void add(Object element);

  /**
   * 添加多个元素到集合
   */
  <E> void addAll(List<E> element);

}
