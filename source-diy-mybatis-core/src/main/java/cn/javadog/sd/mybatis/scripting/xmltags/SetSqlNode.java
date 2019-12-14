package cn.javadog.sd.mybatis.scripting.xmltags;

import java.util.Arrays;
import java.util.List;

import cn.javadog.sd.mybatis.session.Configuration;

/**
 * @author 余勇
 * @date 2019-12-14 18:42
 *
 * <set /> 标签的 SqlNode 实现类。用于 UPDATE。
 * 使用示例：
 * <update id="updateAuthorIfNecessary" parameterType="org.apache.ibatis.domain.blog.Author">
 * 	 update Author
 * 	 <set>
 * 			<if test="username != null">username=#{username},</if>
 * 			<if test="password != null">password=#{password},</if>
 * 			<if test="email != null">email=#{email},</if>
 * 			<if test="bio != null">bio=#{bio}</if>
 * 	 </set>
 * 		where id=#{id}
 * </update>
 */
public class SetSqlNode extends TrimSqlNode {

  /**
   * 连接语句的后缀
   */
  private static List<String> suffixList = Arrays.asList(",");

  /**
   * 构造函数
   */
  public SetSqlNode(Configuration configuration, SqlNode contents) {
    // 指定 前缀为 'SET'，最后一个参数是要移除的后缀。因为需要移除到最后一个逗号
    super(configuration, contents, "SET", null, null, suffixList);
  }

}
