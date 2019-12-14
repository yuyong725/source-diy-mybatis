package cn.javadog.sd.mybatis.builder.xml;

import java.util.List;

import cn.javadog.sd.mybatis.builder.MapperBuilderAssistant;
import cn.javadog.sd.mybatis.mapping.Discriminator;
import cn.javadog.sd.mybatis.mapping.ResultMap;
import cn.javadog.sd.mybatis.mapping.ResultMapping;

/**
 * @author 余勇
 * @date 2019-12-12 14:26
 * ResultMap 解析器。
 * 记录 xml 解析过程中出现失败的 resultMap，最后统一再解析一遍
 * note 原本是在 builder 包下，但其功能和 {@link cn.javadog.sd.mybatis.builder.annotation.MethodResolver}，因此移到当前包下更合适
 */
public class ResultMapResolver {

  /**
   * MapperBuilderAssistant 对象，大部分解析工作由它完成
   */
  private final MapperBuilderAssistant assistant;

  /**
   * ResultMap 编号，由此可见，一个解析器对应一个 resultMap
   */
  private final String id;

  /**
   * 类型
   */
  private final Class<?> type;

  /**
   * extend 属性，类比 Java类的继承
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

  /**
   * 构造函数
   */
  public ResultMapResolver(MapperBuilderAssistant assistant, String id, Class<?> type, String extend, Discriminator discriminator, List<ResultMapping> resultMappings, Boolean autoMapping) {
    this.assistant = assistant;
    this.id = id;
    this.type = type;
    this.extend = extend;
    this.discriminator = discriminator;
    this.resultMappings = resultMappings;
    this.autoMapping = autoMapping;
  }

  /**
   * 解析ResultMap，交给 assistant 完成
   */
  public ResultMap resolve() {
    return assistant.addResultMap(this.id, this.type, this.extend, this.discriminator, this.resultMappings, this.autoMapping);
  }

}