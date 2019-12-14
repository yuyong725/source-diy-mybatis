package cn.javadog.sd.mybatis.scripting.xmltags;

import cn.javadog.sd.mybatis.support.io.Resources;
import ognl.DefaultClassResolver;

/**
 * @author 余勇
 * @date 2019-12-14 22:37
 *
 * OGNL 类解析器实现类，不懂不深究
 * @see <a href='https://github.com/mybatis/mybatis-3/issues/161'>Issue 161</a>
 */
public class OgnlClassResolver extends DefaultClassResolver {

  /**
   * 查找指定类
   */
  @Override
  protected Class toClassForName(String className) throws ClassNotFoundException {
    return Resources.classForName(className);
  }

}
