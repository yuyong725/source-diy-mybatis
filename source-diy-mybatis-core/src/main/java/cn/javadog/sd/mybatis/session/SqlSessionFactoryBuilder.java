package cn.javadog.sd.mybatis.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Properties;

import cn.javadog.sd.mybatis.builder.xml.XMLConfigBuilder;
import cn.javadog.sd.mybatis.executor.ErrorContext;
import cn.javadog.sd.mybatis.session.defaults.DefaultSqlSessionFactory;
import cn.javadog.sd.mybatis.support.util.ExceptionUtil;

/**
 * Builds {@link SqlSession} instances.
 *
 * @author Clinton Begin
 *
 * MyBatis 的初始化流程的入口
 */
/**
 * @author 余勇
 * @date 2019-12-17 15:56
 * 会话工厂构造器，MyBatis 的初始化流程的入口
 */
public class SqlSessionFactoryBuilder {

  /**
   * 使用 reader 构造工厂
   */
  public SqlSessionFactory build(Reader reader) {
    return build(reader, null, null);
  }

  /**
   * 使用 reader+环境 构造工厂
   */
  public SqlSessionFactory build(Reader reader, String environment) {
    return build(reader, environment, null);
  }

  /**
   * 使用 reader+properties 构造工厂
   */
  public SqlSessionFactory build(Reader reader, Properties properties) {
    return build(reader, null, properties);
  }

  /**
   * 构造 SqlSessionFactory 对象
   *
   * @param reader Reader 对象
   * @param environment 环境
   * @param properties Properties 变量
   * @return SqlSessionFactory 对象
   */
  public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
      // 创建 XMLConfigBuilder 对象
      XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
      // 执行 XML 解析，创建 DefaultSqlSessionFactory 对象
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error building SqlSession.", e);
    } finally {
      // 重置ErrorContext
      ErrorContext.instance().reset();
      try {
        reader.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  /**
   * 使用 inputStream 构造工厂
   */
  public SqlSessionFactory build(InputStream inputStream) {
    return build(inputStream, null, null);
  }

  /**
   * 使用 inputStream+环境 构造工厂
   */
  public SqlSessionFactory build(InputStream inputStream, String environment) {
    return build(inputStream, environment, null);
  }

  /**
   * 使用 inputStream+properties 构造工厂
   */
  public SqlSessionFactory build(InputStream inputStream, Properties properties) {
    return build(inputStream, null, properties);
  }

  /**
   * 使用 inputStream+properties+环境 构造工厂
   */
  public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
    try {
      // 创建 XMLConfigBuilder 对象
      XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
      // 执行 XML 解析，创建 DefaultSqlSessionFactory 对象
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionUtil.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        inputStream.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }

  /**
   * 使用👆解析得到的配置，创建 DefaultSqlSessionFactory
   */
  public SqlSessionFactory build(Configuration config) {
    return new DefaultSqlSessionFactory(config);
  }

}
