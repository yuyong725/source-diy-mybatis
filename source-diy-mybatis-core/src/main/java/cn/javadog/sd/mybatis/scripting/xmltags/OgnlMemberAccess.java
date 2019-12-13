package cn.javadog.sd.mybatis.scripting.xmltags;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Member;
import java.util.Map;

import cn.javadog.sd.mybatis.support.reflection.Reflector;
import ognl.MemberAccess;

/**
 *
 * 实现 ognl.MemberAccess 接口，OGNL 成员访问器实现类
 * The {@link MemberAccess} class that based on <a href=
 * 'https://github.com/jkuhnert/ognl/blob/OGNL_3_2_1/src/java/ognl/DefaultMemberAccess.java'>DefaultMemberAccess</a>.
 *
 * @author Kazuki Shimizu
 * @since 3.5.0
 *
 * @see <a href=
 *      'https://github.com/jkuhnert/ognl/blob/OGNL_3_2_1/src/java/ognl/DefaultMemberAccess.java'>DefaultMemberAccess</a>
 * @see <a href='https://github.com/jkuhnert/ognl/issues/47'>#47 of ognl</a>
 */
class OgnlMemberAccess implements MemberAccess {

  /**
   * 是否可以修改成员的可访问
   */
  private final boolean canControlMemberAccessible;

  OgnlMemberAccess() {
    this.canControlMemberAccessible = Reflector.canControlMemberAccessible();
  }

  @Override
  public Object setup(Map context, Object target, Member member, String propertyName) {
    Object result = null;
    // 判断是否可以修改
    if (isAccessible(context, target, member, propertyName)) {
      AccessibleObject accessible = (AccessibleObject) member;
      // 不可访问，则设置为可访问
      if (!accessible.isAccessible()) {
        // 标记原来是不可访问的
        result = Boolean.FALSE;
        // 修改可访问
        accessible.setAccessible(true);
      }
    }
    return result;
  }

  @Override
  public void restore(Map context, Object target, Member member, String propertyName,
                      Object state) {
    if (state != null) {
      // 修改为原来的可访问
      ((AccessibleObject) member).setAccessible(((Boolean) state));
    }
  }

  @Override
  public boolean isAccessible(Map context, Object target, Member member, String propertyName) {
    return canControlMemberAccessible;
  }

}
