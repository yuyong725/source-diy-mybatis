package cn.javadog.sd.mybatis.scripting.xmltags;

import cn.javadog.sd.mybatis.support.io.Resources;
import ognl.DefaultClassResolver;

/**
 * OGNL 类解析器实现类
 *
 * Custom ognl {@code ClassResolver} which behaves same like ognl's
 * {@code DefaultClassResolver}. But uses the {@code Resources}
 * utility class to find the target class instead of {@code Class#forName(String)}. 
 *
 * @author Daniel Guggi 
 *
 * @see <a href='https://github.com/mybatis/mybatis-3/issues/161'>Issue 161</a>
 */
public class OgnlClassResolver extends DefaultClassResolver {

  @Override
  protected Class toClassForName(String className) throws ClassNotFoundException {
    return Resources.classForName(className);
  }

}
