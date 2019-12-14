package cn.javadog.sd.mybatis.scripting.xmltags;

/**
 * @author Clinton Begin
 *
 *
 */
/**
 * @author 余勇
 * @date 2019-12-14 15:11
 *
 * <if /> 标签的 SqlNode 实现类。
 * 实际上 <when /> 标签也是这个类.
 *
 * 实例：<if test="password != null">password=#{password},</if>
 */
public class IfSqlNode implements SqlNode {

  /**
   * 表达式计算器
   */
  private final ExpressionEvaluator evaluator;

  /**
   * 判断表达式
   */
  private final String test;

  /**
   * 内嵌的 SqlNode 节点，也就是里面的文本 TextNode
   */
  private final SqlNode contents;

  /**
   * 构造函数
   */
  public IfSqlNode(SqlNode contents, String test) {
    this.test = test;
    this.contents = contents;
    this.evaluator = new ExpressionEvaluator();
  }

  /**
   * 应用标签
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 判断是否符合条件
    if (evaluator.evaluateBoolean(test, context.getBindings())) {
      // 符合，执行 contents 的标签解析
      contents.apply(context);
      // 返回成功
      return true;
    }
    // 不符合，返回失败
    return false;
  }

}
