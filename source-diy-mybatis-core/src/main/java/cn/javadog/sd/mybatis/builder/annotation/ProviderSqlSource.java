package cn.javadog.sd.mybatis.builder.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import cn.javadog.sd.mybatis.binding.ParamNameResolver;
import cn.javadog.sd.mybatis.builder.SqlSourceBuilder;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.parsing.PropertyParser;

/**
 * @author: 余勇
 * @date: 2019-12-13 13:45
 * 基于方法上的 @ProviderXXX 注解的 SqlSource 实现类
 */
public class ProviderSqlSource implements SqlSource {

  /**
   * 全局配置
   */
  private final Configuration configuration;

  /**
   * SqlSource 构建器。
   */
  private final SqlSourceBuilder sqlSourceParser;

  /**
   * `@ProviderXXX` 注解的对应的类，不是 mapper接口类
   */
  private final Class<?> providerType;

  /**
   * `@ProviderXXX` 注解的对应的方法，不是mapper接口方法
   */
  private Method providerMethod;

  /**
   * `@ProviderXXX` 注解的对应的方法的参数名数组
   */
  private String[] providerMethodArgumentNames;

  /**
   * `@ProviderXXX` 注解的对应的方法的参数类型数组
   */
  private Class<?>[] providerMethodParameterTypes;

  /**
   * 若 {@link #providerMethodParameterTypes} 参数有 ProviderContext 类型的，创建 ProviderContext 对象
   */
  private ProviderContext providerContext;

  /**
   * {@link #providerMethodParameterTypes} 参数中，ProviderContext 类型的参数，在数组中的位置
   */
  private Integer providerContextIndex;

  /**
   * 构造
   *
   * @param provider 注解实例对象
   * @param configuration 全局配置
   * @param mapperType mapper接口类
   * @param mapperMethod mapper接口类型
   *
   * @since 3.4.5
   */
  public ProviderSqlSource(Configuration configuration, Object provider, Class<?> mapperType, Method mapperMethod) {
    String providerMethodName;
    try {
      this.configuration = configuration;
      // 创建 SqlSourceBuilder 对象
      this.sqlSourceParser = new SqlSourceBuilder(configuration);
      // 获得 @XXXProvider 注解实例的 type 属性
      this.providerType = (Class<?>) provider.getClass().getMethod("type").invoke(provider);
      // 获得 @XXXProvider 注解的对应的方法相关的信息
      providerMethodName = (String) provider.getClass().getMethod("method").invoke(provider);
      for (Method m : this.providerType.getMethods()) {
        // 找到名字匹配，并且返回类型是字符串的类
        if (providerMethodName.equals(m.getName()) && CharSequence.class.isAssignableFrom(m.getReturnType())) {
          // TODO 为什么如此判断，虽然道理上讲，重复赋值报错没问题
          if (providerMethod != null) {
            throw new BuilderException("Error creating SqlSource for SqlProvider. Method '"
                    + providerMethodName + "' is found multiple in SqlProvider '" + this.providerType.getName()
                    + "'. Sql provider method can not overload.");
          }
          // 初始化方法
          this.providerMethod = m;
          // 初始化方法参数名列表
          this.providerMethodArgumentNames = new ParamNameResolver(configuration, m).getNames();
          // 初始化参数类型列表
          this.providerMethodParameterTypes = m.getParameterTypes();
        }
      }
    } catch (BuilderException e) {
      throw e;
    } catch (Exception e) {
      throw new BuilderException("Error creating SqlSource for SqlProvider.  Cause: " + e, e);
    }

    // 没找到对应的方法，直接GG
    if (this.providerMethod == null) {
      throw new BuilderException("Error creating SqlSource for SqlProvider. Method '"
              + providerMethodName + "' not found in SqlProvider '" + this.providerType.getName() + "'.");
    }

    // 初始化 providerContext 和 providerContextIndex 属性
    for (int i = 0; i < this.providerMethodParameterTypes.length; i++) {
      Class<?> parameterType = this.providerMethodParameterTypes[i];
      if (parameterType == ProviderContext.class) {
        // TODO 同上面的问题一样，providerContext 在构造函数调用之前，不可能不为空啊
        if (this.providerContext != null) {
          throw new BuilderException("Error creating SqlSource for SqlProvider. ProviderContext found multiple in SqlProvider method ("
                  + this.providerType.getName() + "." + providerMethod.getName()
                  + "). ProviderContext can not define multiple in SqlProvider method argument.");
        }
        // 初始化 providerContext
        this.providerContext = new ProviderContext(mapperType, mapperMethod);
        // 设置 providerContextIndex
        this.providerContextIndex = i;
      }
    }
  }


  /**
   * 获取 BoundSql
   */
  @Override
  public BoundSql getBoundSql(Object parameterObject) {
    // 创建 SqlSource 对象
    SqlSource sqlSource = createSqlSource(parameterObject);
    // 获得 BoundSql 对象
    return sqlSource.getBoundSql(parameterObject);
  }

  /**
   * 创建 SqlSource 对象
   *
   * @param parameterObject 一个参数( TODO 包括特殊类型，如RowBounds ?)的话就直接是参数值，多个参数的话就是 paramMap
   */
  private SqlSource createSqlSource(Object parameterObject) {
    try {
      // 获得真正参数的数量(去掉providerContext类型)
      int bindParameterCount = providerMethodParameterTypes.length - (providerContext == null ? 0 : 1);
      String sql;
      if (providerMethodParameterTypes.length == 0) {
        // 获取 SQL，无参数的
        sql = invokeProviderMethod();
      } else if (bindParameterCount == 0) {
        // 获取 SQL，只有 providerContext 一个参数的
        sql = invokeProviderMethod(providerContext);
      } else if (bindParameterCount == 1 &&
              (parameterObject == null || providerMethodParameterTypes[(providerContextIndex == null || providerContextIndex == 1) ? 0 : 1].isAssignableFrom(parameterObject.getClass()))) {
        // 获取 SQL，参数有1个，providerContext可有可无，参数值要么是空，要么类型符合解析时的方法参数的类型。这种情况要修改下方法的数据结构再执行
        sql = invokeProviderMethod(extractProviderMethodArguments(parameterObject));
      } else if (parameterObject instanceof Map) {
        // 获取 SQL，参数类型是 Map(TODO paramMap?)，这个map之所以没进👆的判断，是因为方法的参数不是map类型，这个map包含了所有参数的名字和值
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) parameterObject;
        sql = invokeProviderMethod(extractProviderMethodArguments(params, providerMethodArgumentNames));
      } else {
        // 其他情况，直接GG
        throw new BuilderException("Error invoking SqlProvider method ("
                + providerType.getName() + "." + providerMethod.getName()
                + "). Cannot invoke a method that holds "
                + (bindParameterCount == 1 ? "named argument(@Param)" : "multiple arguments")
                + " using a specifying parameterObject. In this case, please specify a 'java.util.Map' object.");
      }
      // 获得parameterObject的类型
      Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
      // 替换掉 SQL 上的属性，解析出 SqlSource 对象
      return sqlSourceParser.parse(replacePlaceholder(sql), parameterType, new HashMap<>());
    } catch (BuilderException e) {
      throw e;
    } catch (Exception e) {
      throw new BuilderException("Error invoking SqlProvider method ("
              + providerType.getName() + "." + providerMethod.getName()
              + ").  Cause: " + e, e);
    }
  }

  /**
   * 解析参数值。如果参数中包含 providerContext ，就将其也加在参数数组中。
   * 因为 parameterObject 是传给接口方法的参数值，不可能有 providerContext 的
   */
  private Object[] extractProviderMethodArguments(Object parameterObject) {
    // providerContext 不为空的，将结果拆成大小为2的数组
    if (providerContext != null) {
      Object[] args = new Object[2];
      // note 这里可以看出 providerContext 不一定是provider类的方法的最后一个参数。且其方法只至多有两个参数，providerContext+可变参数arg...
      // providerContext 是provider方法的第一个参数的话，就将其依然放在最前面，否则，就放在后面
      args[providerContextIndex == 0 ? 1 : 0] = parameterObject;
      args[providerContextIndex] = providerContext;
      return args;
    } else {
      // 没有 providerContext 的话，直接包在数组里面返回就好
      return new Object[] { parameterObject };
    }
  }

  /**
   * 从map类型的参数值中，获取所有的方法。
   * note argumentNames 的 size 可能比 params 的 size 大1，因为它可能包含 providerContext
   */
  private Object[] extractProviderMethodArguments(Map<String, Object> params, String[] argumentNames) {
    Object[] args = new Object[argumentNames.length];
    for (int i = 0; i < args.length; i++) {
      // 参数中有providerContext的话，就加上
      if (providerContextIndex != null && providerContextIndex == i) {
        args[i] = providerContext;
      } else {
        // 其他参数从map里面拿
        args[i] = params.get(argumentNames[i]);
      }
    }
    return args;
  }

  /**
   * 执行 provider的方法，拼接的逻辑在sql里面完成，获得SQL
   */
  private String invokeProviderMethod(Object... args) throws Exception {
    Object targetObject = null;
    // 非静态方法的话，就创建实例
    if (!Modifier.isStatic(providerMethod.getModifiers())) {
      targetObject = providerType.newInstance();
    }
    // note 很有意思，静态方法的话，对象可以是null
    CharSequence sql = (CharSequence) providerMethod.invoke(targetObject, args);
    // 返回 sql
    return sql != null ? sql.toString() : null;
  }

  /**
   * 替换 SQL 的属性，这里替换的是 ${} 符号的属性，即说明 SQL语句可以写成如 "SELECT * FROM table WHERE global_property=${global_property}"
   * note 这里没有  #{} 符号的要替换，因为这不是 xml形式的。
   */
  private String replacePlaceholder(String sql) {
    return PropertyParser.parse(sql, configuration.getVariables());
  }

}
