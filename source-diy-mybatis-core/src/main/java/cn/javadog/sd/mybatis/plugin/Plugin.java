package cn.javadog.sd.mybatis.plugin;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import cn.javadog.sd.mybatis.support.exceptions.PluginException;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * @author 余勇
 * @date 2019-12-14 12:42
 *
 * 实现 InvocationHandler 接口，插件类，一方面提供创建动态代理对象的方法，另一方面实现对指定类的指定方法的拦截处理。
 */
public class Plugin implements InvocationHandler {

  /**
   * 目标对象
   */
  private final Object target;

  /**
   * 拦截器
   */
  private final Interceptor interceptor;

  /**
   * 拦截的方法映射。
   * 为什么是一对多呢，因为可能有多个方法签名，拦截的都是同一个类的不同方法
   *
   * KEY：类
   * VALUE：方法集合
   */
  private final Map<Class<?>, Set<Method>> signatureMap;

  /**
   * 构造函数
   */
  private Plugin(Object target, Interceptor interceptor, Map<Class<?>, Set<Method>> signatureMap) {
    this.target = target;
    this.interceptor = interceptor;
    this.signatureMap = signatureMap;
  }

  /**
   * 创建目标类的代理对象。静态方法。
   * note 只能拦截四大接口
   *  - Executor (update, query, flushStatements, commit, rollback, getTransaction, close, isClosed)
   *  - ParameterHandler (getParameterObject, setParameters)
   *  - ResultSetHandler (handleResultSets, handleOutputParameters)
   *  - StatementHandler (prepare, parameterize, batch, update, query)
   *
   * TODO 动态代理最明白的使用，务必好好看看
   */
  public static Object wrap(Object target, Interceptor interceptor) {
    // 获得所有拦截的方法映射
    Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
    // 获得目标类的类型
    Class<?> type = target.getClass();
    // 获得目标类的接口集合。note 因为拦截器拦截的只是MyBatis的四个核心接口实现类的方法
    Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
    // 若有接口，则创建目标对象的 JDK Proxy 对象
    if (interfaces.length > 0) {
      return Proxy.newProxyInstance(
              type.getClassLoader(),
              interfaces,
              // 因为 Plugin 实现了 InvocationHandler 接口，所以可以作为 JDK 动态代理的调用处理器
              new Plugin(target, interceptor, signatureMap));
    }
    // 如果没有，则返回原始的目标对象
    return target;
  }

  /**
   * 拦截方法的逻辑
   */
  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      // 获得目标类有哪些方法会被拦截
      Set<Method> methods = signatureMap.get(method.getDeclaringClass());
      // 要执行的方法是否在被拦截的名单里面
      if (methods != null && methods.contains(method)) {
        // 如果是，则拦截处理该方法
        return interceptor.intercept(new Invocation(target, method, args));
      }
      // 如果不是，则调用原方法
      return method.invoke(target, args);
    } catch (Exception e) {
      throw ExceptionUtil.unwrapThrowable(e);
    }
  }

  /**
   * 获得拦截的方法映射
   */
  private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
    // 拿到拦截器上的 @Intercepts 注解对象
    Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
    // 没有注解直接GG，可以看看 issue #251
    if (interceptsAnnotation == null) {
      throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
    }
    // 拿到 @Intercepts 下的 所有方法签名
    Signature[] sigs = interceptsAnnotation.value();
    // 初始化 拦截的方法映射
    Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
    // 遍历签名
    for (Signature sig : sigs) {
      Set<Method> methods = signatureMap.computeIfAbsent(sig.type(), k -> new HashSet<>());
      try {
        // 拿到指定名称指定参数的方法
        Method method = sig.type().getMethod(sig.method(), sig.args());
        methods.add(method);
      } catch (NoSuchMethodException e) {
        throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
      }
    }
    return signatureMap;
  }

  /**
   * 获得目标类的接口集合
   */
  private static Class<?>[] getAllInterfaces(Class<?> type, Map<Class<?>, Set<Method>> signatureMap) {
    // 接口的集合
    Set<Class<?>> interfaces = new HashSet<>();
    // 循环递归 type 类，及其父类
    while (type != null) {
      // 遍历接口集合，若在 signatureMap 中，则添加到 interfaces 中
      for (Class<?> c : type.getInterfaces()) {
        if (signatureMap.containsKey(c)) {
          interfaces.add(c);
        }
      }
      // 获得父类
      type = type.getSuperclass();
    }
    // 创建接口的数组
    return interfaces.toArray(new Class<?>[interfaces.size()]);
  }

}
