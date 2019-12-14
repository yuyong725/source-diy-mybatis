package cn.javadog.sd.mybatis.mapping;

/**
 * @author 余勇
 * @date 2019-12-13 22:21
 * 参数模式。OUT、 INOUT 是针对存储过程的，当前框架使用不到
 */
public enum ParameterMode {

  /**
   * 输入
   */
  IN,

  /**
   * 输出
   */
  OUT,

  /**
   * IN + OUT
   */
  INOUT
}
