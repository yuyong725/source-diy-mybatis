package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.List;

/**
 * @author 余勇
 * @date 2019-12-14 14:23
 *
 * <choose /> 标签的 SqlNode 实现类。
 * 使用如下：
 *   <choose>
 *     <when test="title != null">
 *       AND title like #{title}
 *     </when>
 *     <when test="author != null and author.name != null">
 *       AND author_name like #{author.name}
 *     </when>
 *     <otherwise>
 *       AND featured = 1
 *     </otherwise>
 *   </choose>
 */
public class ChooseSqlNode implements SqlNode {

  /**
   * <otherwise /> 标签对应的 SqlNode 节点
   */
  private final SqlNode defaultSqlNode;

  /**
   * <when /> 标签对应的 SqlNode 节点数组。
   * 叫 ifSqlNodes 很有意思，侧面说明作用和 if 标签一样
   */
  private final List<SqlNode> ifSqlNodes;

  /**
   * 构造
   */
  public ChooseSqlNode(List<SqlNode> ifSqlNodes, SqlNode defaultSqlNode) {
    this.ifSqlNodes = ifSqlNodes;
    this.defaultSqlNode = defaultSqlNode;
  }

  /**
   * 应用标签
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 先判断  <when /> 标签中，是否有符合条件的节点。
    // 如果有，则进行应用。并且只因应用一个 SqlNode 对象
    for (SqlNode sqlNode : ifSqlNodes) {
      if (sqlNode.apply(context)) {
        // 因为 choose 标签最终只会有一个条件应用成功，因此只要应用成功，直接返回
        return true;
      }
    }
    // 再判断  <otherwise /> 标签，是否存在
    // 如果存在，则进行应用
    if (defaultSqlNode != null) {
      defaultSqlNode.apply(context);
      return true;
    }
    // 返回都失败
    return false;
  }
}
