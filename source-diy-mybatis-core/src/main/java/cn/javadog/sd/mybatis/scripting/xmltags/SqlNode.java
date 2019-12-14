package cn.javadog.sd.mybatis.scripting.xmltags;

/**
 * @author 余勇
 * @date 2019-12-14 14:22
 *
 * 每个 XML Node 会解析成对应的 SQL Node 对象
 */
public interface SqlNode {

  /**
   * 应用当前 SQL Node 节点。可以理解为，将当前sql节点解析后的内容，拼接到context
   * @param context 上下文
   * @return 当前 SQL Node 节点是否应用成功。
   */
  boolean apply(DynamicContext context);
}
