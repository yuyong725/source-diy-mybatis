package cn.javadog.sd.mybatis.scripting.defaults;


import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.scripting.xmltags.XMLLanguageDriver;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.parsing.XNode;

/**
 * As of 3.2.4 the default XML language is able to identify static statements
 * and create a {@link RawSqlSource}. So there is no need to use RAW unless you
 * want to make sure that there is not any dynamic tag for any reason.
 * 
 * @since 3.2.0
 * @author Eduardo Macarron
 *
 * 继承 XMLLanguageDriver 类，RawSqlSource 语言驱动器实现类，确保创建的 SqlSource 是 RawSqlSource 类
 */
public class RawLanguageDriver extends XMLLanguageDriver {

  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
    // 调用父类，创建 SqlSource 对象
    SqlSource source = super.createSqlSource(configuration, script, parameterType);
    // 校验创建的是 RawSqlSource 对象
    checkIsNotDynamic(source);
    return source;
  }

  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
    // 调用父类，创建 SqlSource 对象
    SqlSource source = super.createSqlSource(configuration, script, parameterType);
    // 校验创建的是 RawSqlSource 对象
    checkIsNotDynamic(source);
    return source;
  }

  /**
   * 校验是 RawSqlSource 对象
   *
   * @param source 创建的 SqlSource 对象
   */
  private void checkIsNotDynamic(SqlSource source) {
    if (!RawSqlSource.class.equals(source.getClass())) {
      throw new BuilderException("Dynamic content is not allowed when using RAW language");
    }
  }

}
