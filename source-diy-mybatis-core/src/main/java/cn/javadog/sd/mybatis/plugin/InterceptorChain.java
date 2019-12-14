package cn.javadog.sd.mybatis.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author 余勇
 * @date 2019-12-14 12:05
 *
 * 拦截器 Interceptor 链。
 * note 扫描 mybatis-config.xml 得到的拦截器存储在 全局配置 configuration 的InterceptorChain里面。
 *  再由其根据拦截器签名的不同，对指定对象加载所有拦截器
 */
public class InterceptorChain {

  /**
   * 拦截器列表
   */
  private final List<Interceptor> interceptors = new ArrayList<>();

  /**
   * 应用所有拦截器到指定目标对象
   */
  public Object pluginAll(Object target) {
    for (Interceptor interceptor : interceptors) {
      // 逐个将拦截器加载在目标对象上
      target = interceptor.plugin(target);
    }
    return target;
  }

  /**
   * 添加拦截器
   */
  public void addInterceptor(Interceptor interceptor) {
    interceptors.add(interceptor);
  }

  /**
   * 获取一个被锁定的拦截器数组
   */
  public List<Interceptor> getInterceptors() {
    return Collections.unmodifiableList(interceptors);
  }

}
