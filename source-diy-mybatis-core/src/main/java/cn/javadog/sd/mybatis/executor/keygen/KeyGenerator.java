package cn.javadog.sd.mybatis.executor.keygen;

import java.sql.Statement;

import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.mapping.MappedStatement;


/**
 * @author 余勇
 * @date 2019-12-15 14:29
 * 主键生成器接口，用于将数据库生成的值，反写回来。
 * note 不一定是主键，只是为了翻译，可能是任何字段
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
