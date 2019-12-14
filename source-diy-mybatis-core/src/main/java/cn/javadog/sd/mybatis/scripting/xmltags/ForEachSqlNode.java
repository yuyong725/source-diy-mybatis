package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Map;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.parsing.GenericTokenParser;

/**
 * @author 余勇
 * @date 2019-12-14 15:46
 *
 * <foreach /> 标签的 SqlNode 实现类。
 * 使用事例：
 * <foreach collection="ids" item="id" index="index" separator="union" open="(" close=")">
 *    <if test="schema != null">
 *       select * from ${schema}.names where id = #{id}
 *    </if>
 * </foreach>
 */
public class ForEachSqlNode implements SqlNode {

  /**
   * 特定标签前缀，binding里面的foreach标签的部分key会以此开头
   */
  public static final String ITEM_PREFIX = "__frch_";

  /**
   * 表达式计算器
   */
  private final ExpressionEvaluator evaluator;

  /**
   * 集合的表达式，就是 collection 属性值
   */
  private final String collectionExpression;

  /**
   * 字节点
   */
  private final SqlNode contents;

  /**
   * 开标签
   */
  private final String open;

  /**
   * 闭标签
   */
  private final String close;

  /**
   * 分隔符
   */
  private final String separator;

  /**
   * 集合项
   */
  private final String item;

  /**
   * 索引变量的名称
   */
  private final String index;

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * 构造函数
   */
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

  /**
   * 应用标签
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 拿到 binding
    Map<String, Object> bindings = context.getBindings();
    // 获得遍历的集合的 Iterable 对象，用于遍历。
    final Iterable<?> iterable = evaluator.evaluateIterable(collectionExpression, bindings);
    // 如果一个元素都没有，直接返回
    if (!iterable.iterator().hasNext()) {
      return true;
    }
    boolean first = true;
    // 添加 open 到 SQL 中
    applyOpen(context);
    int i = 0;
    // 遍历元素
    for (Object o : iterable) {
      // 记录原始的 context 对象
      DynamicContext oldContext = context;
      // 生成新的 context。note 这是新的 context ，但依然使用的是原来的context做委托对象，也就是添加参数值，会添加到里面
      if (first || separator == null) {
        // 对于第一个标签，或者没有分隔符
        context = new PrefixedContext(context, "");
      } else {
        // 使用分隔符作为前缀
        context = new PrefixedContext(context, separator);
      }
      // 获得唯一编号
      int uniqueNumber = context.getUniqueNumber();
      // 绑定到 context 中，Issue #709
      if (o instanceof Map.Entry) {
        // 如果是map类型
        @SuppressWarnings("unchecked")
        Map.Entry<Object, Object> mapEntry = (Map.Entry<Object, Object>) o;
        // 记录index
        applyIndex(context, mapEntry.getKey(), uniqueNumber);
        // 记录item
        applyItem(context, mapEntry.getValue(), uniqueNumber);
      } else {
        // 记录index
        applyIndex(context, i, uniqueNumber);
        // 记录item
        applyItem(context, o, uniqueNumber);
      }
      // 执行 contents 的应用，也就是子标签。
      // 使用FilteredDynamicContext，替换占位符表达式，并不会替换值。最好自己debug下，就很清楚了
      contents.apply(new FilteredDynamicContext(configuration, context, index, item, uniqueNumber));
      // 判断 prefix 是否已经插入。isPrefixApplied 属性什么时候会有false变成true呢，就在 FilteredDynamicContext 的 appendSql 里，
      // 它在完成自己逻辑后，会执行委托对象，也就是 PrefixedContext 的 appendSql 逻辑，在这里就会进行 isPrefixApplied 属性的更改。这也是装饰器模式
      if (first) {
        // 如果以及插入，就不再是 第一个 元素。
        first = !((PrefixedContext) context).isPrefixApplied();
      }
      // 恢复原始的 context 对象。note 在这之前，context 将 oldContext 装饰了n层的，写在返璞归真，由下一个元素继续装饰
      context = oldContext;
      i++;
    }
    // 添加 close 到 SQL 中
    applyClose(context);
    // 移除 index 和 item 对应的绑定，实际上这俩标签没起过啥作用
    context.getBindings().remove(item);
    context.getBindings().remove(index);
    return true;
  }

  /**
   * 将下标信息记录到 binding里面
   *
   * @param context sql上下文，使用里面的binding记录所有参数值的映射
   * @param o 集合正在遍历的位置下标。如果是map的话，就是entry的key
   * @param i context的流水号
   */
  private void applyIndex(DynamicContext context, Object o, int i) {
    // note index可能为空，当我们不需要下标时，一般不会去设置
    if (index != null) {
      // 添加 index变量名 => index值/index的key
      context.bind(index, o);
      // 添加 (ITEM_PREFIX + index变量名 + "_" + 流水号) => index值/index的key
      context.bind(itemizeItem(index, i), o);
    }
  }

  /**
   * 将参数值记录到 binding里面。
   * note 结合👆的 {@link #applyIndex(DynamicContext, Object, int)} ,两个方法允许后，会在binding中新增4个键值对，分别是：
   *  1、 index变量名 => index值/index的key
   *  2、 (ITEM_PREFIX + index变量名 + "_" + 流水号) => index值/index的key
   *  3、 item变量名 => item值/item的value
   *  4、 (ITEM_PREFIX + item变量名 + "_" + 流水号) => item值/
   *
   *  从后面的逻辑看，1、3 没用到，但2、4很重要。相当于将list的每个值拆分后添加到binding
   *
   * @param context sql上下文，使用里面的binding记录所有参数值的映射
   * @param o 集合正在遍历的位置值，从0开始。如果是map的话，就是entry的key
   * @param i context的流水号
   */
  private void applyItem(DynamicContext context, Object o, int i) {
    // item一般不会为空。note 这个item指的是标签中，指向真实item值的变量名，除非遍历不需要使用真实item的值，否则肯定要设置该遍历名
    if (item != null) {
      // 添加 item变量名 => item值/item的value
      context.bind(item, o);
      // 添加 (ITEM_PREFIX + item变量名 + "_" + 流水号) => item值/item的value
      context.bind(itemizeItem(item, i), o);
    }
  }

  /**
   * 将 open 属性值 添加到 sql
   */
  private void applyOpen(DynamicContext context) {
    if (open != null) {
      context.appendSql(open);
    }
  }

  /**
   * 将 close 属性值 添加到 sql
   */
  private void applyClose(DynamicContext context) {
    if (close != null) {
      context.appendSql(close);
    }
  }

  /**
   * 逐条标记item，给它一个唯一的标记
   */
  private static String itemizeItem(String item, int i) {
    return ITEM_PREFIX + item + "_" + i;
  }

  /**
   * 实现子节点访问 <foreach /> 标签中的变量的替换的 DynamicContext 实现类
   */
  private static class FilteredDynamicContext extends DynamicContext {

    /**
     * 委托的 SQL 上下文
     */
    private final DynamicContext delegate;

    /**
     * 唯一标识 {@link DynamicContext#getUniqueNumber()}
     */
    private final int index;

    /**
     * 索引变量名 {@link ForEachSqlNode#index}
     */
    private final String itemIndex;

    /**
     * 集合项 {@link ForEachSqlNode#item}
     */
    private final String item;

    /**
     * 构造函数
     */
    public FilteredDynamicContext(Configuration configuration,DynamicContext delegate, String itemIndex, String item, int i) {
      super(configuration, null);
      this.delegate = delegate;
      this.index = i;
      this.itemIndex = itemIndex;
      this.item = item;
    }

    /**
     * 获取 Bindings
     */
    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    /**
     * 添加键值对到Bindings
     */
    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    /**
     * 获取拼接后的sql
     */
    @Override
    public String getSql() {
      return delegate.getSql();
    }

    /**
     * 拼接后SQL。这里并没有使用 binding 里的参数值替换占位符号，只是加了流水号。
     * note 为什么要这样做呢？举个例子：<foreach item="item">#{item.id}</foreach>，如果不对占位符做变更，按照先遍历再替换的逻辑，
     *  遍历后就成了 "#{item.id},#{item.id},#{item.id},#{item.id}",而如果做了如下的变更，就变成了：
     *  "#{item.id_1},#{item.id_2},#{item.id_3},#{item.id_4}"。这样就很清晰了，拼接SQL时自然去替换对应的分隔符。
     */
    @Override
    public void appendSql(String sql) {
      // TODO 第三个参数就是 FunctionalInterface 的写法
      GenericTokenParser parser = new GenericTokenParser("#{", "}", content -> {

        // 将对 item 的访问，替换成 itemizeItem(item, index) 。也就是 (ITEM_PREFIX + item变量名 + "_" + 流水号)
        String newContent = content.replaceFirst("^\\s*" + item + "(?![^.,:\\s])", itemizeItem(item, index));
        // 将对 itemIndex 的访问，替换成 itemizeItem(itemIndex, index) 。也就是 (ITEM_PREFIX + index变量名 + "_" + 流水号)
        if (itemIndex != null && newContent.equals(content)) {
          newContent = content.replaceFirst("^\\s*" + itemIndex + "(?![^.,:\\s])", itemizeItem(itemIndex, index));
        }
        return "#{" + newContent + "}";
      });

      // 拼接SQL
      delegate.appendSql(parser.parse(sql));
    }

    /**
     * 获取标示，逻辑上说不会去调用此方法
     */
    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

  }

  /**
   * 内部类，支持添加 <foreach /> 标签中，多个元素之间的分隔符的 DynamicContext 实现类
   */
  private class PrefixedContext extends DynamicContext {

    /**
     * 动态sql上下文，在这里是委托对象，主要功能委托其完成
     */
    private final DynamicContext delegate;

    /**
     * 前缀
     */
    private final String prefix;

    /**
     * 是否添加了前缀
     */
    private boolean prefixApplied;

    /**
     * 构造函数
     */
    public PrefixedContext(DynamicContext delegate, String prefix) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefix = prefix;

      /**
       * 是否已经应用 prefix，默认为false
       */
      this.prefixApplied = false;
    }

    /**
     * 获取 prefixApplied
     */
    public boolean isPrefixApplied() {
      return prefixApplied;
    }

    /**
     * 获取用来绑定的参数值，也就是 binding
     */
    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    /**
     * 添加参数值到 binding
     */
    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    /**
     * 拼接 sql
     */
    @Override
    public void appendSql(String sql) {
      // 如果未应用 prefix ，并且，方法参数 sql 非空
      // 则添加 prefix 到 delegate 中，并标记 prefixApplied 为 true ，表示已经应用
      if (!prefixApplied && sql != null && sql.trim().length() > 0) {
        // note 即使 prefix 是空字符串或者就是空，也算插入前缀了
        delegate.appendSql(prefix);
        prefixApplied = true;
      }
      // 添加 sql 到 delegate 中
      delegate.appendSql(sql);
    }

    /**
     * 获取 sql
     */
    @Override
    public String getSql() {
      return delegate.getSql();
    }

    /**
     * 获取 唯一编号
     */
    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }
  }

}
