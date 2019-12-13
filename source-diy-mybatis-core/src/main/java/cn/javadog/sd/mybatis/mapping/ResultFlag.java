package cn.javadog.sd.mybatis.mapping;

/**
 * @author: 余勇
 * @date: 2019-12-11 20:55
 *
 * ResultMap标签下的两个属性。
 * 这两个标签与 <result /> 标签效果一致。
 */
public enum ResultFlag {

  /**
   * <id /> 标签
   */
  ID,

  /**
   * <constructor /> 标签
   * 与<result />区别在于，一个是使用构造器，而一个使用的是set方法
   */
  CONSTRUCTOR
}
