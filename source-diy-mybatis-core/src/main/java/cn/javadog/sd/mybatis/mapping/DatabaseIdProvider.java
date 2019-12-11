package cn.javadog.sd.mybatis.mapping;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Should return an id to identify the type of this database.
 * That id can be used later on to build different queries for each database type
 * This mechanism enables supporting multiple vendors or versions
 * 
 * @author Eduardo Macarron
 *
 * 数据库标识提供器接口
 */
public interface DatabaseIdProvider {

  /**
   * 设置属性
   *
   * @param p Properties 对象
   */
  void setProperties(Properties p);

  /**
   * 获得数据库标识
   *
   * @param dataSource 数据源
   * @return 数据库标识
   * @throws SQLException 当 DB 发生异常时
   */
  String getDatabaseId(DataSource dataSource) throws SQLException;
}
