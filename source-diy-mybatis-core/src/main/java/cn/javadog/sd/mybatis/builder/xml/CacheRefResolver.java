package cn.javadog.sd.mybatis.builder.xml;

import cn.javadog.sd.mybatis.builder.MapperBuilderAssistant;
import cn.javadog.sd.mybatis.support.cache.Cache;

/**
 * @author 余勇
 * @date 2019-12-12 13:19
 * CacheRef 解析器，实际功能是由 {@link #assistant} 完成的，只是用来记录解析过程出错的 <cache-ref /> 标签，最后统一记录下
 * note 源码中是放在 builder 包下，但从功能划分，放在当前包下更合适
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