package cn.javadog.sd.mybatis.binding;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * @author: 余勇
 * @date: 2019-12-10 17:51
 *
 * 实现 InvocationHandler、Serializable 接口，Mapper Proxy
 */
public class MapperProxy<T> implements InvocationHandler, Serializable {

  private static final long serialVersionUID = -6424540398559729838L;

  /**
   * SqlSession 对象,方法的执行由它完成，实际是交给MapperMethod，由它完成
   */
  private final SqlSession sqlSession;

  /**
   * 标记了@mapper的 Mapper 接口
   */
  private final Class<T> mapperInterface;

  /**
   * 方法与 MapperMethod 的映射
   *
   * 从 {@link MapperProxyFactory#getMethodCache()} 传递过来
   */
  private final Map<Method, MapperMethod> methodCache;

  /**
   * 构造函数
   *
   * @param mapperInterface 标记@mapper注解的接口
   * @param sqlSession
   * @param methodCache 方法与 MapperMethod 的映射
   */
  public MapperProxy(SqlSession sqlSession, Class<T> mapperInterface, Map<Method, MapperMethod> methodCache) {
    this.sqlSession = sqlSession;
    this.mapperInterface = mapperInterface;
    this.methodCache = methodCache;
  }

  /**
   * 执行方法
   *
   * @param proxy 执行方法的对象
   * @param method 要执行的方法
   * @param args 执行方法的参数
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      if (Object.class.equals(method.getDeclaringClass())) {
        // 如果是 Object 定义的方法，直接调用
        return method.invoke(this, args);
      } else if (isDefaultMethod(method)) {
        // 如果是接口的默认方法，如下方式调用
        // 见 https://github.com/mybatis/mybatis-3/issues/709 ，支持 JDK8 default 方法
        return invokeDefaultMethod(proxy, method, args);
      }
    } catch (Throwable t) {
      throw ExceptionUtil.unwrapThrowable(t);
    }
    // 获得 MapperMethod 对象
    final MapperMethod mapperMethod = cachedMapperMethod(method);
    // 执行 MapperMethod 方法
    return mapperMethod.execute(sqlSession, args);
  }

  /**
   * 将方法对应的MapperMethod缓存起来，并返回
   */
  private MapperMethod cachedMapperMethod(Method method) {
    return methodCache.computeIfAbsent(method, k -> new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
  }

  /**
   * 进行反射调用，执行接口的默认方法。
   * note 涉及的jdk的反射的API太深了，忽略
   */
  private Object invokeDefaultMethod(Object proxy, Method method, Object[] args)
      throws Throwable {
    final Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
        .getDeclaredConstructor(Class.class, int.class);
    if (!constructor.isAccessible()) {
      constructor.setAccessible(true);
    }
    final Class<?> declaringClass = method.getDeclaringClass();
    return constructor
        .newInstance(declaringClass,
            MethodHandles.Lookup.PRIVATE | MethodHandles.Lookup.PROTECTED
                | MethodHandles.Lookup.PACKAGE | MethodHandles.Lookup.PUBLIC)
        .unreflectSpecial(method, declaringClass).bindTo(proxy).invokeWithArguments(args);
  }

  /**
   * 判断是否接口的默认方法
   * java.lang.reflect.Method#isDefault() 的补丁
   */
  private boolean isDefaultMethod(Method method) {
    return (method.getModifiers()
        & (Modifier.ABSTRACT | Modifier.PUBLIC | Modifier.STATIC)) == Modifier.PUBLIC
        && method.getDeclaringClass().isInterface();
  }
}
