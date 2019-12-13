package cn.javadog.sd.mybatis.scripting.xmltags;

/**
 * @author Clinton Begin
 *
 * 静态文本的 SqlNode 实现类
 */
public class StaticTextSqlNode implements SqlNode {
  private final String text;

  public StaticTextSqlNode(String text) {
    this.text = text;
  }

  @Override
  public boolean apply(DynamicContext context) {
    // 直接拼接到 context 中
    context.appendSql(text);
    return true;
  }

}