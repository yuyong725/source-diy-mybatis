package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author Clinton Begin
 * <where /> 标签的 SqlNode 实现类
 */
public class WhereSqlNode extends TrimSqlNode {

  /**
   * 要去掉的前缀
   */
  private static List<String> prefixList = Arrays.asList("AND ","OR ","AND\n", "OR\n", "AND\r", "OR\r", "AND\t", "OR\t");

  /**
   * 构造函数
   */
  public WhereSqlNode(Configuration configuration, SqlNode contents) {
    // 核心逻辑由 TrimSqlNode 完成
    super(configuration, contents, "WHERE", prefixList, null, null);
  }

}
