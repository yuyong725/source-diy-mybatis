package cn.javadog.sd.mybatis.scripting.xmltags;

/**
 * @author 余勇
 * @date 2019-12-14 18:48
 *
 * 静态文本的 SqlNode 实现类。MyBatis拼接会有大量的空字符串的 StaticTextSqlNode
 * note 静态的标准是字符串中没有 '${}'
 */
public class StaticTextSqlNode implements SqlNode {

  /**
   * 文本内容
   */
  private final String text;

  /**
   * 构造函数
   */
  public StaticTextSqlNode(String text) {
    this.text = text;
  }

  /**
   * 将SQL拼接到 context
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 直接拼接到 context 中
    context.appendSql(text);
    return true;
  }

}