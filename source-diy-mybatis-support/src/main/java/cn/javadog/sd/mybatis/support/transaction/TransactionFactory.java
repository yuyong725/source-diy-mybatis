package cn.javadog.sd.mybatis.support.transaction;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

/**
 * @author: 余勇
 * @date: 2019-12-04 14:09
 *
 */
public interface TransactionFactory {

  /**
   * 设置工厂的属性
   */
  void setProperties(Properties props);

  /**
   * 创建 Transaction 事务
   */
  Transaction newTransaction(Connection conn);
  
  /**
   * 创建 Transaction 事务
   */
  Transaction newTransaction(DataSource dataSource, TransactionIsolationLevel level, boolean autoCommit);

}
