package cn.javadog.sd.mybatis.executor.keygen;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.defaults.DefaultSqlSession.StrictMap;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.type.JdbcType;
import cn.javadog.sd.mybatis.support.type.ParamMap;
import cn.javadog.sd.mybatis.support.type.TypeHandler;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;
import cn.javadog.sd.mybatis.support.util.ArrayUtil;

/**
 * @author Clinton Begin
 * @author Kazuki Shimizu
 *
 * 基于 Statement#getGeneratedKeys() 方法的 KeyGenerator 实现类，适用于 MySQL、H2 主键生成
 */
public class Jdbc3KeyGenerator implements KeyGenerator {

  /**
   * A shared instance.
   * 
   * @since 3.4.3
   * 共享的单例
   */
  public static final Jdbc3KeyGenerator INSTANCE = new Jdbc3KeyGenerator();

  /**
   * 空实现。因为对于 Jdbc3KeyGenerator 类的主键，是在 SQL 执行后，才生成
   */
  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    // do nothing
  }

  /**
   *调用 #processBatch(Executor executor, MappedStatement ms, Statement stmt, Object parameter) 方法，处理返回的自增主键。
   * 单个 parameter 参数，可以认为是批量的一个特例
   */
  @Override
  public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    processBatch(ms, stmt, parameter);
  }

  public void processBatch(MappedStatement ms, Statement stmt, Object parameter) {
    // <1> 获得主键属性的配置。如果为空，则直接返回，说明不需要主键
    final String[] keyProperties = ms.getKeyProperties();
    if (keyProperties == null || keyProperties.length == 0) {
      return;
    }
    ResultSet rs = null;
    try {
      // <2> 获得返回的自增主键
      rs = stmt.getGeneratedKeys();
      final Configuration configuration = ms.getConfiguration();
      if (rs.getMetaData().getColumnCount() >= keyProperties.length) {
        // <3> 获得唯一的参数对象
        Object soleParam = getSoleParameter(parameter);
        if (soleParam != null) {
          // <3.1> 设置主键们，到参数 soleParam 中
          assignKeysToParam(configuration, rs, keyProperties, soleParam);
        } else {
          // <3.2> 设置主键们，到参数 parameter 中
          assignKeysToOneOfParams(configuration, rs, keyProperties, (Map<?, ?>) parameter);
        }
      }
    } catch (Exception e) {
      throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
    } finally {
      // <4> 关闭 ResultSet 对象
      if (rs != null) {
        try {
          rs.close();
        } catch (Exception e) {
          // ignore
        }
      }
    }
  }

  /**
   * 设置主键们，到参数 parameter 中
   */
  protected void assignKeysToOneOfParams(final Configuration configuration, ResultSet rs, final String[] keyProperties,
      Map<?, ?> paramMap) throws SQLException {
    // Assuming 'keyProperty' includes the parameter name. e.g. 'param.id'.
    // <1> 需要有 `.` 。
    int firstDot = keyProperties[0].indexOf('.');
    if (firstDot == -1) {
      throw new ExecutorException(
          "Could not determine which parameter to assign generated keys to. "
              + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
              + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
              + paramMap.keySet());
    }
    // 获得真正的参数值
    String paramName = keyProperties[0].substring(0, firstDot);
    Object param;
    if (paramMap.containsKey(paramName)) {
      param = paramMap.get(paramName);
    } else {
      throw new ExecutorException("Could not find parameter '" + paramName + "'. "
          + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
          + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
          + paramMap.keySet());
    }
    // Remove param name from 'keyProperty' string. e.g. 'param.id' -> 'id'
    // 获得主键的属性的配置
    String[] modifiedKeyProperties = new String[keyProperties.length];
    for (int i = 0; i < keyProperties.length; i++) {
      if (keyProperties[i].charAt(firstDot) == '.' && keyProperties[i].startsWith(paramName)) {
        modifiedKeyProperties[i] = keyProperties[i].substring(firstDot + 1);
      } else {
        throw new ExecutorException("Assigning generated keys to multiple parameters is not supported. "
            + "Note that when there are multiple parameters, 'keyProperty' must include the parameter name (e.g. 'param.id'). "
            + "Specified key properties are " + ArrayUtil.toString(keyProperties) + " and available parameters are "
            + paramMap.keySet());
      }
    }
    // 设置主键们，到参数 param 中
    assignKeysToParam(configuration, rs, modifiedKeyProperties, param);
  }

  /**
   * 设置主键们，到参数 param 中
   */
  private void assignKeysToParam(final Configuration configuration, ResultSet rs, final String[] keyProperties,
      Object param)
      throws SQLException {
    final TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
    final ResultSetMetaData rsmd = rs.getMetaData();
    // Wrap the parameter in Collection to normalize the logic.
    // <1> 包装成 Collection 对象
    Collection<?> paramAsCollection;
    if (param instanceof Object[]) {
      paramAsCollection = Arrays.asList((Object[]) param);
    } else if (!(param instanceof Collection)) {
      paramAsCollection = Collections.singletonList(param);
    } else {
      paramAsCollection = (Collection<?>) param;
    }
    TypeHandler<?>[] typeHandlers = null;
    // <2> 遍历 paramAsCollection 数组
    for (Object obj : paramAsCollection) {
      // <2.1> 顺序遍历 rs
      if (!rs.next()) {
        break;
      }
      // <2.2> 创建 MetaObject 对象
      MetaObject metaParam = configuration.newMetaObject(obj);
      // <2.3> 获得 TypeHandler 数组
      if (typeHandlers == null) {
        typeHandlers = getTypeHandlers(typeHandlerRegistry, metaParam, keyProperties, rsmd);
      }
      // <2.4> 填充主键们
      populateKeys(rs, metaParam, keyProperties, typeHandlers);
    }
  }

  /**
   * 获得唯一的参数对象
   *
   * 如果获得不到唯一的参数对象，则返回 null
   *
   * @param parameter 参数对象
   * @return 唯一的参数对象
   */
  private Object getSoleParameter(Object parameter) {
    // <1> 如果非 Map 对象，则直接返回 parameter
    if (!(parameter instanceof ParamMap || parameter instanceof StrictMap)) {
      return parameter;
    }
    // <3> 如果是 Map 对象，则获取第一个元素的值
    // <2> 如果有多个元素，则说明获取不到唯一的参数对象，则返回 null
    Object soleParam = null;
    for (Object paramValue : ((Map<?, ?>) parameter).values()) {
      if (soleParam == null) {
        soleParam = paramValue;
      } else if (soleParam != paramValue) {
        soleParam = null;
        break;
      }
    }
    return soleParam;
  }

  /**
   * 获得 TypeHandler 数组
   */
  private TypeHandler<?>[] getTypeHandlers(TypeHandlerRegistry typeHandlerRegistry, MetaObject metaParam, String[] keyProperties, ResultSetMetaData rsmd) throws SQLException {
    // 获得主键们，对应的每个属性的，对应的 TypeHandler 对象
    TypeHandler<?>[] typeHandlers = new TypeHandler<?>[keyProperties.length];
    for (int i = 0; i < keyProperties.length; i++) {
      if (metaParam.hasSetter(keyProperties[i])) {
        Class<?> keyPropertyType = metaParam.getSetterType(keyProperties[i]);
        typeHandlers[i] = typeHandlerRegistry.getTypeHandler(keyPropertyType, JdbcType.forCode(rsmd.getColumnType(i + 1)));
      } else {
        throw new ExecutorException("No setter found for the keyProperty '" + keyProperties[i] + "' in '"
            + metaParam.getOriginalObject().getClass().getName() + "'.");
      }
    }
    return typeHandlers;
  }

  /**
   * 填充主键们
   */
  private void populateKeys(ResultSet rs, MetaObject metaParam, String[] keyProperties, TypeHandler<?>[] typeHandlers) throws SQLException {
    // 遍历 keyProperties
    for (int i = 0; i < keyProperties.length; i++) {
      // 获得属性名
      String property = keyProperties[i];
      // 获得 TypeHandler 对象
      TypeHandler<?> th = typeHandlers[i];
      if (th != null) {
        // 从 rs 中，获得对应的 值
        Object value = th.getResult(rs, i + 1);
        // 设置到 metaParam 的对应 property 属性种
        metaParam.setValue(property, value);
      }
    }
  }

}
