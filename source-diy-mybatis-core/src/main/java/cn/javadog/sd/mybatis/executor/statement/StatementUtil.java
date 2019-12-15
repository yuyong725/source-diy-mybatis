package cn.javadog.sd.mybatis.executor.statement;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author 余勇
 * @date 2019-12-15 13:12
 *
 * statement 工具。
 * core模块就这一个util结尾的工具类 ，所以不另外开一个包了
 */
public class StatementUtil {

  /**
   * 构造方法，不对外开放
   */
  private StatementUtil() {
  }

  /**
   * 设置statement的超时时间，基于事务的超时时间和全局的查询超时时间
   *
   * @param statement 目标会话
   * @param queryTimeout 全局查询超时时间
   * @param transactionTimeout 事务超时时间
   * @throws SQLException 如果发生SQL异常，会调用 statement 的close方法
   */
  public static void applyTransactionTimeout(Statement statement, Integer queryTimeout, Integer transactionTimeout) throws SQLException {
    // 事务超时时间为 null 的话，直接返回
    if (transactionTimeout == null){
      return;
    }
    Integer timeToLiveOfQuery = null;
    // 如果全局 查询超时时间 为0或者null的话
    if (queryTimeout == null || queryTimeout == 0) {
      // 设置 statement 查询超时时间为事务的超时时间
      timeToLiveOfQuery = transactionTimeout;
    } else if (transactionTimeout < queryTimeout) {
      // 如果全局查询超时时间比事务的超时时间还要大的话，设置 statement 超时时间为事务超时时间
      timeToLiveOfQuery = transactionTimeout;
    }
    // 设置 statement 的查询超时时间
    if (timeToLiveOfQuery != null) {
      statement.setQueryTimeout(timeToLiveOfQuery);
    }
  }

}
