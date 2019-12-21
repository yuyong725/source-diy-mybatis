package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.HashMap;
import java.util.Map;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import ognl.OgnlContext;
import ognl.OgnlException;
import ognl.OgnlRuntime;
import ognl.PropertyAccessor;

/**
 * @author 余勇
 * @date 2019-12-14 14:26
 * 动态 SQL 上下文，用于每次执行 SQL 操作时，记录动态 SQL 处理后的最终 SQL 字符串
 */
public class DynamicContext {

  /**
   * {@link #bindings} _parameter 的键，参数
   */
  public static final String PARAMETER_OBJECT_KEY = "_parameter";

  /**
   * {@link #bindings} _databaseId 的键，数据库编号
   */
  public static final String DATABASE_ID_KEY = "_databaseId";

  /**
   * 设置 OGNL 的属性访问器，TODO 没玩过啊
   */
  static {
    OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());
  }

  /**
   * 上下文的参数集合。
   *
   * bind 元素可以从 OGNL 表达式中创建一个变量并将其绑定到上下文。比如：
   * <select id="selectBlogsLike" resultType="Blog">
   *   <bind name="pattern" value="'%' + _parameter.getTitle() + '%'" />
   *   SELECT * FROM BLOG
   *   WHERE title LIKE #{pattern}
   * </select>
   */
  private final ContextMap bindings;

  /**
   * 生成后的 SQL
   */
  private final StringBuilder sqlBuilder = new StringBuilder();

  /**
   * 唯一编号。
   */
  private int uniqueNumber = 0;

  /**
   * 构造函数。
   * 当需要使用到 OGNL 表达式时，parameterObject 非空
   */
  public DynamicContext(Configuration configuration, Object parameterObject) {
    // 初始化 bindings 参数.
    // note 还要求不是map类型，因为是map类型的话，👇的 bindings.put 后会当作map用的
    if (parameterObject != null && !(parameterObject instanceof Map)) {
      MetaObject metaObject = configuration.newMetaObject(parameterObject);
      bindings = new ContextMap(metaObject);
    } else {
      bindings = new ContextMap(null);
    }
    // 添加 bindings 的默认值。note 这里并没有添加 configuration.getVariables()
    bindings.put(PARAMETER_OBJECT_KEY, parameterObject);
  }

  /**
   * 获取 binding
   */
  public Map<String, Object> getBindings() {
    return bindings;
  }

  /**
   * 往 binding 中加值
   */
  public void bind(String name, Object value) {
    bindings.put(name, value);
  }

  /**
   * 拼接 sql
   */
  public void appendSql(String sql) {
    sqlBuilder.append(sql);
    sqlBuilder.append(" ");
  }

  /**
   * 拿到拼接过后的sql
   */
  public String getSql() {
    return sqlBuilder.toString().trim();
  }

  /**
   * 获取唯一标示号，并自增
   */
  public int getUniqueNumber() {
    return uniqueNumber++;
  }

  /**
   * 继承 HashMap 类，上下文的参数集合
   */
  static class ContextMap extends HashMap<String, Object> {
    private static final long serialVersionUID = 2977601501966151582L;

    /**
     * parameter 对应的 MetaObject 对象
     */
    private MetaObject parameterMetaObject;

    /**
     * 构造函数。note 这包的不是 paramMap 类型！
     */
    public ContextMap(MetaObject parameterMetaObject) {
      this.parameterMetaObject = parameterMetaObject;
    }

    /**
     * 获取
     */
    @Override
    public Object get(Object key) {
      // 转成 string类型，TODO 强转？
      String strKey = (String) key;
      // 如果有 key 对应的值，直接获得
      if (super.containsKey(strKey)) {
        return super.get(strKey);
      }

      // 从 parameterMetaObject 中，获得 key 对应的属性
      if (parameterMetaObject != null) {
        // 读取时，不用去更改 context 的值。原因参见 issue #61
        return parameterMetaObject.getValue(strKey);
      }

      return null;
    }
  }

  /**
   * 实现 ognl.PropertyAccessor 接口，上下文访问器
   */
  static class ContextAccessor implements PropertyAccessor {

    /**
     * 获取指定的属性
     * TODO target 就是OgnlRuntime.setPropertyAccessor(ContextMap.class, new ContextAccessor());中的ContextMap对象？
     * @param context 貌似没用上
     */
    @Override
    public Object getProperty(Map context, Object target, Object name) throws OgnlException {
      Map map = (Map) target;

      // 优先从 ContextMap 中，获得属性
      Object result = map.get(name);
      if (map.containsKey(name) || result != null) {
        return result;
      }

      // 如果没有，则从 PARAMETER_OBJECT_KEY 对应的 Map 中，获得属性
      Object parameterObject = map.get(PARAMETER_OBJECT_KEY);
      // 因为很可能 parameterObject 就是 ParamMap
      if (parameterObject instanceof Map) {
        return ((Map)parameterObject).get(name);
      }

      return null;
    }

    /**
     * 设置属性
     */
    @Override
    public void setProperty(Map context, Object target, Object name, Object value) throws OgnlException {
      Map<Object, Object> map = (Map<Object, Object>) target;
      map.put(name, value);
    }

    /*两个空实现*/

    @Override
    public String getSourceAccessor(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }

    @Override
    public String getSourceSetter(OgnlContext arg0, Object arg1, Object arg2) {
      return null;
    }
  }
}