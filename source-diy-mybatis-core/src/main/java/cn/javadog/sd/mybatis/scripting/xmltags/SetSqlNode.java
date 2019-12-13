package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author Clinton Begin
 *
 * <set /> 标签的 SqlNode 实现类
 */
public class SetSqlNode extends TrimSqlNode {

  private static List<String> suffixList = Arrays.asList(",");

  public SetSqlNode(Configuration configuration,SqlNode contents) {
    super(configuration, contents, "SET", null, null, suffixList);
  }

}
