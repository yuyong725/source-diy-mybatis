package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import ognl.Ognl;
import ognl.OgnlException;

/**
 * @author 余勇
 * @date 2019-12-14 22:33
 *
 * 缓存 OGNL 解析过的表达式。
 * 这玩意我也不懂，不深究，感兴趣可以看看：
 * @see <a href='http://code.google.com/p/mybatis/issues/detail?id=342'>Issue 342</a>
 */
public final class OgnlCache {

  /**
   * OgnlMemberAccess 单例
   */
  private static final OgnlMemberAccess MEMBER_ACCESS = new OgnlMemberAccess();

  /**
   * OgnlClassResolver 单例
   */
  private static final OgnlClassResolver CLASS_RESOLVER = new OgnlClassResolver();

  /**
   * 表达式的缓存的映射
   *
   * KEY：表达式
   * VALUE：表达式的缓存 @see #parseExpression(String)
   */
  private static final Map<String, Object> expressionCache = new ConcurrentHashMap<>();

  /**
   * 构造函数，不对外暴露
   */
  private OgnlCache() {
  }

  /**
   * 获取指定表达式的值
   */
  public static Object getValue(String expression, Object root) {
    try {
      // 创建 OGNL Context 对象
      Map context = Ognl.createDefaultContext(root, MEMBER_ACCESS, CLASS_RESOLVER, null);
      // 解析表达式，并获取表达式对应的值
      return Ognl.getValue(parseExpression(expression), context, root);
    } catch (OgnlException e) {
      throw new BuilderException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
    }
  }

  /**
   * 解析表达式
   */
  private static Object parseExpression(String expression) throws OgnlException {
    // 从缓存里拿
    Object node = expressionCache.get(expression);
    // 拿不到就去解析，然后放进缓存
    if (node == null) {
      node = Ognl.parseExpression(expression);
      expressionCache.put(expression, node);
    }
    return node;
  }

}
