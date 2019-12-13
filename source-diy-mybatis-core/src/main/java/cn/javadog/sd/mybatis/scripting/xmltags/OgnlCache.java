package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import ognl.Ognl;
import ognl.OgnlException;

/**
 * Caches OGNL parsed expressions.
 *
 * @author Eduardo Macarron
 *
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

  private OgnlCache() {
    // Prevent Instantiation of Static Class
  }

  public static Object getValue(String expression, Object root) {
    try {
      // <1> 创建 OGNL Context 对象
      Map context = Ognl.createDefaultContext(root, MEMBER_ACCESS, CLASS_RESOLVER, null);
      // <2> 解析表达式
      // <3> 获得表达式对应的值
      return Ognl.getValue(parseExpression(expression), context, root);
    } catch (OgnlException e) {
      throw new BuilderException("Error evaluating expression '" + expression + "'. Cause: " + e, e);
    }
  }

  private static Object parseExpression(String expression) throws OgnlException {
    Object node = expressionCache.get(expression);
    if (node == null) {
      node = Ognl.parseExpression(expression);
      expressionCache.put(expression, node);
    }
    return node;
  }

}
