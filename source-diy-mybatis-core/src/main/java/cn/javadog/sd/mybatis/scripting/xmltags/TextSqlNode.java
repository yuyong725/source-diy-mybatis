package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.regex.Pattern;

import cn.javadog.sd.mybatis.support.exceptions.ScriptingException;
import cn.javadog.sd.mybatis.support.parsing.GenericTokenParser;
import cn.javadog.sd.mybatis.support.parsing.TokenHandler;
import cn.javadog.sd.mybatis.support.type.SimpleTypeRegistry;

/**
 * @author 余勇
 * @date 2019-12-14 15:17
 * 文本的 SqlNode 实现类。有可能含有表达式，如 if 标签的文本
 * 相比 StaticTextSqlNode 的实现来说，TextSqlNode 不确定是否为静态文本，所以提供 #isDynamic() 方法，进行判断是否为动态文本。
 *
 */
public class TextSqlNode implements SqlNode {

  /**
   * 文本
   */
  private final String text;

  /**
   * 目前该属性只在单元测试中使用，暂时无视
   */
  private final Pattern injectionFilter;

  /**
   * 构造函数
   */
  public TextSqlNode(String text) {
    this(text, null);
  }

  /**
   * 构造函数
   */
  public TextSqlNode(String text, Pattern injectionFilter) {
    this.text = text;
    this.injectionFilter = injectionFilter;
  }

  /**
   * 判断是否为动态文本，判断的依据是文本中是否包含 "${}"
   */
  public boolean isDynamic() {
    // 创建 DynamicCheckerTokenParser 对象
    DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
    // 创建 GenericTokenParser 对象
    GenericTokenParser parser = createParser(checker);
    // 执行解析
    parser.parse(text);
    // 判断是否为动态文本
    return checker.isDynamic();
  }

  /**
   * 解析SQL，拼接到 context
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 创建 BindingTokenParser 对象，并创建 GenericTokenParser 对象
    GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
    // 执行解析，将解析的结果，添加到 context 中
    context.appendSql(parser.parse(text));
    return true;
  }

  /**
   * 创建 GenericTokenParser，检测的标签是 "${}"
   */
  private GenericTokenParser createParser(TokenHandler handler) {
    return new GenericTokenParser("${", "}", handler);
  }

  /**
   * 内部类
   */
  private static class BindingTokenParser implements TokenHandler {

    /**
     * 动态 SQL 上下文
     */
    private DynamicContext context;

    /**
     * 目前该属性只在单元测试中使用，暂时无视
     */
    private Pattern injectionFilter;

    /**
     * 构造函数
     */
    public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
      this.context = context;
      this.injectionFilter = injectionFilter;
    }

    /**
     * 替换 '${}' token 的内容
     */
    @Override
    public String handleToken(String content) {
      // 从 context 中拿到所有参数值
      Object parameter = context.getBindings().get("_parameter");
      // TODO 下面这个往binding里添加key为"value"的键值对的目的是什么
      if (parameter == null) {
        // 参数值为空，比如没有参数的情况。将向 Bindings 中添加一个值为 null， key 为 "value" 的键值对
        context.getBindings().put("value", null);
      } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
        // 如果参数值是基础类型，将向 Bindings 中添加一个值为 parameter， key 为 "value" 的键值对
        context.getBindings().put("value", parameter);
      }
      // 使用 OGNL 表达式，从binding中获得对应的值
      Object value = OgnlCache.getValue(content, context.getBindings());
      // 如果没找到对应的值，就用空字符串代替，参见 issue #274
      String srtValue = (value == null ? "" : String.valueOf(value));
      // 检查是否SQL注入
      checkInjection(srtValue);
      // 返回该值
      return srtValue;
    }

    /**
     * 检查是否SQL注入，实际用不到
     */
    private void checkInjection(String value) {
      if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
        throw new ScriptingException("Invalid input. Please conform to regex" + injectionFilter.pattern());
      }
    }
  }

  /**
   * 内部类，动态文本检查器
   */
  private static class DynamicCheckerTokenParser implements TokenHandler {

    /**
     * 是否为动态文本
     */
    private boolean isDynamic;

    /**
     * 构造函数，但类是private修饰的，因此外界也拿不到
     */
    public DynamicCheckerTokenParser() {
      // Prevent Synthetic Access
    }

    /**
     * 获取 isDynamic
     */
    public boolean isDynamic() {
      return isDynamic;
    }

    /**
     * 处理token，
     * 因为检测 动态字符(${},#{}) 的任务是交给 GenericTokenParser，检测到了直接标记是动态就行，没检测到默认就是false
     */
    @Override
    public String handleToken(String content) {
      // 当检测到 token ，标记为动态文本
      this.isDynamic = true;
      return null;
    }
  }
  
}