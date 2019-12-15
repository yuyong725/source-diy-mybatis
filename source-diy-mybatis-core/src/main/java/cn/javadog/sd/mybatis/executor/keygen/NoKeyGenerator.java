package cn.javadog.sd.mybatis.executor.keygen;

import java.sql.Statement;

import cn.javadog.sd.mybatis.executor.Executor;
import cn.javadog.sd.mybatis.mapping.MappedStatement;

/**
 * @author 余勇
 * @date 2019-12-13 21:07
 *
 * 空的 KeyGenerator 实现类，即无需主键生成
 */
public class NoKeyGenerator implements KeyGenerator {

  /**
   * 共享的单例
   * @since 3.4.3
   */
  public static final NoKeyGenerator INSTANCE = new NoKeyGenerator();

  /**
   * 啥事不干
   */
  @Override
  public void processBefore(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
  }

  /**
   * 啥事不干
   */
  @Override
  public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
  }

}
