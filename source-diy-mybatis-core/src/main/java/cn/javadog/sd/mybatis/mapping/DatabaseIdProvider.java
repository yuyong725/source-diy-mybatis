package cn.javadog.sd.mybatis.mapping;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Properties;

/**
 * @author 余勇
 * @date 2019-12-13 19:59
 *
 * 数据库标识提供器接口。作用就是可以在xml中，根据数据库类型，选择指定的SQL，如xml中有两个ID相同的select语句，
 * 但数据库标示不同，那么启动的时候，mybatis会根据最终连接的数据库类型，加载指定的sql
 */
public interface DatabaseIdProvider {

  /**
   * 设置属性
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
