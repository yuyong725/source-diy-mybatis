package cn.javadog.sd.mybatis.session;

/**
 * @author: 余勇
 * @date: 2019-12-13 16:19
 *
 * 结果上下文接口
 */
public interface ResultContext<T> {

  /**
   * 获取 当前结果对象
   */
  T getResultObject();

  /**
   * 获取 总的结果对象的数量
   */
  int getResultCount();

  /**
   *  是否已经停止对结果的解析
   */
  boolean isStopped();

  /**
   * 停止结果解析
   */
  void stop();

}
