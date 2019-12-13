package cn.javadog.sd.mybatis.session;

/**
 * @author Clinton Begin
 *
 * 结果处理器接口
 */
public interface ResultHandler<T> {

  /**
   * 处理当前结果
   *
   * @param resultContext ResultContext 对象。在其中，可以获得当前结果
   */
  void handleResult(ResultContext<? extends T> resultContext);

}
