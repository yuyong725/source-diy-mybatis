package cn.javadog.sd.mybatis.executor.result;

import java.util.ArrayList;
import java.util.List;

import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;

/**
 * @author 余勇
 * @date 2019-12-15 17:33
 * 实现 ResultHandler 接口，默认的 ResultHandler 的实现类
 */
public class DefaultResultHandler implements ResultHandler<Object> {

  /**
   * 结果数组
   */
  private final List<Object> list;

  /**
   * 构造函数
   */
  public DefaultResultHandler() {
    list = new ArrayList<>();
  }

  /**
   * 构造函数
   */
  @SuppressWarnings("unchecked")
  public DefaultResultHandler(ObjectFactory objectFactory) {
    // 使用 objectFactory 创建实例
    list = objectFactory.create(List.class);
  }

  @Override
  public void handleResult(ResultContext<? extends Object> context) {
    // 将当前结果，添加到结果数组中
    list.add(context.getResultObject());
  }

  /**
   * 获取结果
   */
  public List<Object> getResultList() {
    return list;
  }

}
