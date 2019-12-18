package cn.javadog.sd.mybatis.session;

/**
 * @author 余勇
 * @date 2019-12-17 15:29
 * 一级缓存的范围
 */
public enum LocalCacheScope {

  /**
   * 会话级
   */
  SESSION,

  /**
   * SQL 语句级
   */
  STATEMENT

}
