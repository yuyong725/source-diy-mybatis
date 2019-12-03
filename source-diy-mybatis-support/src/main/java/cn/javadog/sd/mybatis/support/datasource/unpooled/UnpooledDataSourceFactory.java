package cn.javadog.sd.mybatis.support.datasource.unpooled;

import javax.sql.DataSource;
import java.util.Properties;

import cn.javadog.sd.mybatis.support.datasource.DataSourceFactory;
import cn.javadog.sd.mybatis.support.exceptions.DataSourceException;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.reflection.meta.SystemMetaObject;

/**
 * @author: 余勇
 * @date: 2019-12-02 23:46n Begin
 *
 * 实现 DataSourceFactory 接口，非池化的 DataSourceFactory 实现类
 *
 * UNPOOLED – 这个数据源的实现只是每次被请求时打开和关闭连接。虽然有点慢，但对于在数据库连接可用性方面没有太高要求的简单应用程序来说，是一个很好的选择。
 * 不同的数据库在性能方面的表现也是不一样的，对于某些数据库来说，使用连接池并不重要，这个配置就很适合这种情形。UNPOOLED 类型的数据源仅仅需要配置以下 5 种属性：
 *    driver – 这是 JDBC 驱动的 Java 类的完全限定名（并不是 JDBC 驱动中可能包含的数据源类）。
 *    url – 这是数据库的 JDBC URL 地址。
 *    username – 登录数据库的用户名。
 *    password – 登录数据库的密码。
 *    defaultTransactionIsolationLevel – 默认的连接事务隔离级别。
 *
 * 作为可选项，你也可以传递属性给数据库驱动。要这样做，属性的前缀为“driver.”，例如：
 *    driver.encoding=UTF8
 * 这将通过 DriverManager.getConnection(url,driverProperties) 方法传递值为 UTF8 的 encoding 属性给数据库驱动。
 *
 * note datasource看起来很高级，其实啥也没有，就一些数据库相应的配置信息，从用户的配置的properties获取，然后配置到driver里，getConnection时用到
 */
public class UnpooledDataSourceFactory implements DataSourceFactory {

  private static final String DRIVER_PROPERTY_PREFIX = "driver.";
  private static final int DRIVER_PROPERTY_PREFIX_LENGTH = DRIVER_PROPERTY_PREFIX.length();

  /**
   * DataSource 对象
   */
  protected DataSource dataSource;

  /**
   * 构造
   */
  public UnpooledDataSourceFactory() {
    this.dataSource = new UnpooledDataSource();
  }

  /**
   * 将 properties 的属性，初始化到 dataSource 中
   * SpringBoot大概率传进来
   */
  @Override
  public void setProperties(Properties properties) {
    Properties driverProperties = new Properties();
    // 创建 dataSource 对应的 MetaObject 对象
    MetaObject metaDataSource = SystemMetaObject.forObject(dataSource);
    // 遍历 properties 属性，初始化到 driverProperties 和 MetaObject 中
    for (Object key : properties.keySet()) {
      String propertyName = (String) key;

      if (propertyName.startsWith(DRIVER_PROPERTY_PREFIX)) {
        // 将以"driver."开头的属性初始化到 driverProperties 中
        String value = properties.getProperty(propertyName);
        driverProperties.setProperty(propertyName.substring(DRIVER_PROPERTY_PREFIX_LENGTH), value);
      } else if (metaDataSource.hasSetter(propertyName)) {
        // 不是driver开头，但dataSource有相关的属性
        String value = (String) properties.get(propertyName);
        // 转化属性类型
        Object convertedValue = convertValue(metaDataSource, propertyName, value);
        // 初始化到 metaDataSource 中
        metaDataSource.setValue(propertyName, convertedValue);
      } else {
        // 属性多了直接报错，任性！
        throw new DataSourceException("Unknown DataSource property: " + propertyName);
      }
    }
    // 设置 driverProperties 到 MetaObject 中
    if (driverProperties.size() > 0) {
      metaDataSource.setValue("driverProperties", driverProperties);
    }
  }

  /**
   * 返回 DataSource 对象
   */
  @Override
  public DataSource getDataSource() {
    return dataSource;
  }

  /**
   * 将字符串转化成对应属性的类型，其实就是转换类型
   */
  private Object convertValue(MetaObject metaDataSource, String propertyName, String value) {
    Object convertedValue = value;
    // 获得该属性的 setting 方法的参数类型
    Class<?> targetType = metaDataSource.getSetterType(propertyName);
    // 转化
    if (targetType == Integer.class || targetType == int.class) {
      convertedValue = Integer.valueOf(value);
    } else if (targetType == Long.class || targetType == long.class) {
      convertedValue = Long.valueOf(value);
    } else if (targetType == Boolean.class || targetType == boolean.class) {
      convertedValue = Boolean.valueOf(value);
    }
    // 返回
    return convertedValue;
  }

}
