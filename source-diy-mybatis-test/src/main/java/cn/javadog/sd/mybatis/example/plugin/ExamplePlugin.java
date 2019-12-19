package cn.javadog.sd.mybatis.example.plugin;

import java.util.Properties;

import cn.javadog.sd.mybatis.plugin.Interceptor;
import cn.javadog.sd.mybatis.plugin.Intercepts;
import cn.javadog.sd.mybatis.plugin.Invocation;
import cn.javadog.sd.mybatis.plugin.Plugin;

@Intercepts({})
public class ExamplePlugin implements Interceptor {
  private Properties properties;
  @Override
  public Object intercept(Invocation invocation) throws Throwable {
    return invocation.proceed();
  }

  @Override
  public Object plugin(Object target) {
    return Plugin.wrap(target, this);
  }

  @Override
  public void setProperties(Properties properties) {
    this.properties = properties;
  }

  public Properties getProperties() {
    return properties;
  }

}
