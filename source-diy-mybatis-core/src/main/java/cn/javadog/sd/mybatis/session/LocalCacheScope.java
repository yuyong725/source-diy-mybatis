package cn.javadog.sd.mybatis.session;

/**
 * @author Eduardo Macarron
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
