package cn.javadog.sd.mybatis.executor.result;

/**
 * @author 余勇
 * @date 2019-12-15 17:30
 *
 * 结果处理器接口，源码在session包下
 */
public interface ResultHandler<T> {

  /**
   * 处理当前结果
   *
   * @param resultContext ResultContext 对象。在其中，可以获得当前结果
   */
  void handleResult(ResultContext<? extends T> resultContext);

}
