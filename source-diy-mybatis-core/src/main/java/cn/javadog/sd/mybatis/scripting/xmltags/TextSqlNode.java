package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.regex.Pattern;

import cn.javadog.sd.mybatis.support.exceptions.ScriptingException;
import cn.javadog.sd.mybatis.support.parsing.GenericTokenParser;
import cn.javadog.sd.mybatis.support.parsing.TokenHandler;
import cn.javadog.sd.mybatis.support.type.SimpleTypeRegistry;

/**
 * @author Clinton Begin
 *
 * 文本的 SqlNode 实现类。
 * 相比 StaticTextSqlNode 的实现来说，TextSqlNode 不确定是否为静态文本，所以提供 #isDynamic() 方法，进行判断是否为动态文本
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

  public TextSqlNode(String text) {
    this(text, null);
  }
  
  public TextSqlNode(String text, Pattern injectionFilter) {
    this.text = text;
    this.injectionFilter = injectionFilter;
  }

  /**
   * 判断是否为动态文本
   */
  public boolean isDynamic() {
    // <1> 创建 DynamicCheckerTokenParser 对象
    DynamicCheckerTokenParser checker = new DynamicCheckerTokenParser();
    // <2> 创建 GenericTokenParser 对象
    GenericTokenParser parser = createParser(checker);
    // <3> 执行解析
    parser.parse(text);
    // <4> 判断是否为动态文本
    return checker.isDynamic();
  }

  @Override
  public boolean apply(DynamicContext context) {
    // <1> 创建 BindingTokenParser 对象
    // <2> 创建 GenericTokenParser 对象
    GenericTokenParser parser = createParser(new BindingTokenParser(context, injectionFilter));
    // <3> 执行解析
    // <4> 将解析的结果，添加到 context 中
    context.appendSql(parser.parse(text));
    return true;
  }
  
  private GenericTokenParser createParser(TokenHandler handler) {
    return new GenericTokenParser("${", "}", handler);
  }

  private static class BindingTokenParser implements TokenHandler {

    private DynamicContext context;
    private Pattern injectionFilter;

    public BindingTokenParser(DynamicContext context, Pattern injectionFilter) {
      this.context = context;
      this.injectionFilter = injectionFilter;
    }

    @Override
    public String handleToken(String content) {
      // 初始化 value 属性到 context 中
      Object parameter = context.getBindings().get("_parameter");
      if (parameter == null) {
        context.getBindings().put("value", null);
      } else if (SimpleTypeRegistry.isSimpleType(parameter.getClass())) {
        context.getBindings().put("value", parameter);
      }
      // 使用 OGNL 表达式，获得对应的值
      Object value = OgnlCache.getValue(content, context.getBindings());
      String srtValue = (value == null ? "" : String.valueOf(value)); // issue #274 return "" instead of "null"
      checkInjection(srtValue);
      // 返回该值
      return srtValue;
    }



    private void checkInjection(String value) {
      if (injectionFilter != null && !injectionFilter.matcher(value).matches()) {
        throw new ScriptingException("Invalid input. Please conform to regex" + injectionFilter.pattern());
      }
    }
  }
  
  private static class DynamicCheckerTokenParser implements TokenHandler {

    /**
     * 是否为动态文本
     */
    private boolean isDynamic;

    public DynamicCheckerTokenParser() {
      // Prevent Synthetic Access
    }

    public boolean isDynamic() {
      return isDynamic;
    }

    @Override
    public String handleToken(String content) {
      // 当检测到 token ，标记为动态文本
      this.isDynamic = true;
      return null;
    }
  }
  
}