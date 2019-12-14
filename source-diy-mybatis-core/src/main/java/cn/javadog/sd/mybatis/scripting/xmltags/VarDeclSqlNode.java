package cn.javadog.sd.mybatis.scripting.xmltags;

/**
 * @author 余勇
 * @date 2019-12-14 21:33
 * 实现 SqlNode 接口，<bind /> 标签的 SqlNode 实现类。
 *
 * 用法如下：<bind name="condition" value="p" />，value支持ognl表达式
 */
public class VarDeclSqlNode implements SqlNode {

  /**
   * 名字属性
   */
  private final String name;

  /**
   * 表达式，也就是value属性
   */
  private final String expression;

  /**
   * 构造函数
   */
  public VarDeclSqlNode(String var, String exp) {
    name = var;
    expression = exp;
  }

  @Override
  public boolean apply(DynamicContext context) {
    // 解析value表达式对应的值
    final Object value = OgnlCache.getValue(expression, context.getBindings());
    // 绑定到上下文
    context.bind(name, value);
    return true;
  }

}
