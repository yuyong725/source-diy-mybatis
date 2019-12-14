package cn.javadog.sd.mybatis.scripting.defaults;

import cn.javadog.sd.mybatis.mapping.SqlSource;
import cn.javadog.sd.mybatis.scripting.xmltags.XMLLanguageDriver;
import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.exceptions.BuilderException;
import cn.javadog.sd.mybatis.support.parsing.XNode;

/**
 * @author 余勇
 * @date 2019-12-14 14:09
 * @since 3.2.0
 *
 * 继承 XMLLanguageDriver 类，RawSqlSource 语言驱动器实现类，确保创建的 SqlSource 是 RawSqlSource 类.
 *
 * 从 3.2.4 版本之后，默认的 XMLLanguageDriver 就能够识别静态的statements，并创建相应的{@link RawSqlSource}对象。
 * 因此不在需要此类，除非你想去确认真的没有任何动态标签
 */
public class RawLanguageDriver extends XMLLanguageDriver {

  /**
   * 使用 xml 的方式创建SqlSource。
   * 创建的逻辑使用的是父类，这里主要是校验
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, XNode script, Class<?> parameterType) {
    // 调用父类，创建 SqlSource 对象
    SqlSource source = super.createSqlSource(configuration, script, parameterType);
    // 校验创建的是 RawSqlSource 对象
    checkIsNotDynamic(source);
    return source;
  }

  /**
   * 使用 注解 的方式创建SqlSource。
   */
  @Override
  public SqlSource createSqlSource(Configuration configuration, String script, Class<?> parameterType) {
    // 调用父类，创建 SqlSource 对象
    SqlSource source = super.createSqlSource(configuration, script, parameterType);
    // 校验创建的是 RawSqlSource 对象
    checkIsNotDynamic(source);
    return source;
  }

  /**
   * 校验是 RawSqlSource 对象。确认创建的 SqlSource 不是动态的。
   * 其实就是判断下类型而已
   */
  private void checkIsNotDynamic(SqlSource source) {
    if (!RawSqlSource.class.equals(source.getClass())) {
      throw new BuilderException("Dynamic content is not allowed when using RAW language");
    }
  }

}
