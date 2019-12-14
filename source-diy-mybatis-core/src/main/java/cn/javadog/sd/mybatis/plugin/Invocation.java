package cn.javadog.sd.mybatis.plugin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author 余勇
 * @date 2019-12-14 12:36
 *
 * 方法调用信息
 */
public class Invocation {

  /**
   * 拦截的对象，对应 {@link Signature#type()}
   */
  private final Object target;

  /**
   * 拦截的方法，对应 {@link Signature#method()}
   */
  private final Method method;

  /**
   * 拦截的方法参数值，对应 {@link Signature#args()}
   */
  private final Object[] args;

  /**
   * 构造函数
   */
  public Invocation(Object target, Method method, Object[] args) {
    this.target = target;
    this.method = method;
    this.args = args;
  }

  /*所有属性的get方法*/

  public Object getTarget() {
    return target;
  }

  public Method getMethod() {
    return method;
  }

  public Object[] getArgs() {
    return args;
  }

  /**
   * 执行被拦截的方法
   */
  public Object proceed() throws InvocationTargetException, IllegalAccessException {
    return method.invoke(target, args);
  }

}
