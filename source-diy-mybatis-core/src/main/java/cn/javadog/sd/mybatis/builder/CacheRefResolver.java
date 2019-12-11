package cn.javadog.sd.mybatis.builder;

import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author Clinton Begin
 *
 * Cache 指向解析器
 */

public class CacheRefResolver {
  private final MapperBuilderAssistant assistant;

  /**
   * Cache 指向的命名空间
   */
  private final String cacheRefNamespace;

  public CacheRefResolver(MapperBuilderAssistant assistant, String cacheRefNamespace) {
    this.assistant = assistant;
    this.cacheRefNamespace = cacheRefNamespace;
  }

  public Cache resolveCacheRef() {
    return assistant.useCacheRef(cacheRefNamespace);
  }
}