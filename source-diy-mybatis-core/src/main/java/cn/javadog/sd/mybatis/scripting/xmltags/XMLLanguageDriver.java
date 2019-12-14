package cn.javadog.sd.mybatis.scripting.xmltags;

import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.scripting.LanguageDriver;
import cn.javadog.sd.mybatis.scripting.defaults.DefaultParameterHandler;
import cn.javadog.sd.mybatis.scripting.defaults.RawSqlSource;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.parsing.PropertyParser;
import cn.javadog.sd.mybatis.support.parsing.XNode;
import cn.javadog.sd.mybatis.support.parsing.XPathParser;

/**
 * @author 余勇
 * @date 2019-12-14 21:36
 *
 * 实现 LanguageDriver 接口，XML 语言驱动实现类
 */
public class XMLLanguageDriver implements LanguageDriver {

  /**
   * 创建 ParameterHandler 对象，用于解析参数
   */
  @Override
  public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    // 创建 DefaultParameterHandler 对象
    return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
  }

  /**
   * 基于 xml 形式创建 SqlSource
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
    // 创建 XMLScriptBuilder 对象，执行解析
    XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType);
    return builder.parseScriptNode();
  }

  /**
   * 基于注解形式创建 SqlSource。
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
    // 如果是 <script> 开头，使用 XML 配置的方式，使用动态 SQL。也就是说，可以把xml格式的SQL直接写在注解上，但必须使用<script>。可以看看 issue #3
    // 如 @Select("<script>SELECT firstName <if test=\"includeLastName != null\">, lastName</if> FROM names WHERE lastName LIKE #{name}</script>")
    if (script.startsWith("<script>")) {
      // 创建 XPathParser 对象，解析出 <script /> 节点
      XPathParser parser = new XPathParser(script, configuration.getVariables());
      // 调用上面的 #createSqlSource(...) 方法，创建 SqlSource 对象
      return createSqlSource(configuration, parser.evalNode("/script"), parameterType);
    } else {
      // 直接使用注解上的SQL，理占位符 "${}"，可以看看 issue #127
      script = PropertyParser.parse(script, configuration.getVariables());
      // 创建 TextSqlNode 对象
      TextSqlNode textSqlNode = new TextSqlNode(script);
      // 如果是动态 SQL ，则创建 DynamicSqlSource 对象。
      // note 按理上面已经解析了占位符，现在已经没了，不可能是动态SQL了啊？但是！如果前面找到占位符，但没匹配到值的话，会将占位符原封不动换回来，所以这里依然要匹配
      //  可以理解为 configuration.getVariables() 只处理系统级的通用变量。binding处理方法调用者传过来的参数值
      if (textSqlNode.isDynamic()) {
        return new DynamicSqlSource(configuration, textSqlNode);
      } else {
        return new RawSqlSource(configuration, script, parameterType);
      }
    }
  }

}
