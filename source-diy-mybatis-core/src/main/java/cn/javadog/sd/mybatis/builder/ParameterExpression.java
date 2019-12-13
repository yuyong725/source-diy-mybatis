package cn.javadog.sd.mybatis.builder;

import java.util.HashMap;

import cn.javadog.sd.mybatis.support.exceptions.BuilderException;

/**
 * @author: 余勇
 * @date: 2019-12-12 13:22
 *
 * 内联参数表达式解析器，支持一些简单的语法
 * 内联的含义可以看看：https://baike.baidu.com/item/%E5%86%85%E8%81%94%E5%87%BD%E6%95%B0，当然，这只是我的猜测
 * 举了一些我看不太懂的例子，TODO 遇到实际使用场景时再看，英文都没有注释
 * note 暂时丢掉这部分的翻译，因为ParameterMap基本也废弃了，很少使用，官方文档对用法说明都删除了
 *
 * <pre>
 * inline-parameter = (propertyName | expression) oldJdbcType attributes
 * propertyName = /expression language's property navigation path/
 * expression = '(' /expression language's expression/ ')'
 * oldJdbcType = ':' /any valid jdbc type/
 * attributes = (',' attribute)*
 * attribute = name '=' value
 * </pre>
 */
public class ParameterExpression extends HashMap<String, String> {

  private static final long serialVersionUID = -2417552199605158680L;

  /**
   * 构造函数
   */
  public ParameterExpression(String expression) {
  	// 立马解析
    parse(expression);
  }

  /**
   * 解析表达式
   */
  private void parse(String expression) {
  	// 跳过空格
    int p = skipWS(expression, 0);
    // 剩下的部分以'('开头的话，就是表达式，否则就是属性
    if (expression.charAt(p) == '(') {
	  // 解析表达式
      expression(expression, p + 1);
    } else {
      // 解析属性
      property(expression, p);
    }
  }

  /**
   * 解析表达式
   * note 从方法的逻辑可以看出，必须有对应的')'，且与'('至少要相隔一个字符，不然这里是会报错的
   *
   * @param expression 要解析的表达式
   * @param left expression去掉空格和最左边的'('后的第一个字符的下标
   */
  private void expression(String expression, int left) {
  	// 匹配的数量，遇到'('就+1，遇到')'就-1，因为进入这个函数代表遇到了'('，所以初始值是1，当找到与之匹配的')'时，match的值应为0
    int match = 1;
    int right = left + 1;
    while (match > 0) {
      if (expression.charAt(right) == ')') {
        match--;
      } else if (expression.charAt(right) == '(') {
        match++;
      }
      // 当找到与第一个'('对应的')'后，right依然会+1，此时的值为对应的')'的后面一个字符的下标
      right++;
    }
    // 将 '(***)'中的值赋值给 expression
    put("expression", expression.substring(left, right - 1));
    jdbcTypeOpt(expression, right);
  }

  /**
   * 解析property
   */
  private void property(String expression, int left) {
    if (left < expression.length()) {
      int right = skipUntil(expression, left, ",:");
      put("property", trimmedStr(expression, left, right));
      jdbcTypeOpt(expression, right);
    }
  }

  /**
   * 返回第一个不是空格的字符的角标，WS是whitespace的缩写
   * 参考ASCII码表：https://blog.csdn.net/xufox/article/details/8077914
   */
  private int skipWS(String expression, int p) {
    for (int i = p; i < expression.length(); i++) {
	  // 0x20 是16进制的32，对应的ASCII值是空格' '，只有大于32的，才是Java会用到的字符
      if (expression.charAt(i) > 0x20) {
        return i;
      }
    }
    return expression.length();
  }

  /**
   * 跳过从下标p开始，跳过一段距离，直到某个字符恰好截止字符串 endChars 也有，返回那个字符的下标
   */
  private int skipUntil(String expression, int p, final String endChars) {
    for (int i = p; i < expression.length(); i++) {
      char c = expression.charAt(i);
      if (endChars.indexOf(c) > -1) {
        return i;
      }
    }
    return expression.length();
  }

  private void jdbcTypeOpt(String expression, int p) {
    p = skipWS(expression, p);
    if (p < expression.length()) {
      if (expression.charAt(p) == ':') {
        jdbcType(expression, p + 1);
      } else if (expression.charAt(p) == ',') {
        option(expression, p + 1);
      } else {
        throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
      }
    }
  }

  private void jdbcType(String expression, int p) {
    int left = skipWS(expression, p);
    int right = skipUntil(expression, left, ",");
    if (right > left) {
      put("jdbcType", trimmedStr(expression, left, right));
    } else {
      throw new BuilderException("Parsing error in {" + expression + "} in position " + p);
    }
    option(expression, right + 1);
  }

  private void option(String expression, int p) {
    int left = skipWS(expression, p);
    if (left < expression.length()) {
      int right = skipUntil(expression, left, "=");
      String name = trimmedStr(expression, left, right);
      left = right + 1;
      right = skipUntil(expression, left, ",");
      String value = trimmedStr(expression, left, right);
      put(name, value);
      option(expression, right + 1);
    }
  }

  private String trimmedStr(String str, int start, int end) {
    while (str.charAt(start) <= 0x20) {
      start++;
    }
    while (str.charAt(end - 1) <= 0x20) {
      end--;
    }
    return start >= end ? "" : str.substring(start, end);
  }

}
