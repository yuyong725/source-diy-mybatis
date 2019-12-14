package cn.javadog.sd.mybatis.support.transaction.jdbc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Properties;

import cn.javadog.sd.mybatis.support.transaction.Transaction;
import cn.javadog.sd.mybatis.support.transaction.TransactionFactory;
import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;


/**
 * Creates {@link JdbcTransaction} instances.
 *
 * @author Clinton Begin
 *
 * @see JdbcTransaction
 *
 *
 */
/**
 * @author 余勇
 * @date 2019-12-04 13:51
 * JdbcTransaction 工厂实现类，MyBatis有大量工厂模式
 */
public class JdbcTransactionFactory implements TransactionFactory {

  /**
   * 设置属性，很多框架设置都有这个口子，比如mybatis的插件，反射里的ObjectFactory
   * TODO 这个方法会在启动时读取配置文件，然后允许下面的其他方法调用？
   */
  @Override
  public void setProperties(Properties props) {
  }

  /**
   * 初始化一个事务，注意这不是静态方法，调用必须要先new JdbcTransactionFactory；工厂模式的工厂一般都是new的
   */
  @Override
  public Transaction newTransaction(Connection conn) {
    // 创建 JdbcTransaction 对象
    return new JdbcTransaction(conn);
  }

  /**
   * 初始化一个事务；参数对标的就是JdbcTransaction的构造参数
   */
  @Override
  public Transaction newTransaction(DataSource ds, TransactionIsolationLevel level, boolean autoCommit) {
    return new JdbcTransaction(ds, level, autoCommit);
  }
}
