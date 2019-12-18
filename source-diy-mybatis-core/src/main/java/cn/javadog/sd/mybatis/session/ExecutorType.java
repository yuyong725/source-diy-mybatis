package cn.javadog.sd.mybatis.session;

import cn.javadog.sd.mybatis.executor.BatchExecutor;
import cn.javadog.sd.mybatis.executor.ReuseExecutor;
import cn.javadog.sd.mybatis.executor.SimpleExecutor;

/**
 * @author 余勇
 * @date 2019-12-17 15:29
 *
 * 执行器类型
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
