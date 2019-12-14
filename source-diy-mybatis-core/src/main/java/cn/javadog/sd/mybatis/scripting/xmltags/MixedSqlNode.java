package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.List;

/**
 * @author 余勇
 * @date 2019-12-14 18:36
 *
 * 混合的 SqlNode 实现类。也就是说有多个子节点。一般动态SQL都是这种节点。
 *
 * 如下，就有三个子节点。上下一个 {@link StaticTextSqlNode} , 中间一个 {@link ForEachSqlNode}：
 * <select id="getUser" resultType="org.apache.ibatis.submitted.foreach.User">
 * 	 select * from users WHERE id in
 *     <foreach item="item" index="index" collection="friendList" open="(" close=")" separator="," >
 *       #{item.id}
 *     </foreach>
 * </select>
 *
 * note 空格也算节点，只有标签之间有空格，那肯定会有相应的节点
 */
public class MixedSqlNode implements SqlNode {

  /**
   * 内嵌的 SqlNode 数组
   */
  private final List<SqlNode> contents;

  /**
   * 构造函数
   */
  public MixedSqlNode(List<SqlNode> contents) {
    this.contents = contents;
  }

  /**
   * 解析节点的内容，拼接到 context 里面
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 遍历 SqlNode 数组
    for (SqlNode sqlNode : contents) {
      // 解析拼接
      sqlNode.apply(context);
    }
    return true;
  }
}
