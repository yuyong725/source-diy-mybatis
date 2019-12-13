package cn.javadog.sd.mybatis.executor.result;


import cn.javadog.sd.mybatis.session.ResultContext;

/**
 * @author Clinton Begin
 */
public class DefaultResultContext<T> implements ResultContext<T> {

  /**
   * 当前结果对象
   */
  private T resultObject;
  /**
   * 总的结果对象的数量
   */
  private int resultCount;
  /**
   * 是否暂停
   */
  private boolean stopped;

  public DefaultResultContext() {
    resultObject = null;
    resultCount = 0;
    stopped = false;
  }

  @Override
  public T getResultObject() {
    return resultObject;
  }

  @Override
  public int getResultCount() {
    return resultCount;
  }

  @Override
  public boolean isStopped() {
    return stopped;
  }

  /**
   * 当前结果对象
   *
   * @param resultObject 当前结果对象
   */
  public void nextResultObject(T resultObject) {
    // 数量 + 1
    resultCount++;
    // 当前结果对象
    this.resultObject = resultObject;
  }


  @Override
  public void stop() {
    this.stopped = true;
  }

}
