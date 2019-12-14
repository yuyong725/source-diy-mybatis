package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author 余勇
 * @date 2019-12-14 18:54
 *
 * <trim /> 标签的 SqlNode 实现类。
 * 使用示例如下：
 * <trim prefix="WHERE" prefixOverrides="AND |OR " suffix=";">
 * 		AND <foreach collection="list" item="enum" open="name IN (" close=") " separator=", ">#{enum}</foreach>
 * 		AND id = #{id}
 * </trim>
 */
public class TrimSqlNode implements SqlNode {

  /**
   * 内含的 SqlNode 节点
   */
  private final SqlNode contents;

  /**
   * 前缀
   */
  private final String prefix;

  /**
   * 后缀
   */
  private final String suffix;

  /**
   * 需要被删除的前缀
   */
  private final List<String> prefixesToOverride;

  /**
   * 需要被删除的后缀
   */
  private final List<String> suffixesToOverride;

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * 构造函数
   */
  public TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, String prefixesToOverride, String suffix, String suffixesToOverride) {
    // 将 xml 中的 prefixOverrides 和 suffixesToOverride 属性以'｜'切割成数组
    this(configuration, contents, prefix, parseOverrides(prefixesToOverride), suffix, parseOverrides(suffixesToOverride));
  }

  /**
   * 构造函数
   */
  protected TrimSqlNode(Configuration configuration, SqlNode contents, String prefix, List<String> prefixesToOverride, String suffix, List<String> suffixesToOverride) {
    this.contents = contents;
    this.prefix = prefix;
    this.prefixesToOverride = prefixesToOverride;
    this.suffix = suffix;
    this.suffixesToOverride = suffixesToOverride;
    this.configuration = configuration;
  }

  /**
   * 解析SQL，拼接到 context
   */
  @Override
  public boolean apply(DynamicContext context) {
    // 创建 FilteredDynamicContext 对象
    FilteredDynamicContext filteredDynamicContext = new FilteredDynamicContext(context);
    // 执行 contents 的应用
    boolean result = contents.apply(filteredDynamicContext);
    // 执行 FilteredDynamicContext 的应用，trim的逻辑在这里
    filteredDynamicContext.applyAll();
    return result;
  }

  /**
   * 使用 | 分隔字符串成字符串数组，并都转换成大写
   */
  private static List<String> parseOverrides(String overrides) {
    if (overrides != null) {
      final StringTokenizer parser = new StringTokenizer(overrides, "|", false);
      final List<String> list = new ArrayList<>(parser.countTokens());
      while (parser.hasMoreTokens()) {
        list.add(parser.nextToken().toUpperCase(Locale.ENGLISH));
      }
      return list;
    }
    return Collections.emptyList();
  }

  /**
   * 支持 trim 逻辑的 DynamicContext 实现类
   */
  private class FilteredDynamicContext extends DynamicContext {

    /**
     * 委托的 DynamicContext 对象
     */
    private DynamicContext delegate;

    /**
     * 是否 prefix 已经被应用
     */
    private boolean prefixApplied;

    /**
     * 是否 suffix 已经被应用
     */
    private boolean suffixApplied;

    /**
     * StringBuilder 对象
     *
     * @see #appendSql(String)
     */
    private StringBuilder sqlBuffer;

    /**
     * 构造函数
     */
    public FilteredDynamicContext(DynamicContext delegate) {
      super(configuration, null);
      this.delegate = delegate;
      this.prefixApplied = false;
      this.suffixApplied = false;
      // 一开始是个空字符串
      this.sqlBuffer = new StringBuilder();
    }

    /**
     * 应用trim的逻辑
     */
    public void applyAll() {
      // trim 掉多余的空格，生成新的 sqlBuffer 对象
      sqlBuffer = new StringBuilder(sqlBuffer.toString().trim());
      // 将 sqlBuffer 大写，生成新的 trimmedUppercaseSql 对象
      String trimmedUppercaseSql = sqlBuffer.toString().toUpperCase(Locale.ENGLISH);
      // 应用 TrimSqlNode 的 trim 逻辑
      if (trimmedUppercaseSql.length() > 0) {
        applyPrefix(sqlBuffer, trimmedUppercaseSql);
        applySuffix(sqlBuffer, trimmedUppercaseSql);
      }
      // 将结果，添加到 delegate 中。此时才真正意义上拼接
      delegate.appendSql(sqlBuffer.toString());
    }

    /**
     * 获取 Bindings
     */
    @Override
    public Map<String, Object> getBindings() {
      return delegate.getBindings();
    }

    /**
     * 添加键值对到 Bindings
     */
    @Override
    public void bind(String name, Object value) {
      delegate.bind(name, value);
    }

    /**
     * 获取唯一标示
     */
    @Override
    public int getUniqueNumber() {
      return delegate.getUniqueNumber();
    }

    /**
     * 将SQL暂时拼接到 sqlBuffer
     */
    @Override
    public void appendSql(String sql) {
      sqlBuffer.append(sql);
    }

    /**
     * 获取拼接好的SQL
     */
    @Override
    public String getSql() {
      return delegate.getSql();
    }

    /**
     * 拼接前缀到sql上
     *
     * @param sql 原SQL
     * @param trimmedUppercaseSql 原SQL大写后的SQL
     */
    private void applyPrefix(StringBuilder sql, String trimmedUppercaseSql) {
      // 没拼接才去拼接，相当于只能调用一次
      if (!prefixApplied) {
        // 标记成已拼接
        prefixApplied = true;
        // prefixesToOverride 非空，先删除；也就是如果前缀含有指定标签，就删掉
        if (prefixesToOverride != null) {
          for (String toRemove : prefixesToOverride) {
            // 大小的目的在这
            if (trimmedUppercaseSql.startsWith(toRemove)) {
              sql.delete(0, toRemove.trim().length());
              break;
            }
          }
        }
        // prefix 非空，再添加
        if (prefix != null) {
          // 保险起见，前面先加个空字符串
          sql.insert(0, " ");
          // 拼接前缀
          sql.insert(0, prefix);
        }
      }
    }

    /**
     * 拼接后缀
     */
    private void applySuffix(StringBuilder sql, String trimmedUppercaseSql) {
      if (!suffixApplied) {
        // 标记成已拼接
        suffixApplied = true;
        // suffixesToOverride 非空，先删除
        if (suffixesToOverride != null) {
          for (String toRemove : suffixesToOverride) {
            // TODO 总感觉这里不严谨，不深究吧
            if (trimmedUppercaseSql.endsWith(toRemove) || trimmedUppercaseSql.endsWith(toRemove.trim())) {
              int start = sql.length() - toRemove.trim().length();
              int end = sql.length();
              sql.delete(start, end);
              break;
            }
          }
        }
        // suffix 非空，再添加
        if (suffix != null) {
          sql.append(" ");
          sql.append(suffix);
        }
      }
    }

  }

}
