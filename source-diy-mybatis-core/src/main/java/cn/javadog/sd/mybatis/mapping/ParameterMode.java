package cn.javadog.sd.mybatis.mapping;

/**
 * 参数模式
 * TODO 网上没有好的说明区别的文档，貌似 INOUT 是针对存储过程的
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
