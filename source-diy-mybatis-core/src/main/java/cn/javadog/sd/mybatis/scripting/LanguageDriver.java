package cn.javadog.sd.mybatis.scripting;

import cn.javadog.sd.mybatis.executor.parameter.ParameterHandler;
import cn.javadog.sd.mybatis.mapping.BoundSql;
import cn.javadog.sd.mybatis.mapping.MappedStatement;
import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.scripting.defaults.DefaultParameterHandler;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.parsing.XNode;

/**
 * @author 余勇
 * @date 2019-12-14 13:16
 * 语言驱动接口
 */
public interface LanguageDriver {

  /**
   * 创建 ParameterHandler 对象。这个对象会将方法真正的参数值传给 JDBC 的 statement
   *
   * 默认实现 {@link DefaultParameterHandler}
   */
  ParameterHandler createParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql);

  /**
   * 创建 SqlSource 对象，从 Mapper XML 配置的 Statement 标签中，即 <select /> 等。
   * MyBatis启动的时候，当读取 mappedStatement 就会调用
   *
   * @param parameterType 输入的参数类型。从mapper接口中解析的，或者 <parameterMap />标签，TODO 多个参数，会变成 paramMap 吗？
   */
  SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType);

  /**
   * 创建 SqlSource 对象，从方法注解配置，即 @Select 等。
   * MyBatis启动的时候，当读取 mappedStatement 就会调用
   *
   * @param script 注解上的语句
   */
  SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType);

}
