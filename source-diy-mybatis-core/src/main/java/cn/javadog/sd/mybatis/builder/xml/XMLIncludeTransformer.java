package cn.javadog.sd.mybatis.builder.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import cn.javadog.sd.mybatis.builder.MapperBuilderAssistant;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.exceptions.IncompleteElementException;
import cn.javadog.sd.mybatis.support.parsing.PropertyParser;
import cn.javadog.sd.mybatis.support.parsing.XNode;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author: 余勇
 * @date: 2019-12-11 16:07
 *
 * XML <include /> 标签的转换器，负责将 SQL 中的 <include /> 标签转换成对应的 <sql /> 的内容
 */
public class XMLIncludeTransformer {

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * mapper构建工具🔧
   */
  private final MapperBuilderAssistant builderAssistant;

  /**
   * 构造
   */
  public XMLIncludeTransformer(Configuration configuration, MapperBuilderAssistant builderAssistant) {
    this.configuration = configuration;
    this.builderAssistant = builderAssistant;
  }

  /**
   * 将 <include /> 标签，替换成引用的 <sql />
   * 如：
   * <include refid="colsSuffix">
   *    <property name="suffix" value="a" />
   *    <property name="suffix" value="b" />
   * </include>
   */
  public void applyIncludes(Node source) {
    // 创建 variablesContext ，并将 configurationVariables 添加到其中
    Properties variablesContext = new Properties();
    Properties configurationVariables = configuration.getVariables();
    if (configurationVariables != null) {
      variablesContext.putAll(configurationVariables);
    }
    // 处理 <include />
    applyIncludes(source, variablesContext, false);
  }

  /**
   * 使用递归的方式，将 <include /> 标签，替换成引用的 <sql />
   *
   * @param source node节点
   * @param variablesContext 当前上下文的静态变量值
   * @param included TODO
   */
  private void applyIncludes(Node source, final Properties variablesContext, boolean included) {
    // 如果是 <include /> 标签
    if (source.getNodeName().equals("include")) {
      // 获得 refid 对应的 <sql /> 对应的节点
      Node toInclude = findSqlFragment(getStringAttribute(source, "refid"), variablesContext);
      // 获得包含 <include /> 标签内的属性
      Properties toIncludeContext = getVariablesContext(source, variablesContext);
      // 递归调用 #applyIncludes(...) 方法，继续替换。注意，此处是 <sql /> 对应的节点，因为 SQL 节点也可能有 include 标签
      applyIncludes(toInclude, toIncludeContext, true);
      if (toInclude.getOwnerDocument() != source.getOwnerDocument()) {
        // 这个情况，艿艿暂时没调试出来!!! TODO 其实我也不懂，w3c文档操作的。按理除非 SQL 节点 与当前节点 不在一个 XML里面，但这不可能啊
        toInclude = source.getOwnerDocument().importNode(toInclude, true);
      }
      // 将 <include /> 节点替换成 <sql /> 节点。note，这是一个奇葩的 API ，前者为 newNode ，后者为 oldNode
      source.getParentNode().replaceChild(toInclude, source);
      // 将 <sql /> 子节点添加到 <sql /> 节点前面，如 <sql id="colsSuffix">col_${suffix}</sql> 变成 col_${suffix}<sql id="colsSuffix"></sql>
      while (toInclude.hasChildNodes()) {
        // 这里有个点，一定要注意，卡了艿艿很久。当子节点添加到其它节点下面后，这个子节点会不见了，相当于是“移动操作”
        toInclude.getParentNode().insertBefore(toInclude.getFirstChild(), toInclude);
      }
      // 移除 <include /> 标签自身，与上面逻辑相辅相成
      toInclude.getParentNode().removeChild(toInclude);
    }
    // 如果节点类型为 Node.ELEMENT_NODE，但不是 include 节点。比如 <sql /> 节点
    else if (source.getNodeType() == Node.ELEMENT_NODE) {
      // 如果正在处理 <include /> 标签(处理一半，递归调用，未完成)，则替换其上的属性，例如 <sql id="123" lang="${cpu}"> 的情况，lang 属性是可以被替换的
      if (included && !variablesContext.isEmpty()) {
        // 遍历替换上面的属性
        NamedNodeMap attributes = source.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
          Node attr = attributes.item(i);
          attr.setNodeValue(PropertyParser.parse(attr.getNodeValue(), variablesContext));
        }
      }
      // 遍历子节点，递归调用 #applyIncludes(...) 方法，继续替换
      NodeList children = source.getChildNodes();
      for (int i = 0; i < children.getLength(); i++) {
        applyIncludes(children.item(i), variablesContext, included);
      }
    }
    // 如果在处理 <include /> 标签中，并且节点类型为 Node.TEXT_NODE(比如SQL的文本子节点)
    else if (included && source.getNodeType() == Node.TEXT_NODE && !variablesContext.isEmpty()) {
      // 变量非空，则进行变量的替换，并修改原节点 source。note 这并不能修改xml的内容，但这个由xml解析到内存，并修改了的dom树，会在本次查询中使用
      source.setNodeValue(PropertyParser.parse(source.getNodeValue(), variablesContext));
    }
  }

  /**
   * 获得对应的 <sql /> 节点
   */
  private Node findSqlFragment(String refid, Properties variables) {
    // 因为 refid 可能是动态变量，所以进行替换
    refid = PropertyParser.parse(refid, variables);
    // 获得完整的 refid ，格式为 "${namespace}.${refid}"
    refid = builderAssistant.applyCurrentNamespace(refid, true);
    try {
      // 获得对应的 <sql /> 节点
      XNode nodeToInclude = configuration.getSqlFragments().get(refid);
      // 获得 Node 节点，进行克隆 note 源码中大量类似的操作，就是不要更改全局中某些属性的值
      return nodeToInclude.getNode().cloneNode(true);
    } catch (IllegalArgumentException e) {
      throw new IncompleteElementException("Could not find SQL statement to include with refid '" + refid + "'", e);
    }
  }

  /**
   * 获取节点指定属性的值
   */
  private String getStringAttribute(Node node, String name) {
    return node.getAttributes().getNamedItem(name).getNodeValue();
  }

  /**
   * 获得包含 <include /> 标签内的子属性 Properties 对象
   *
   */
  private Properties getVariablesContext(Node node, Properties inheritedVariablesContext) {
    // 获得 <include /> 标签的属性集合
    Map<String, String> declaredProperties = null;
    NodeList children = node.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node n = children.item(i);
      if (n.getNodeType() == Node.ELEMENT_NODE) {
        // 拿到 name 属性
        String name = getStringAttribute(n, "name");
        // 拿到 value 属性，有变量的替换一下
        String value = PropertyParser.parse(getStringAttribute(n, "value"), inheritedVariablesContext);
        if (declaredProperties == null) {
          // 初始化一下，这样代表肯定有属性
          declaredProperties = new HashMap<>();
        }
        if (declaredProperties.put(name, value) != null) {
          // 如果重复定义，抛出异常。note 多么优雅的写法
          throw new BuilderException("Variable " + name + " defined twice in the same include definition");
        }
      }
    }
    // 如果 <include /> 标签内没有属性，直接使用 inheritedVariablesContext 即可
    if (declaredProperties == null) {
      return inheritedVariablesContext;
    } else {
      // 如果 <include /> 标签内有属性，则创建新的 newProperties 集合，将 inheritedVariablesContext + declaredProperties 合并
      // TODO 直接 declaredProperties putAll inheritedVariablesContext 不好吗？
      Properties newProperties = new Properties();
      newProperties.putAll(inheritedVariablesContext);
      newProperties.putAll(declaredProperties);
      return newProperties;
    }
  }
}
