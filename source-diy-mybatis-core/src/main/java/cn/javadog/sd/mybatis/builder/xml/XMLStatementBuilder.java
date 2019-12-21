package cn.javadog.sd.mybatis.builder.xml;

import java.util.List;
import java.util.Locale;

import cn.javadog.sd.mybatis.builder.BaseBuilder;
import cn.javadog.sd.mybatis.builder.MapperBuilderAssistant;
import cn.javadog.sd.mybatis.executor.keygen.Jdbc3KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.KeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.NoKeyGenerator;
import cn.javadog.sd.mybatis.executor.keygen.SelectKeyGenerator;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.ResultSetType;
import cn.javadog.sd.mybatis.mapping.SqlCommandType;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.mapping.StatementType;
import cn.javadog.sd.mybatis.scripting.LanguageDriver;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.parsing.XNode;

/**
 * @author 余勇
 * @date 2019-12-12 22:36
 *
 * 加载 Statement 配置
 * 继承 BaseBuilder 抽象类，Statement XML 配置构建器，主要负责解析 Statement 配置，即 <select />、<insert />、<update />、<delete /> 标签
 */
public class XMLStatementBuilder extends BaseBuilder {

  private final MapperBuilderAssistant builderAssistant;

  /**
   * 当前 XML 节点，例如：<select />、<insert />、<update />、<delete /> 标签
   */
  private final XNode context;

  public XMLStatementBuilder(Configuration configuration, MapperBuilderAssistant builderAssistant, XNode context) {
    super(configuration);
    this.builderAssistant = builderAssistant;
    this.context = context;
  }

  /**
   * 执行 Statement 解析
   */
  public void parseStatementNode() {
    // 获得 id 属性，编号。
    String id = context.getStringAttribute("id");
    // 获得各种属性
    Integer fetchSize = context.getIntAttribute("fetchSize");
    Integer timeout = context.getIntAttribute("timeout");
    String parameterMap = context.getStringAttribute("parameterMap");
    String parameterType = context.getStringAttribute("parameterType");
    Class<?> parameterTypeClass = resolveClass(parameterType);
    String resultMap = context.getStringAttribute("resultMap");
    String resultType = context.getStringAttribute("resultType");
    String lang = context.getStringAttribute("lang");
    // 获得 lang 对应的 LanguageDriver 对象
    LanguageDriver langDriver = getLanguageDriver(lang);
    // 获得 resultType 对应的类
    Class<?> resultTypeClass = resolveClass(resultType);
    // 获得 resultSet 对应的枚举值
    String resultSetType = context.getStringAttribute("resultSetType");
    ResultSetType resultSetTypeEnum = resolveResultSetType(resultSetType);
    // 获得 statementType 对应的枚举值，默认是 PREPARED
    StatementType statementType = StatementType.valueOf(context.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    // 获得 SQL 对应的 SqlCommandType 枚举值，也就是说 SqlCommand 对应的值就是解析的节点的名字
    String nodeName = context.getNode().getNodeName();
    SqlCommandType sqlCommandType = SqlCommandType.valueOf(nodeName.toUpperCase(Locale.ENGLISH));
    // 获得各种属性
    boolean isSelect = sqlCommandType == SqlCommandType.SELECT;
    boolean flushCache = context.getBooleanAttribute("flushCache", !isSelect);
    boolean useCache = context.getBooleanAttribute("useCache", isSelect);
    boolean resultOrdered = context.getBooleanAttribute("resultOrdered", false);
    // 正式解析之前，先创建 XMLIncludeTransformer 对象
    XMLIncludeTransformer includeParser = new XMLIncludeTransformer(configuration, builderAssistant);
    // 替换 <include /> 标签相关的内容
    includeParser.applyIncludes(context.getNode());
    // 解析 <selectKey /> 标签
    processSelectKeyNodes(id, parameterTypeClass, langDriver);
    // 创建 SqlSource
    SqlSource sqlSource = langDriver.createSqlSource(configuration, context, parameterTypeClass);
    // 获得 KeyGenerator 对象
    String resultSets = context.getStringAttribute("resultSets");
    String keyProperty = context.getStringAttribute("keyProperty");
    String keyColumn = context.getStringAttribute("keyColumn");
    KeyGenerator keyGenerator;
    // 优先，从 configuration 中获得 KeyGenerator 对象。
    String keyStatementId = id + SelectKeyGenerator.SELECT_KEY_SUFFIX;
    keyStatementId = builderAssistant.applyCurrentNamespace(keyStatementId, true);
    // 意味着是 <selectKey /> 标签配置的，note 也侧面说明了 selectKey 的优先级是大于 useGeneratedKeys
    if (configuration.hasKeyGenerator(keyStatementId)) {
      keyGenerator = configuration.getKeyGenerator(keyStatementId);
    }
    // 其次，根据标签属性的情况，判断是否使用对应的 Jdbc3KeyGenerator 或者 NoKeyGenerator 对象
    else {
      // 优先，基于 useGeneratedKeys 属性判断，
      keyGenerator = context.getBooleanAttribute("useGeneratedKeys",
          // 没有设置的话就用全局属性，并且要求是INSERT语句
          configuration.isUseGeneratedKeys() && SqlCommandType.INSERT.equals(sqlCommandType))
          // 符合条件就用Jdbc3KeyGenerator，否则用NoKeyGenerator
          ? Jdbc3KeyGenerator.INSTANCE : NoKeyGenerator.INSTANCE;
    }

    // 创建 MappedStatement 对象
    builderAssistant.addMappedStatement(
        id,
        sqlSource,
        statementType,
        sqlCommandType,
        fetchSize,
        timeout,
        parameterMap,
        parameterTypeClass,
        resultMap,
        resultTypeClass,
        resultSetTypeEnum,
        flushCache,
        useCache,
        resultOrdered,
        keyGenerator,
        keyProperty,
        keyColumn,
        langDriver);
  }

  /**
   * 解析 <selectKey /> 标签，核心逻辑是👇的{@link #parseSelectKeyNodes(String, List, Class, LanguageDriver)}
   */
  private void processSelectKeyNodes(String id, Class<?> parameterTypeClass, LanguageDriver langDriver) {
    // 获得 <selectKey /> 节点们
    List<XNode> selectKeyNodes = context.evalNodes("selectKey");
    // 执行解析 <selectKey /> 节点们
    parseSelectKeyNodes(id, selectKeyNodes, parameterTypeClass, langDriver);
    // 移除 <selectKey /> 节点们
    removeSelectKeyNodes(selectKeyNodes);
  }

  /**
   * 所有解析 <selectKey /> 节点.
   * 该节点一般是用于拿插入后生成的主键。
   * <selectKey resultType="java.lang.Integer" keyProperty="id">
   *    CALL IDENTITY()
   * </selectKey>
   */
  private void parseSelectKeyNodes(String parentId, List<XNode> list, Class<?> parameterTypeClass, LanguageDriver langDriver) {
    // 遍历 <selectKey /> 节点们
    for (XNode nodeToHandle : list) {
      // 获得完整 id ，格式为 `${id}!selectKey`
      String id = parentId + SelectKeyGenerator.SELECT_KEY_SUFFIX;
      // 执行解析单个 <selectKey /> 节点
      parseSelectKeyNode(id, nodeToHandle, parameterTypeClass, langDriver);
    }
  }

  /**
   * 执行解析单个 <selectKey /> 节点
   */
  private void parseSelectKeyNode(String id, XNode nodeToHandle, Class<?> parameterTypeClass, LanguageDriver langDriver) {
    // 获得各种属性和对应的类
    String resultType = nodeToHandle.getStringAttribute("resultType");
    Class<?> resultTypeClass = resolveClass(resultType);
    // 获取StatementType，默认使用的是 PREPARED
    StatementType statementType = StatementType.valueOf(nodeToHandle.getStringAttribute("statementType", StatementType.PREPARED.toString()));
    String keyProperty = nodeToHandle.getStringAttribute("keyProperty");
    String keyColumn = nodeToHandle.getStringAttribute("keyColumn");
    boolean executeBefore = "BEFORE".equals(nodeToHandle.getStringAttribute("order", "AFTER"));

    // 创建 MappedStatement 需要用到的默认值
    boolean useCache = false;
    boolean resultOrdered = false;
    KeyGenerator keyGenerator = NoKeyGenerator.INSTANCE;
    Integer fetchSize = null;
    Integer timeout = null;
    boolean flushCache = false;
    String parameterMap = null;
    String resultMap = null;
    ResultSetType resultSetTypeEnum = null;

    // 创建 SqlSource 对象
    SqlSource sqlSource = langDriver.createSqlSource(configuration, nodeToHandle, parameterTypeClass);
    // 创建 SqlCommandType，自然是 SELECT
    SqlCommandType sqlCommandType = SqlCommandType.SELECT;

    // 创建 MappedStatement 对象。属性真鸡儿多，看都不想看
    builderAssistant.addMappedStatement(
        id,
        sqlSource,
        statementType,
        sqlCommandType,
        fetchSize,
        timeout,
        parameterMap,
        parameterTypeClass,
        resultMap,
        resultTypeClass,
        resultSetTypeEnum,
        flushCache,
        useCache,
        resultOrdered,
        keyGenerator,
        keyProperty,
        keyColumn,
        langDriver
    );

    // 获得 SelectKey 的编号，格式为 `${namespace}.${id}`
    id = builderAssistant.applyCurrentNamespace(id, false);
    // 获得 MappedStatement 对象，就是上面的 builderAssistant.addMappedStatement 完成的
    MappedStatement keyStatement = configuration.getMappedStatement(id, false);
    // 创建 SelectKeyGenerator 对象，并添加到 configuration 中
    configuration.addKeyGenerator(id, new SelectKeyGenerator(keyStatement, executeBefore));
  }

  /**
   * 从dom树中移除所有 SelectKey 的标签
   */
  private void removeSelectKeyNodes(List<XNode> selectKeyNodes) {
    for (XNode nodeToHandle : selectKeyNodes) {
      nodeToHandle.getParent().getNode().removeChild(nodeToHandle.getNode());
    }
  }

  /**
   * 获得 lang 对应的 LanguageDriver 对象
   */
  private LanguageDriver getLanguageDriver(String lang) {
    // 解析 lang 对应的类
    Class<? extends LanguageDriver> langClass = null;
    if (lang != null) {
      langClass = resolveClass(lang);
    }
    // 获得 LanguageDriver 对象
    return builderAssistant.getLanguageDriver(langClass);
  }

}
