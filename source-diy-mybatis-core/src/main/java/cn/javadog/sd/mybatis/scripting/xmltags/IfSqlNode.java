package cn.javadog.sd.mybatis.scripting.xmltags;

/**
 * @author Clinton Begin
 *
 * <if /> 标签的 SqlNode 实现类
 */
public class IfSqlNode implements SqlNode {
  private final ExpressionEvaluator evaluator;

  /**
   * 判断表达式
   */
  private final String test;

  /**
   * 内嵌的 SqlNode 节点
   */
  private final SqlNode contents;

  public IfSqlNode(SqlNode contents, String test) {
    this.test = test;
    this.contents = contents;
    this.evaluator = new ExpressionEvaluator();
  }

  @Override
  public boolean apply(DynamicContext context) {
    // <1> 判断是否符合条件
    if (evaluator.evaluateBoolean(test, context.getBindings())) {
      // <2> 符合，执行 contents 的应用
      contents.apply(context);
      // 返回成功
      return true;
    }
    // <3> 不符合，返回失败
    return false;
  }

}
