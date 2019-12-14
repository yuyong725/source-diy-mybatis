package cn.javadog.sd.mybatis.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cn.javadog.sd.mybatis.builder.annotation.MapperAnnotationBuilder;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.support.exceptions.BindingException;
import cn.javadog.sd.mybatis.support.util.ResolverUtil;

/**
 * @author 余勇
 * @date 2019-12-10 20:59
 *
 * Mapper 注册表
 */
public class MapperRegistry {

  /**
   * MyBatis Configuration 对象
   */
  private final Configuration config;

  /**
   * MapperProxyFactory 的映射
   *
   * KEY：Mapper 接口
   */
  private final Map<Class<?>, MapperProxyFactory<?>> knownMappers = new HashMap<>();

  /**
   * 构造
   */
  public MapperRegistry(Configuration config) {
    this.config = config;
  }

  /**
   * 获得 Mapper Proxy 对象
   */
  @SuppressWarnings("unchecked")
  public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
    // 获得 @Mapper 对应的 MapperProxyFactory 对象
    final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
    if (mapperProxyFactory == null) {
      // 不存在，则抛出 BindingException 异常
      throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
    }
    try {
      // 创建 Mapper Proxy 对象
      return mapperProxyFactory.newInstance(sqlSession);
    } catch (Exception e) {
      throw new BindingException("Error getting mapper instance. Cause: " + e, e);
    }
  }

  /**
   * 判断是否有指定 Mapper 的代理工厂
   */
  public <T> boolean hasMapper(Class<T> type) {
    return knownMappers.containsKey(type);
  }

  /**
   * 添加到 knownMappers，并完成 mapper 与 xml 的绑定
   */
  public <T> void addMapper(Class<T> type) {
    // 判断，必须是接口。
    if (type.isInterface()) {
      // 已经添加过，则抛出 BindingException 异常
      if (hasMapper(type)) {
        throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
      }
      boolean loadCompleted = false;
      try {
        // 添加到 knownMappers 中
        knownMappers.put(type, new MapperProxyFactory<T>(type));
        /**
         * TODO 这段翻译可能不准确
         * type添加到knownMappers，必须要先于👇的parse动作；
         * 否则的话，👇的parser会自动去绑定type；
         * 如果type已经添加到了knownMappers，parser就不会去尝试绑定。
         */
        // 解析 Mapper 的注解配置
        MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
        parser.parse();
        // 标记加载完成
        loadCompleted = true;

        /**
         * 注意这里没有catch，异常是直接不吊的。对比下面的扫描逻辑，如果mapper所在的包下有其他的不是mapper的接口类，中间出错不吊就好
         * TODO 其实这里catch下异常，打印个warn级别的日志挺好的
         */

      } finally {
        // 若加载未完成，从 knownMappers 中移除
        if (!loadCompleted) {
          knownMappers.remove(type);
        }
      }
    }
  }

  /**
   * 获取已解析的mapper数组
   * @since 3.2.2
   */
  public Collection<Class<?>> getMappers() {
    return Collections.unmodifiableCollection(knownMappers.keySet());
  }

  /**
   * 扫描指定包，并将符合的类，添加到 knownMappers
   * @since 3.2.2
   */
  public void addMappers(String packageName, Class<?> superType) {
    // 扫描指定包下的指定类的子类
    ResolverUtil<Class<?>> resolverUtil = new ResolverUtil<>();
    resolverUtil.find(new ResolverUtil.IsA(superType), packageName);
    Set<Class<? extends Class<?>>> mapperSet = resolverUtil.getClasses();
    // 遍历，添加到 knownMappers 中
    for (Class<?> mapperClass : mapperSet) {
      addMapper(mapperClass);
    }
  }

  /**
   * 扫描指定包下所有类，添加到 knownMappers
   * @since 3.2.2
   */
  public void addMappers(String packageName) {
    addMappers(packageName, Object.class);
  }
  
}
