package cn.javadog.sd.mybatis.builder;

import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author: 余勇
 * @date: 2019-12-12 13:19
 * CacheRef 解析器，实际功能是由 {@link #assistant} 完成的，这是个什么射击模式
 */
public class CacheRefResolver {

  /**
   * MapperBuilderAssistant对象
   */
  private final MapperBuilderAssistant assistant;

  /**
   * Cache 指向的命名空间。注意当前的命名空间信息，assistant是可以拿到的
   */
  private final String cacheRefNamespace;

  /**
   * 构造函数
   */
  public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
    this.assistant = assistant;
    this.cacheRefNamespace = cacheRefNamespace;
  }

  /**
   * 解析 CacheRef
   */
  public Cache resolveCacheRef() {
    return assistant.useCacheRef(cacheRefNamespace);
  }
}