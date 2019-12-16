package cn.javadog.sd.mybatis.executor.loader;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import cn.javadog.sd.mybatis.cursor.Cursor;
import cn.javadog.sd.mybatis.executor.BaseExecutor;
import cn.javadog.sd.mybatis.executor.BatchResult;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;

/**
 * @author Clinton Begin
 * @author Franta Mejta
 */
/**
 * @author 余勇
 * @date 2019-12-15 16:07
 * 用于记录要被懒加载的ResultLoader
 */
public class ResultLoaderMap {

  /**
   * LoadPair 的映射
   * key：首字母大小后的属性
   * value：LoadPair对象
   */
  private final Map<String, LoadPair> loaderMap = new HashMap<>();

  /**
   * 将指定属性的懒加载信息添加到 loaderMap 中
   */
  public void addLoader(String property, MetaObject metaResultObject, ResultLoader resultLoader) {
    // 将首字母大小
    String upperFirst = getUppercaseFirstProperty(property);
    // 已存在，则抛出 ExecutorException 异常
    if (!upperFirst.equalsIgnoreCase(property) && loaderMap.containsKey(upperFirst)) {
      throw new ExecutorException("Nested lazy loaded result property '" + property +
              "' for query id '" + resultLoader.mappedStatement.getId() +
              " already exists in the result map. The leftmost property of all lazy loaded properties must be unique within a result map.");
    }
    // 创建 LoadPair 对象，添加到 loaderMap 中
    loaderMap.put(upperFirst, new LoadPair(property, metaResultObject, resultLoader));
  }

  /**
   * 获取所有懒加载属性及相应的加载器
   */
  public final Map<String, LoadPair> getProperties() {
    return new HashMap<>(this.loaderMap);
  }

  /**
   * 获取未加载的属性名
   */
  public Set<String> getPropertyNames() {
    return loaderMap.keySet();
  }

  /**
   * 获取还有多少属性未加载
   */
  public int size() {
    return loaderMap.size();
  }

  /**
   * 查询指定属性是否未加载
   */
  public boolean hasLoader(String property) {
    return loaderMap.containsKey(property.toUpperCase(Locale.ENGLISH));
  }

  /**
   * 执行指定属性的加载
   */
  public boolean load(String property) throws SQLException {
    // 获得 LoadPair 对象，并移除
    LoadPair pair = loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
    // 执行加载
    if (pair != null) {
      pair.load();
      // 加载成功
      return true;
    }
    // 加载失败
    return false;
  }

  /**
   * 移除指定属性
   */
  public void remove(String property) {
    loaderMap.remove(property.toUpperCase(Locale.ENGLISH));
  }

  /**
   * 执行所有属性的加载
   */
  public void loadAll() throws SQLException {
    // 遍历 loaderMap 属性
    final Set<String> methodNameSet = loaderMap.keySet();
    String[] methodNames = methodNameSet.toArray(new String[methodNameSet.size()]);
    for (String methodName : methodNames) {
      // 执行加载
      load(methodName);
    }
  }

  /**
   * 使用 . 分隔属性，并获得首个字符串，并大写。
   *
   * 如 abc.bcd => Abc.Bcd
   *
   * @param property 属性
   * @return 字符串 + 大写
   */
  private static String getUppercaseFirstProperty(String property) {
    String[] parts = property.split("\\.");
    return parts[0].toUpperCase(Locale.ENGLISH);
  }

  /**
   * 内部类，记录要被懒加载的字段，及懒记载的方式等信息。主要用于序列化场景吧，即主对象还有属性未被加载，此时序列化如何处理未加载的属性
   */
  public static class LoadPair implements Serializable {

    private static final long serialVersionUID = 20130412;

    /**
     * 获取数据库连接的工厂方法的名字
     */
    private static final String FACTORY_METHOD = "getConfiguration";

    /**
     * 检查对象是否序列化
     */
    private final transient Object serializationCheck = new Object();

    /**
     * Meta object which sets loaded properties.
     */
    private transient MetaObject metaResultObject;

    /**
     * 未加载属性的结果加载器
     */
    private transient ResultLoader resultLoader;

    /**
     * 日志打印器
     */
    private transient Log log;

    /**
     * 全局配置工厂，提供此工厂可以拿到数据库连接信息。查询结果可序列化才会初始化此属性
     */
    private Class<?> configurationFactory;

    /**
     * 要被懒加载的属性名
     */
    private String property;

    /**
     * 加载属性值的SQL对应的mappedStatement。查询结果可序列化才会初始化此属性
     */
    private String mappedStatement;

    /**
     * sql语句的参数值。查询结果可序列化才会初始化此属性
     */
    private Serializable mappedParameter;

    /**
     * 构造方法
     *
     * @param metaResultObject 上一级的结果对象
     */
    private LoadPair(final String property, MetaObject metaResultObject, ResultLoader resultLoader) {
      this.property = property;
      this.metaResultObject = metaResultObject;
      this.resultLoader = resultLoader;

      // 当 `metaResultObject.originalObject` 可序列化时，则记录 mappedStatement、mappedParameter、configurationFactory 属性
      if (metaResultObject != null && metaResultObject.getOriginalObject() instanceof Serializable) {
        final Object mappedStatementParameter = resultLoader.parameterObject;

        /* @todo May the parameter be null? */
        if (mappedStatementParameter instanceof Serializable) {
          this.mappedStatement = resultLoader.mappedStatement.getId();
          this.mappedParameter = (Serializable) mappedStatementParameter;
          this.configurationFactory = resultLoader.configuration.getConfigurationFactory();
        } else {
          Log log = this.getLogger();
          // 打印日志
          if (log.isDebugEnabled()) {
            log.debug("Property [" + this.property + "] of ["
                    + metaResultObject.getOriginalObject().getClass() + "] cannot be loaded "
                    + "after deserialization. Make sure it's loaded before serializing "
                    + "forenamed object.");
          }
        }
      }
    }

    /**
     * 执行加载
     */
    public void load() throws SQLException {
      // metaResultObject 和 resultLoader 不能为空，除非已经被序列化，因为序列化时，这两个属性不会被序列化。
      // 在那种情况下，此方法不会被加载
      if (this.metaResultObject == null) {
        throw new IllegalArgumentException("metaResultObject is null");
      }
      if (this.resultLoader == null) {
        throw new IllegalArgumentException("resultLoader is null");
      }
      this.load(null);
    }

    /**
     * 记载属性值.
     * 主要是对属性的校验，加载逻辑是 ResultLoader 完成的
     */
    public void load(final Object userObject) throws SQLException {
      if (this.metaResultObject == null || this.resultLoader == null) {
        if (this.mappedParameter == null) {
          throw new ExecutorException("Property [" + this.property + "] cannot be loaded because "
                  + "required parameter of mapped statement ["
                  + this.mappedStatement + "] is not serializable.");
        }

        // 获得 Configuration 对象
        final Configuration config = this.getConfiguration();
        // 获得 MappedStatement 对象
        final MappedStatement ms = config.getMappedStatement(this.mappedStatement);
        if (ms == null) {
          throw new ExecutorException("Cannot lazy load property [" + this.property
                  + "] of deserialized object [" + userObject.getClass()
                  + "] because configuration does not contain statement ["
                  + this.mappedStatement + "]");
        }

        // 获得对应的 MetaObject 对象。note 这是针对 metaResultObject || resultLoader == null
        this.metaResultObject = config.newMetaObject(userObject);
        // 创建 ResultLoader 对象
        this.resultLoader = new ResultLoader(config, new ClosedExecutor(), ms, this.mappedParameter,
                metaResultObject.getSetterType(this.property), null, null);
      }
      // 使用新的执行器，因为我们可能是在新的新的线程进行加载，为了保证线程安全
      if (this.serializationCheck == null) {
        final ResultLoader old = this.resultLoader;
        this.resultLoader = new ResultLoader(old.configuration, new ClosedExecutor(), old.mappedStatement,
                old.parameterObject, old.targetType, old.cacheKey, old.boundSql);
      }
      // 将结果设置到指定的属性
      this.metaResultObject.setValue(property, this.resultLoader.loadResult());
    }

    /**
     * 获取 Configuration，都是些校验，不细看了
     */
    private Configuration getConfiguration() {
      if (this.configurationFactory == null) {
        throw new ExecutorException("Cannot get Configuration as configuration factory was not set.");
      }

      Object configurationObject = null;
      try {
        final Method factoryMethod = this.configurationFactory.getDeclaredMethod(FACTORY_METHOD);
        if (!Modifier.isStatic(factoryMethod.getModifiers())) {
          throw new ExecutorException("Cannot get Configuration as factory method ["
                  + this.configurationFactory + "]#["
                  + FACTORY_METHOD + "] is not static.");
        }

        if (!factoryMethod.isAccessible()) {
          configurationObject = AccessController.doPrivileged(new PrivilegedExceptionAction<Object>() {
            @Override
            public Object run() throws Exception {
              try {
                factoryMethod.setAccessible(true);
                return factoryMethod.invoke(null);
              } finally {
                factoryMethod.setAccessible(false);
              }
            }
          });
        } else {
          configurationObject = factoryMethod.invoke(null);
        }
      } catch (final ExecutorException ex) {
        throw ex;
      } catch (final NoSuchMethodException ex) {
        throw new ExecutorException("Cannot get Configuration as factory class ["
                + this.configurationFactory + "] is missing factory method of name ["
                + FACTORY_METHOD + "].", ex);
      } catch (final PrivilegedActionException ex) {
        throw new ExecutorException("Cannot get Configuration as factory method ["
                + this.configurationFactory + "]#["
                + FACTORY_METHOD + "] threw an exception.", ex.getCause());
      } catch (final Exception ex) {
        throw new ExecutorException("Cannot get Configuration as factory method ["
                + this.configurationFactory + "]#["
                + FACTORY_METHOD + "] threw an exception.", ex);
      }

      if (!(configurationObject instanceof Configuration)) {
        throw new ExecutorException("Cannot get Configuration as factory method ["
                + this.configurationFactory + "]#["
                + FACTORY_METHOD + "] didn't return [" + Configuration.class + "] but ["
                + (configurationObject == null ? "null" : configurationObject.getClass()) + "].");
      }

      return Configuration.class.cast(configurationObject);
    }

    /**
     * 获取日志对象
     */
    private Log getLogger() {
      if (this.log == null) {
        this.log = LogFactory.getLog(this.getClass());
      }
      return this.log;
    }
  }

  /**
   * 已经关闭的 Executor 实现类
   * 仅仅在 ResultLoaderMap 中，作为一个“空”的 Executor 对象。没有什么特殊的意义和用途。
   */
  private static final class ClosedExecutor extends BaseExecutor {

    public ClosedExecutor() {
      super(null, null);
    }

    @Override
    public boolean isClosed() {
      return true;
    }

    /*👇的方法都没做实现，因为只是个占位符*/

    @Override
    protected int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected List<BatchResult> doFlushStatements(boolean isRollback) throws SQLException {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
      throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    protected <E> Cursor<E> doQueryCursor(MappedStatement ms, Object parameter, RowBounds rowBounds, BoundSql boundSql) throws SQLException {
      throw new UnsupportedOperationException("Not supported.");
    }
  }
}
