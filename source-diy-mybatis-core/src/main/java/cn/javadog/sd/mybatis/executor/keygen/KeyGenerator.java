package cn.javadog.sd.mybatis.executor.keygen;

import java.sql.Statement;

import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.mapping.MappedStatement;


/**
 * @author Clinton Begin
 */
public interface KeyGenerator {

  /**
   * SQL 执行前
   */
  void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

  /**
   * SQL 执行后
   */
  void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter);

}
