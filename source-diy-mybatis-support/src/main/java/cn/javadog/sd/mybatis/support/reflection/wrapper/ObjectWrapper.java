package cn.javadog.sd.mybatis.support.reflection.wrapper;

import java.util.List;

import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.property.PropertyTokenizer;

/**
 * @author 余勇
 * @date 2019-12-02 16:03
 *
 * 对象包装器接口, 对对象进行增强
 */
public interface ObjectWrapper {

  /**
   * 获得指定位置的值
   *
   * @param prop PropertyTokenizer 对象，标记了位置 如 student.class[1].score  表示学生第一门课的得分
   * @return 值
   */
  Object get(PropertyTokenizer prop);

  /**
   * 设置 当前对象(调用者) 指定位置(PropertyTokenizer) 的值 (value)
   *
   * @param prop PropertyTokenizer 对象，相当于键
   * @param value 值
   */
  void set(PropertyTokenizer prop, Object value);

  /**
   * 将指定表达式进行转换，如 student[4].age => student. ?
   * TODO 这个逻辑测试有些问题？具体作用待定
   */
  String findProperty(String name, boolean useCamelCaseMapping);

  /**
   * 获取可读属性列表
   */
  String[] getGetterNames();

  /**
   * 获取可写属性列表
   */
  String[] getSetterNames();

  /**
   * 获取指定参数的 set方法参数/setFiledInvoker字段 的类型
   *
   * @param name PropertyTokenizer规则的表达式，不一定是属性！
   */
  Class<?> getSetterType(String name);

  /**
   * 获取指定参数的 get方法参数/getFiledInvoker字段 的类型
   *
   * @param name PropertyTokenizer规则的表达式，不一定是属性！
   */
  Class<?> getGetterType(String name);

  /**
   * 查询指定参数是否有对应的 setType
   */
  boolean hasSetter(String name);

  /**
   * 查询指定参数是否有对应的 getType
   */
  boolean hasGetter(String name);

  /**
   * 是实例化？
   * TODO
   */
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
