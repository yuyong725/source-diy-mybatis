package cn.javadog.sd.mybatis.executor.result;

import java.util.ArrayList;
import java.util.List;

import cn.javadog.sd.mybatis.session.ResultContext;
import cn.javadog.sd.mybatis.session.ResultHandler;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;

/**
 * @author Clinton Begin
 *
 * 实现 ResultHandler 接口，默认的 ResultHandler 的实现类
 */
public class DefaultResultHandler implements ResultHandler<Object> {

  /**
   * 结果数组
   */
  private final List<Object> list;

  public DefaultResultHandler() {
    list = new ArrayList<>();
  }

  @SuppressWarnings("unchecked")
  public DefaultResultHandler(ObjectFactory objectFactory) {
    list = objectFactory.create(List.class);
  }

  @Override
  public void handleResult(ResultContext<? extends Object> context) {
    // <1> 将当前结果，添加到结果数组中
    list.add(context.getResultObject());
  }

  public List<Object> getResultList() {
    return list;
  }

}
