package cn.javadog.sd.mybatis.mapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;

/**
 * Vendor DatabaseId provider
 * 
 * It returns database product name as a databaseId
 * If the user provides a properties it uses it to translate database product name
 * key="Microsoft SQL Server", value="ms" will return "ms" 
 * It can return null, if no database product name or 
 * a properties was specified and no translation was found 
 * 
 * @author Eduardo Macarron
 *
 *
 */
/**
 * @author 余勇
 * @date 2019-12-13 20:14
 *
 * 实现 DatabaseIdProvider 接口，供应商数据库标识提供器实现类。
 * 主要作用就是返回数据库的标示，当然，如果你提供类一个别名映射，它会拿厂商返回的标示去别名映射里面找别名。
 * 如 厂商返回的是"Microsoft SQL Server", 而命名映射里有那个key，对应的value是"ms"，那么就会返回 "ms"
 */
public class VendorDatabaseIdProvider implements DatabaseIdProvider {

  /**
   * 日志
   */
  private static final Log log = LogFactory.getLog(VendorDatabaseIdProvider.class);

  /**
   * 别名properties
   */
  private Properties properties;

  /**
   * 获取指定数据库的标示
   */
  @Override
  public String getDatabaseId(DataSource dataSource) {
    if (dataSource == null) {
      throw new NullPointerException("dataSource cannot be null");
    }
    try {
      // 获得数据库标识
      return getDatabaseName(dataSource);
    } catch (Exception e) {
      log.error("Could not get a databaseId from dataSource", e);
    }
    return null;
  }

  /**
   * 设置 properties
   */
  @Override
  public void setProperties(Properties p) {
    this.properties = p;
  }

  /**
   * 获取指定数据库的名称，Properties中有别名就会替换
   */
  private String getDatabaseName(DataSource dataSource) throws SQLException {
    // 获得数据库产品名
    String productName = getDatabaseProductName(dataSource);
    if (this.properties != null) {
      for (Map.Entry<Object, Object> property : properties.entrySet()) {
        // 如果产品名包含 KEY ，则返回对应的  VALUE
        if (productName.contains((String) property.getKey())) {
          return (String) property.getValue();
        }
      }
      // 有 properties，但没匹配到，就返回null
      return null;
    }
    // 不存在 properties ，则直接返回
    return productName;
  }

  /**
   * 获得数据库产品名
   */
  private String getDatabaseProductName(DataSource dataSource) throws SQLException {
    Connection con = null;
    try {
      // 获得数据库连接
      con = dataSource.getConnection();
      // 获得数据库产品名
      DatabaseMetaData metaData = con.getMetaData();
      return metaData.getDatabaseProductName();
    } finally {
      if (con != null) {
        try {
          con.close();
        } catch (SQLException e) {
          // 无视
        }
      }
    }
  }
  
}
