package cn.javadog.sd.mybatis.plugin;

import java.util.Properties;

/**
 * @author 余勇
 * @date 2019-12-14 12:03
 * 拦截器接口
 */
public interface Interceptor {

  /**
   * 拦截方法
   *
   * @param invocation 调用信息
   * @return 调用结果
   */
  Object intercept(Invocation invocation) throws Throwable;

  /**
   * 应用插件。如应用成功，则会创建目标对象的代理对象。
   * note 由{@link InterceptorChain#pluginAll(Object)}调用
   *
   * @param target 目标对象
   * @return 应用的结果对象，可以是代理对象，也可以是 target 对象，也可以是任意对象。具体的，看代码实现
   */
  Object plugin(Object target);

  /**
   * 设置拦截器属性
   *
   * @param properties 属性
   */
  void setProperties(Properties properties);

}
