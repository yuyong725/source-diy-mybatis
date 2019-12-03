package cn.javadog.sd.mybatis.support.datasource;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author: 余勇
 * @date: 2019-12-02 23:10
 *
 * 工厂接口, 很眼熟的接口
 */
public interface DataSourceFactory {

  /**
   * 设置 DataSource 对象的属性
   *
   * @param props 属性
   */
  void setProperties(Properties props);

  /**
   * 获得 DataSource 对象
   *
   * @return DataSource 对象
   */
  DataSource getDataSource();

}
