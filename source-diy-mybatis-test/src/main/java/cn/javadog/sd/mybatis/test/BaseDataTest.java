package cn.javadog.sd.mybatis.test;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import cn.javadog.sd.mybatis.jdbc.ScriptRunner;
import cn.javadog.sd.mybatis.session.SqlSessionFactory;
import cn.javadog.sd.mybatis.session.SqlSessionFactoryBuilder;
import cn.javadog.sd.mybatis.support.datasource.pooled.PooledDataSource;
import cn.javadog.sd.mybatis.support.datasource.unpooled.UnpooledDataSource;
import cn.javadog.sd.mybatis.support.io.Resources;
import cn.javadog.sd.mybatis.support.logging.Log;
import cn.javadog.sd.mybatis.support.logging.LogFactory;
import org.junit.BeforeClass;

/**
 * @author 余勇
 * @date 2019-12-19 14:01
 * 基础类，加载测试
 */
public abstract class BaseDataTest {

  /**
   * 日志打印器
   */
  protected static Log log = LogFactory.getLog(BaseDataTest.class);

  /**
   * derby数据库的配置
   */
  public static final String BLOG_PROPERTIES = "blog/blog-derby.properties";
  public static final String BLOG_DDL = "blog/blog-derby-schema.sql";
  public static final String BLOG_DATA = "blog/blog-derby-dataload.sql";

  /**
   * session 工厂
   */
  protected static SqlSessionFactory sqlSessionFactory;

  /**
   * 连接数据库，初始化 SqlSessionFactory
   */
  @BeforeClass
  public static void setup() throws Exception {
    log.debug("创建blog数据库资源");
    createBlogDataSource();
    log.debug("创建blog数据库资源完毕，开始解析MapperConfig.xml");
    final String resource = "blog/MapperConfig.xml";
    final Reader reader = Resources.getResourceAsReader(resource);
    sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader);
    log.debug("SqlSessionFactory构建完毕");
  }

  /**
   * 创建非池化的datasource
   */
  public static UnpooledDataSource createUnpooledDataSource(String resource) throws IOException {
    Properties props = Resources.getResourceAsProperties(resource);
    UnpooledDataSource ds = new UnpooledDataSource();
    ds.setDriver(props.getProperty("driver"));
    ds.setUrl(props.getProperty("url"));
    ds.setUsername(props.getProperty("username"));
    ds.setPassword(props.getProperty("password"));
    return ds;
  }

  /**
   * 创建池化的datasource
   */
  public static PooledDataSource createPooledDataSource(String resource) throws IOException {
    Properties props = Resources.getResourceAsProperties(resource);
    PooledDataSource ds = new PooledDataSource();
    ds.setDriver(props.getProperty("driver"));
    ds.setUrl(props.getProperty("url"));
    ds.setUsername(props.getProperty("username"));
    ds.setPassword(props.getProperty("password"));
    return ds;
  }

  /**
   * 跑脚本
   */
  public static void runScript(DataSource ds, String resource) throws IOException, SQLException {
    Connection connection = ds.getConnection();
    try {
      ScriptRunner runner = new ScriptRunner(connection);
      runner.setAutoCommit(true);
      runner.setStopOnError(false);
      runner.setLogWriter(null);
      runner.setErrorLogWriter(null);
      runScript(runner, resource);
    } finally {
      connection.close();
    }
  }

  /**
   * 跑脚本
   */
  public static void runScript(ScriptRunner runner, String resource) throws IOException, SQLException {
    Reader reader = Resources.getResourceAsReader(resource);
    try {
      runner.runScript(reader);
    } finally {
      reader.close();
    }
  }

  /**
   * 创建 blog 数据库及数据
   */
  public static DataSource createBlogDataSource() throws IOException, SQLException {
    log.debug("=====> 创建数据库资源");
    DataSource ds = createUnpooledDataSource(BLOG_PROPERTIES);
    log.debug("=====> 创建blog数据库");
    runScript(ds, BLOG_DDL);
    log.debug("=====> 添加数据到blog");
    runScript(ds, BLOG_DATA);
    return ds;
  }
}
