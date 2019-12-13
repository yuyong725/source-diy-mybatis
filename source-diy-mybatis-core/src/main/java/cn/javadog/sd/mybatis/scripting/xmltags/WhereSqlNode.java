package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author Clinton Begin
 * <where /> 标签的 SqlNode 实现类
 */
public class WhereSqlNode extends TrimSqlNode {

  private static List<String> prefixList = Arrays.asList("AND ","OR ","AND\n", "OR\n", "AND\r", "OR\r", "AND\t", "OR\t");

  public WhereSqlNode(Configuration configuration, SqlNode contents) {
    super(configuration, contents, "WHERE", prefixList, null, null);
  }

}
