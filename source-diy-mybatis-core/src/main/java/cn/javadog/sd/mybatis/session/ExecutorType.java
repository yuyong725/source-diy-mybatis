package cn.javadog.sd.mybatis.session;

import cn.javadog.sd.mybatis.executor.BatchExecutor;
import cn.javadog.sd.mybatis.executor.ReuseExecutor;
import cn.javadog.sd.mybatis.executor.SimpleExecutor;

/**
 * @author Clinton Begin
 */
public enum ExecutorType {
  /**
   * {@link SimpleExecutor}
   */
  SIMPLE,
  /**
   * {@link ReuseExecutor}
   */
  REUSE,
  /**
   * {@link BatchExecutor}
   */
  BATCH
}
