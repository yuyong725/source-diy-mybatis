package cn.javadog.sd.mybatis.jdbc;

/**
 * @author 余勇
 * @date 2019-12-18 13:59
 *
 * 继承 AbstractSQL，并没有多做什么
 */
public class SQL extends AbstractSQL<SQL> {

  @Override
  public SQL getSelf() {
    return this;
  }

}
