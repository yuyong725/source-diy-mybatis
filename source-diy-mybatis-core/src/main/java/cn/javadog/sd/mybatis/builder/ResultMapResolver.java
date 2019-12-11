package cn.javadog.sd.mybatis.builder;

import java.util.List;

import cn.javadog.sd.mybatis.mapping.Discriminator;
import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.mapping.ResultMapping;

/**
 * @author Eduardo Macarron
 *
 * ResultMap 解析器
 */
public class ResultMapResolver {
  private final MapperBuilderAssistant assistant;

  /**
   * ResultMap 编号
   */
  private final String id;

  /**
   * 类型
   */
  private final Class<?> type;

  /**
   * 继承自哪个 ResultMap
   */
  private final String extend;

  /**
   * Discriminator 对象
   */
  private final Discriminator discriminator;

  /**
   * ResultMapping 集合
   */
  private final List<ResultMapping> resultMappings;

  /**
   * 是否自动匹配
   */
  private final Boolean autoMapping;

  public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, String extend, Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
    this.assistant = assistant;
    this.id = id;
    this.type = type;
    this.extend = extend;
    this.discriminator = discriminator;
    this.resultMappings = resultMappings;
    this.autoMapping = autoMapping;
  }

  public ResultMap resolve() {
    return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings, this.autoMapping);
  }

}