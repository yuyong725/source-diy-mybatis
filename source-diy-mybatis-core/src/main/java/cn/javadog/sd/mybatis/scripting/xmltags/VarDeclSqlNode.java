package cn.javadog.sd.mybatis.scripting.xmltags;

/**
 * @author Frank D. Martinez [mnesarco]
 *
 * 实现 SqlNode 接口，<bind /> 标签的 SqlNode 实现类
 */
public class VarDeclSqlNode implements SqlNode {

  /**
   * 名字
   */
  private final String name;

  /**
   * 表达式
   */
  private final String expression;

  public VarDeclSqlNode(String var, String exp) {
    name = var;
    expression = exp;
  }

  @Override
  public boolean apply(DynamicContext context) {
    // <1> 获得值
    final Object value = OgnlCache.getValue(expression, context.getBindings());
    // <2> 绑定到上下文
    context.bind(name, value);
    return true;
  }

}
