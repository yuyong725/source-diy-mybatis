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
 * 实现 LanguageDriver 接口，XML 语言驱动实现类
 *
 * @author Eduardo Macarron
 */
public class XMLLanguageDriver implements LanguageDriver {

  @Override
  public ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
    // 创建 DefaultParameterHandler 对象
    return new DefaultParameterHandler(mappedStatement, parameterObject, boundSql);
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
    // 创建 XMLScriptBuilder 对象，执行解析
    XMLScriptBuilder builder = new XMLScriptBuilder(configuration, script, parameterType);
    return builder.parseScriptNode();
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
    // issue #3
    // <1> 如果是 <script> 开头，使用 XML 配置的方式，使用动态 SQL
    if (script.startsWith("<script>")) {
      // <1.1> 创建 XPathParser 对象，解析出 <script /> 节点
      XPathParser parser = new XPathParser(script, configuration.getVariables());
      // <1.2> 调用上面的 #createSqlSource(...) 方法，创建 SqlSource 对象
      return createSqlSource(configuration, parser.evalNode("/script"), parameterType);
    } else {
      // issue #127
      // <2.1> 变量替换
      script = PropertyParser.parse(script, configuration.getVariables());
      // <2.2> 创建 TextSqlNode 对象
      TextSqlNode textSqlNode = new TextSqlNode(script);
      // <2.3.1> 如果是动态 SQL ，则创建 DynamicSqlSource 对象
      if (textSqlNode.isDynamic()) {
        return new DynamicSqlSource(configuration, textSqlNode);
      } else {
        return new RawSqlSource(configuration, script, parameterType);
      }
    }
  }

}
