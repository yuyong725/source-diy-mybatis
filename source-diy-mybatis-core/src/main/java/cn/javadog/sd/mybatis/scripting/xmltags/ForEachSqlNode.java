package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Map;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.parsing.GenericTokenParser;

/**
 * @author Clinton Begin
 *
 * <foreach /> 标签的 SqlNode 实现类
 */
public class ForEachSqlNode implements SqlNode {
  public static final String ITEM_PREFIX = "__frch_";

  private final ExpressionEvaluator evaluator;

  /**
   * 集合的表达式
   */
  private final String collectionExpression;
  private final SqlNode contents;
  private final String open;
  private final String close;
  private final String separator;

  /**
   * 集合项
   */
  private final String item;

  /**
   * 索引变量
   */
  private final String index;
  private final Configuration configuration;

  public ForEachSqlNode(Configuration configuration, SqlNode contents, String collectionExpression, String index, String item, String open, String close, String separator) {
    this.evaluator = new ExpressionEvaluator();
    this.collectionExpression = collectionExpression;
    this.contents = contents;
    this.open = open;
    this.close = close;
    this.separator = separator;
    this.index = index;
    this.item = item;
    this.configuration = configuration;
  }

  @Override
  public boolean apply(DynamicContext context) {
    Map<String, Object> bindings = context.getBindings();
    // <1> 获得遍历的集合的 Iterable 对象，用于遍历。
    final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings);
    if (!iterable.iterator().hasNext()) {
      return true;
    }
    // <2> 添加 open 到 SQL 中
    boolean first = true;
    applyOpen(context);
    int i = 0;
    for (Object o : iterable) {
      // <3> 记录原始的 context 对象
      DynamicContext oldContext = context;
      // <4> 生成新的 context
      if (first || separator == null) {
        context = new PrefixedContext(context, "");
      } else {
        context = new PrefixedContext(context, separator);
      }
      // <5> 获得唯一编号
      int uniqueNumber = context.getUniqueNumber();
      // Issue #709
      // <6> 绑定到 context 中
      if (o instanceof Map.Entry) {
        @SuppressWarnings("unchecked") 
        Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
        applyIndex(context, mapEntry.getKey(), uniqueNumber);
        applyItem(context, mapEntry.getValue(), uniqueNumber);
      } else {
        applyIndex(context, i, uniqueNumber);
        applyItem(context, o, uniqueNumber);
      }
      // <7> 执行 contents 的应用
      contents.apply(new FilteredDynamicContext(configuration, context, index, item, uniqueNumber));
      // <8> 判断 prefix 是否已经插入
      if (first) {
        first = !((PrefixedContext) context).isPrefixApplied();
      }
      // <9> 恢复原始的 context 对象
      context = oldContext;
      i++;
    }
    // <10> 添加 close 到 SQL 中
    applyClose(context);
    // <11> 移除 index 和 item 对应的绑定
    context.getBindings().remove(item);
    context.getBindings().remove(index);
    return true;
  }

  private void applyIndex(DynamicContext context, Object o, int i) {
    if (index != null) {
      context.bind(index, o);
      context.bind(itemizeItem(index, i), o);
    }
  }

  private void applyItem(DynamicContext context, Object o, int i) {
    if (item != null) {
      context.bind(item, o);
      context.bind(itemizeItem(item, i), o);
    }
  }

  private void applyOpen(DynamicContext context) {
    if (open != null) {
      context.appendSql(open);
    }
  }

  private void applyClose(DynamicContext context) {
    if (close != null) {
      context.appendSql(close);
    }
  }

  private static String itemizeItem(String item, int i) {
    return ITEM_PREFIX + item + "_" + i;
  }

  /**
   * 实现子节点访问 <foreach /> 标签中的变量的替换的 DynamicContext 实现类
   */
  private static class FilteredDynamicContext extends DynamicContext {
    private final DynamicContext delegate;
    /**
     * 唯一标识 {@link DynamicContext#getUniqueNumber()}
     */
    private final int index;
    /**
     * 索引变量 {@link ForEachSqlNode#index}
     */
    private final String itemIndex;
    /**
     * 集合项 {@link ForEachSqlNode#item}
     */
    private final String item;

    public FilteredDynamicContext(Configuration configuration,DynamicContext delegate, String itemIndex, String item, int i) {
      super(configuration, null);
      this.delegate = delegate;
      this.index = i;
      this.itemIndex = itemIndex;
      this.item = item;
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    @Override
    public void appendSql(String sql) {
      GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> {
        // 将对 item 的访问，替换成 itemizeItem(item, index) 。
        String newContent = content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", itemizeItem(item, index));
        // 将对 itemIndex 的访问，替换成 itemizeItem(itemIndex, index) 。
        if (itemIndex != null && newContent.equals(content)) {
          newContent = content.replaceFirst("^\\s*" + itemIndex + "(?![^.,:\\s])", itemizeItem(itemIndex, index));
        }
        return "#{" + newContent + "}";
      });

      // 执行 GenericTokenParser 的解析
      // 添加到 delegate 中
      delegate.appendSql(parser.parse(sql));
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

  }

  /**
   * 支持添加 <foreach /> 标签中，多个元素之间的分隔符的 DynamicContext 实现类
   */
  private class PrefixedContext extends DynamicContext {
    private final DynamicContext delegate;
    private final String prefix;
    private boolean prefixApplied;

    public PrefixedContext(DynamicContext delegate, String prefix) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefix = prefix;

      /**
       * 是否已经应用 prefix
       */
      this.prefixApplied = false;
    }

    public boolean isPrefixApplied() {
      return prefixApplied;
    }

    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    @Override
    public void appendSql(String sql) {
      // 如果未应用 prefix ，并且，方法参数 sql 非空
      // 则添加 prefix 到 delegate 中，并标记 prefixApplied 为 true ，表示已经应用
      if (!prefixApplied && sql != null && sql.trim().length() > 0) {
        delegate.appendSql(prefix);
        prefixApplied = true;
      }
      // 添加 sql 到 delegate 中
      delegate.appendSql(sql);
    }

    @Override
    public String getSql() {
      return delegate.getSql();
    }

    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }
  }

}