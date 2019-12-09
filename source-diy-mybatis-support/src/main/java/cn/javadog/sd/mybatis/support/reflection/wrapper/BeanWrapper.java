package cn.javadog.sd.mybatis.support.reflection.wrapper;

import java.util.List;

import cn.javadog.sd.mybatis.support.exceptions.ReflectionException;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.invoker.Invoker;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaClass;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.meta.SystemMetaObject;
import cn.javadog.sd.mybatis.support.reflection.property.PropertyTokenizer;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * @author: 余勇
 * @date: 2019-12-01 17:43
 *
 * 继承 BaseWrapper 抽象类，普通对象的 ObjectWrapper 实现类，
 * 例如 User、Order 这样的 POJO 类
 */
public class BeanWrapper extends BaseWrapper {

  /**
   * 要被包装的对象
   */
  private final Object object;

  /**
   * 对象的元信息，大部分工作由它完成
   */
  private final MetaClass metaClass;

  /**
   * 构造函数
   */
  public BeanWrapper(MetaObject metaObject, Object object) {
    super(metaObject);
    this.object = object;
    this.metaClass = MetaClass.forClass(object.getClass(), metaObject.getReflectorFactory());
  }

  /**
   * TODO 这个方法处理不了嵌套的逻辑！！！
   * 获取当前对象指定 PropertyTokenizer 的值
   * note 注意这里的调用流程，如 a[1].b.c[2].d['id']
   *     1. 它会先调用 {@link BaseWrapper#resolveCollection(PropertyTokenizer, Object)}，
   *     2. 进入到 {@link MetaObject#getValue(String)},
   *     3. 由于是简单属性 'a'，最终会回到当前的 {@link BeanWrapper#get(PropertyTokenizer)}
   *     4. 进一步进入到 #{@link BeanWrapper#getBeanProperty(PropertyTokenizer, Object)}
   *     5. 这个时候 #{@link BeanWrapper#resolveCollection(PropertyTokenizer, Object)} 才走完，获取到了 'a' 的值
   *     6. 调用  #{@link BeanWrapper#getCollectionValue(PropertyTokenizer, Object)} 获取 'a[1]' 的值
   *
   */
  @Override
  public Object get(PropertyTokenizer prop) {
    if (prop.getIndex() != null) {
      // 获得集合类型的属性的指定位置的值，如学生对象的成绩列表
      Object collection = resolveCollection(prop, object);
      // 获得指定位置的值 获得指定的一次成绩
      return getCollectionValue(prop, collection);
    } else {
      // 普通属性的话，直接取即可
      return getBeanProperty(prop, object);
    }
  }

  /**
   * 设置指定属性的值，逻辑和 get 类似
   */
  @Override
  public void set(PropertyTokenizer prop, Object value) {
    // 设置集合类型的属性的指定位置的值
    if (prop.getIndex() != null) {
      // 获得集合类型的属性
      Object collection = resolveCollection(prop, object);
      // 设置指定位置的值
      setCollectionValue(prop, collection, value);
    } else {
      // 设置属性的值
      setBeanProperty(prop, object, value);
    }
  }

  /**
   * TODO {@link MetaClass#findProperty(String)}
   */
  @Override
  public String findProperty(String name, boolean useCamelCaseMapping) {
    return metaClass.findProperty(name, useCamelCaseMapping);
  }

  /**
   * 获取可读属性列表
   */
  @Override
  public String[] getGetterNames() {
    return metaClass.getGetterNames();
  }

  /**
   * 获取可写属性列表
   */
  @Override
  public String[] getSetterNames() {
    return metaClass.getSetterNames();
  }

  /**
   * TODO 貌似根本没考虑复杂的情形
   */
  @Override
  public Class<?> getSetterType(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      //
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return metaClass.getSetterType(name);
      } else {
        return metaValue.getSetterType(prop.getChildren());
      }
    } else {
      return metaClass.getSetterType(name);
    }
  }

  /**
   * 获得指定属性的 getting 方法的返回值
   */
  @Override
  public Class<?> getGetterType(String name) {
    // 创建 PropertyTokenizer 对象，对 name 进行分词
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 有子表达式
    if (prop.hasNext()) {
      // <1> 创建 MetaObject 对象
      MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
      // 如果 metaValue 为空，则基于 metaClass 获得返回类型
      if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
        return metaClass.getGetterType(name);
      // 如果 metaValue 非空，则基于 metaValue 获得返回类型。
      // 例如：richType.richMap.nihao ，其中 richMap 是 Map 类型，而 nihao 的类型，需要获得到 nihao 的具体值，才能做真正的判断。
      } else {
        // 递归判断子表达式 children ，获得返回值的类型
        return metaValue.getGetterType(prop.getChildren());
      }
    // 有子表达式
    } else {
      // 直接获得返回值的类型
      return metaClass.getGetterType(name);
    }
  }

  @Override
  public boolean hasSetter(String name) {
    PropertyTokenizer prop = new PropertyTokenizer(name);
    if (prop.hasNext()) {
      if (metaClass.hasSetter(prop.getIndexedName())) {
        MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
        if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
          return metaClass.hasSetter(name);
        } else {
          return metaValue.hasSetter(prop.getChildren());
        }
      } else {
        return false;
      }
    } else {
      return metaClass.hasSetter(name);
    }
  }

  /**
   * 是否有指定属性的 getting 方法
   */
  @Override
  public boolean hasGetter(String name) {
    // 创建 PropertyTokenizer 对象，对 name 进行分词
    PropertyTokenizer prop = new PropertyTokenizer(name);
    // 有子表达式
    if (prop.hasNext()) {
      // 判断是否有该属性的 getting 方法
      if (metaClass.hasGetter(prop.getIndexedName())) {
        // 创建 MetaObject 对象
        MetaObject metaValue = metaObject.metaObjectForProperty(prop.getIndexedName());
        // 如果 metaValue 为空，则基于 metaClass 判断是否有该属性的 getting 方法
        if (metaValue == SystemMetaObject.NULL_META_OBJECT) {
          return metaClass.hasGetter(name);
        // 如果 metaValue 非空，则基于 metaValue 判断是否有 getting 方法。
        } else {
          // 递归判断子表达式 children ，判断是否有 getting 方法
          return metaValue.hasGetter(prop.getChildren());
        }
      // 有子表达式
      } else {
        // 判断是否有该属性的 getting 方法
        return false;
      }
    } else {
      return metaClass.hasGetter(name);
    }
  }

  /**
   * 创建指定属性的值
   */
  @Override
  public MetaObject instantiatePropertyValue(String name, PropertyTokenizer prop, ObjectFactory objectFactory) {
    MetaObject metaValue;
    // 获得 setting 方法的方法参数类型
    Class<?> type = getSetterType(prop.getName());
    try {
      // 创建对象
      Object newObject = objectFactory.create(type);
      // 创建 MetaObject 对象
      metaValue = MetaObject.forObject(newObject, metaObject.getObjectFactory(), metaObject.getObjectWrapperFactory(), metaObject.getReflectorFactory());
      // <1> 设置当前对象的值
      set(prop, newObject);
    } catch (Exception e) {
      throw new ReflectionException("Cannot set value of property '" + name + "' because '" + name + "' is null and cannot be instantiated on instance of " + type.getName() + ". Cause:" + e.toString(), e);
    }
    return metaValue;
  }

  /**
   * 获得属性的值
   */
  private Object getBeanProperty(PropertyTokenizer prop, Object object) {
    try {
      // 获取指定字段的invoker去调用即可
      Invoker method = metaClass.getGetInvoker(prop.getName());
      try {
        return method.invoke(object, NO_ARGUMENTS);
      } catch (Throwable t) {
        // 抛出异常并在以后捕捉
        throw ExceptionUtil.unwrapThrowable(t);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Throwable t) {
      throw new ReflectionException("Could not get property '" + prop.getName() + "' from " + object.getClass() + ".  Cause: " + t.toString(), t);
    }
  }

  private void setBeanProperty(PropertyTokenizer prop, Object object, Object value) {
    try {
      Invoker method = metaClass.getSetInvoker(prop.getName());
      Object[] params = {value};
      try {
        method.invoke(object, params);
      } catch (Throwable t) {
        throw ExceptionUtil.unwrapThrowable(t);
      }
    } catch (Throwable t) {
      throw new ReflectionException("Could not set property '" + prop.getName() + "' of '" + object.getClass() + "' with value '" + value + "' Cause: " + t.toString(), t);
    }
  }

  @Override
  public boolean isCollection() {
    return false;
  }

  @Override
  public void add(Object element) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <E> void addAll(List<E> list) {
    throw new UnsupportedOperationException();
  }

}
