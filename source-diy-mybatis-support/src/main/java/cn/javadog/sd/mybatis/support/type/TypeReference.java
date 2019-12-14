package cn.javadog.sd.mybatis.support.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import cn.javadog.sd.mybatis.support.exceptions.TypeException;

/**
 * @author 余勇
 * @date 2019-12-06 19:17
 *
 * 引用泛型抽象类。目的很简单，就是解析类上定义的泛型
 */
public abstract class TypeReference<T> {

  /**
   * 泛型，其实叫ActualTypeArgument更合适
   */
  private final Type rawType;

  /**
   * 构造
   */
  protected TypeReference() {
    rawType = getSuperclassTypeParameter(getClass());
  }

  /**
   * 通过父类拿到范型。因为使用场景是 Ahandler extents BaseHandler<T>(extends TypeReference<T>)
   */
  Type getSuperclassTypeParameter(Class<?> clazz) {
    // 从父类中获取 <T>，其实就是拿到BaseHandler<T>这一层
    Type genericSuperclass = clazz.getGenericSuperclass();
    if (genericSuperclass instanceof Class) {
      // 能满足这个条件的，例如 GenericTypeSupportedInHierarchiesTestCase.CustomStringTypeHandler 这个类
      // TODO 补一个上面的测试用例
      // 排除 TypeReference 类
      if (TypeReference.class != genericSuperclass) {
        // 不断嵌套直到遇到有用点的东西
        return getSuperclassTypeParameter(clazz.getSuperclass());
      }

      throw new TypeException("'" + getClass() + "' extends TypeReference but misses the type parameter. "
        + "Remove the extension or add a type parameter to it.");
    }

    // 获取 <T>，其实这一步一般就拿到了想要的结果
    Type rawType = ((ParameterizedType) genericSuperclass).getActualTypeArguments()[0];
    // TODO remove this when Reflector is fixed to return Types；啥子玩意？
    // 必须是泛型，才获取 <T>，针对于BaseHandler<List<T>>的情形
    if (rawType instanceof ParameterizedType) {
      rawType = ((ParameterizedType) rawType).getRawType();
    }

    return rawType;
  }

  /**
   * 获取范型
   */
  public final Type getRawType() {
    return rawType;
  }

  /**
   * 重写toString
   */
  @Override
  public String toString() {
    return rawType.toString();
  }

}
