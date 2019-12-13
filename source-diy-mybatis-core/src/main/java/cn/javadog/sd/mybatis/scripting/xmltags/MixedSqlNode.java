package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.List;

/**
 * @author Clinton Begin
 * 混合的 SqlNode 实现类
 */
public class MixedSqlNode implements SqlNode {
  /**
   * 内嵌的 SqlNode 数组
   */
  private final List<SqlNode> contents;

  public MixedSqlNode(List<SqlNode> contents) {
    this.contents = contents;
  }

  @Override
  public boolean apply(DynamicContext context) {
    // 遍历 SqlNode 数组，逐个应用
    for (SqlNode sqlNode : contents) {
      sqlNode.apply(context);
    }
    return true;
  }
}
