package cn.javadog.sd.mybatis.executor;

import java.sql.BatchUpdateException;
import java.util.List;

import cn.javadog.sd.mybatis.support.exceptions.ExecutorException;

/**
 * @author 余勇
 * @date 2019-12-16 20:57
 * 批处理异常。
 * 当进行批处理报错时会抛出，根源是 BatchUpdateException
 */
public class BatchExecutorException extends ExecutorException {

  private static final long serialVersionUID = 154049229650533990L;

  /**
   * 成功的批处理结果
   */
  private final List<BatchResult> successfulBatchResults;

  /**
   * 批量更新异常
   */
  private final BatchUpdateException batchUpdateException;

  /**
   * 出错的那条记录
   */
  private final BatchResult batchResult;

  /**
   * 构造函数
   */
  public BatchExecutorException(String message, 
                                BatchUpdateException cause, 
                                List<BatchResult> successfulBatchResults,
                                BatchResult batchResult) {
    super(message + " Cause: " + cause, cause);
    this.batchUpdateException = cause;
    this.successfulBatchResults = successfulBatchResults;
    this.batchResult = batchResult;
  }

  /**
   * 返回异常的根源 batchUpdateException。该异常包含一个数组，记录批处理语句每一条影响的行数，
   * 通过它能找到到底哪个 statement 产生的错误
   */
  public BatchUpdateException getBatchUpdateException() {
    return batchUpdateException;
  }

  /**
   * 返回 successfulBatchResults。是出错前每个 statement 的结果
   */
  public List<BatchResult> getSuccessfulBatchResults() {
    return successfulBatchResults;
  }

  /**
   * 返回导致识别的 statement 的SQL
   */
  public String getFailingSqlStatement() {
    return batchResult.getSql();
  }

  /**
   * 拿到失败的 statement 的ID
   */
  public String getFailingStatementId() {
    return batchResult.getMappedStatement().getId();
  }
}
