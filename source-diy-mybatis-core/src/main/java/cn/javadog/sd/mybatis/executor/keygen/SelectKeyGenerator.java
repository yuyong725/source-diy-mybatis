package cn.javadog.sd.mybatis.executor.keygen;

import java.sql.Statement;
import java.util.List;

import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.session.ExecutorType;
import cn.javadog.sd.mybatis.session.RowBounds;
import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;

/**
 * @author 余勇
 * @date 2019-12-15 14:31
 * 基于从数据库查询主键的 KeyGenerator 实现类，适用于 Oracle、PostgreSQL
 */
public class SelectKeyGenerator implements KeyGenerator {

  /**
   * selectKey 后缀，标示用
   */
  public static final String SELECT_KEY_SUFFIX = "!selectKey";

  /**
   * 是否在 before 阶段执行
   *
   * true ：before
   * after ：after
   */
  private final boolean executeBefore;

  /**
   * MappedStatement 对象
   */
  private final MappedStatement keyStatement;

  /**
   * 构造函数
   */
  public SelectKeyGenerator(MappedStatement keyStatement, boolean executeBefore) {
    this.executeBefore = executeBefore;
    this.keyStatement = keyStatement;
  }

  /**
   * SQL 执行前
   */
  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    if (executeBefore) {
      processGeneratedKeys(executor, ms, parameter);
    }
  }

  /**
   * SQL 执行后
   */
  @Override
  public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
    if (!executeBefore) {
      processGeneratedKeys(executor, ms, parameter);
    }
  }

  /**
   * 完成生成主键的逻辑
   * @param parameter 参数值
   */
  private void processGeneratedKeys(Executor executor, MappedStatement ms, Object parameter) {
    try {
      // 有查询主键的 SQL 语句，即 keyStatement 对象非空
      if (parameter != null && keyStatement != null && keyStatement.getKeyProperties() != null) {
        // 获取主键字段列表
        String[] keyProperties = keyStatement.getKeyProperties();
        final Configuration configuration = ms.getConfiguration();
        final MetaObject metaParam = configuration.newMetaObject(parameter);
        if (keyProperties != null) {
          // Do not close keyExecutor.
          // 创建执行器，类型为 SimpleExecutor
          Executor keyExecutor = configuration.newExecutor(executor.getTransaction(), ExecutorType.SIMPLE);
          // 执行查询主键的操作
          List<Object> values = keyExecutor.query(keyStatement, parameter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER);
          // 查不到结果，抛出 ExecutorException 异常
          if (values.size() == 0) {
            throw new ExecutorException("SelectKey returned no data.");
          } else if (values.size() > 1) {
            // 查询的结果过多，抛出 ExecutorException 异常。
            throw new ExecutorException("SelectKey returned more than one value.");
          } else {
            // 创建 MetaObject 对象，访问查询主键的结果
            MetaObject metaResult = configuration.newMetaObject(values.get(0));
            // 单个主键
            if (keyProperties.length == 1) {
              // 设置属性到 metaParam 中，相当于设置到 parameter 中
              if (metaResult.hasGetter(keyProperties[0])) {
                setValue(metaParam, keyProperties[0], metaResult.getValue(keyProperties[0]));
              } else {
                // 没有 get 方法，直接将整个对象设置进去
                setValue(metaParam, keyProperties[0], values.get(0));
              }
            } else {
              // 多个主键，遍历，进行赋值
              handleMultipleProperties(keyProperties, metaParam, metaResult);
            }
          }
        }
      }
    } catch (ExecutorException e) {
      throw e;
    } catch (Exception e) {
      throw new ExecutorException("Error selecting key or setting result to parameter object. Cause: " + e, e);
    }
  }

  /**
   * 处理多主键的场景
   *
   * @param keyProperties 主键对应POJO的字段名
   * @param metaParam 参数值元对象
   * @param metaResult 数据库返回结果
   */
  private void handleMultipleProperties(String[] keyProperties, MetaObject metaParam, MetaObject metaResult) {

    // 获取所有主键列
    String[] keyColumns = keyStatement.getKeyColumns();

    // 遍历，进行赋值
    if (keyColumns == null || keyColumns.length == 0) {
      // 如果没有设置 keyColumns，就直接使用 keyProperties 作为列名
      for (String keyProperty : keyProperties) {
        setValue(metaParam, keyProperty, metaResult.getValue(keyProperty));
      }
    } else {
      // 长度不一致直接GG
      if (keyColumns.length != keyProperties.length) {
        throw new ExecutorException("If SelectKey has key columns, the number must match the number of key properties.");
      }
      // 使用 列名 获取返回结果的值
      for (int i = 0; i < keyProperties.length; i++) {
        setValue(metaParam, keyProperties[i], metaResult.getValue(keyColumns[i]));
      }
    }
  }

  /**
   * 设置指定字段的值
   */
  private void setValue(MetaObject metaParam, String property, Object value) {
    if (metaParam.hasSetter(property)) {
      metaParam.setValue(property, value);
    } else {
      throw new ExecutorException("No setter found for the keyProperty '" + property + "' in " + metaParam.getOriginalObject().getClass().getName() + ".");
    }
  }
}
