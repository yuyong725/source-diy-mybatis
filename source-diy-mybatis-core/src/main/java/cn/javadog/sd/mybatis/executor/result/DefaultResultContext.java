package cn.javadog.sd.mybatis.executor.result;

/**
 * @author 余勇
 * @date 2019-12-15 17:36
 *
 * ResultContext 的默认实现
 */
public class DefaultResultContext<T> implements ResultContext<T> {

  /**
   * 当前结果对象
   */
  private T resultObject;

  /**
   * 已经获取的结果数量
   */
  private int resultCount;

  /**
   * 是否停止获取结果
   */
  private boolean stopped;

  /**
   * 构造函数
   */
  public DefaultResultContext() {
    resultObject = null;
    resultCount = 0;
    stopped = false;
  }

  /**
   * 获取结果
   */
  @Override
  public T getResultObject() {
    return resultObject;
  }

  /**
   * 获取返回的结果行数
   */
  @Override
  public int getResultCount() {
    return resultCount;
  }

  /**
   * 是否停止获取结果
   */
  @Override
  public boolean isStopped() {
    return stopped;
  }

  /**
   * 将获取的结果赋值给当前对象
   *
   * @param resultObject 当前结果对象
   */
  public void nextResultObject(T resultObject) {
    // 数量 + 1
    resultCount++;
    // 当前结果对象
    this.resultObject = resultObject;
  }


  /**
   * 停止获取结果
   */
  @Override
  public void stop() {
    this.stopped = true;
  }

}
