package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.List;

/**
 * @author Clinton Begin
 *
 * <choose /> 标签的 SqlNode 实现类
 */
public class ChooseSqlNode implements SqlNode {
  /**
   * <otherwise /> 标签对应的 SqlNode 节点
   */
  private final SqlNode defaultSqlNode;
  /**
   * <when /> 标签对应的 SqlNode 节点数组
   */
  private final List<SqlNode> ifSqlNodes;

  public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
    this.ifSqlNodes = ifSqlNodes;
    this.defaultSqlNode = defaultSqlNode;
  }

  @Override
  public boolean apply(DynamicContext context) {
    // <1> 先判断  <when /> 标签中，是否有符合条件的节点。
    // 如果有，则进行应用。并且只因应用一个 SqlNode 对象
    for (SqlNode sqlNode : ifSqlNodes) {
      if (sqlNode.apply(context)) {
        return true;
      }
    }
    // <2> 再判断  <otherwise /> 标签，是否存在
    // 如果存在，则进行应用
    if (defaultSqlNode != null) {
      defaultSqlNode.apply(context);
      return true;
    }
    // <3> 返回都失败
    return false;
  }
}
