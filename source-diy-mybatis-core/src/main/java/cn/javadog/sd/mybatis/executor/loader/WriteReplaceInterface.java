package cn.javadog.sd.mybatis.executor.loader;

import java.io.ObjectStreamException;

/**
 * @author 余勇
 * @date 2019-12-15 15:30
 * 强烈建议看看这篇：https://www.codeleading.com/article/7710750378/
 * 序列化时，若懒加载相关的属性没有记载，使用此接口处理
 */
public interface WriteReplaceInterface {

  /**
   * 序列化时，替换未加载的对象
   */
  Object writeReplace() throws ObjectStreamException;

}
