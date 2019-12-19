package cn.javadog.sd.mybatis.example.mapper;

/**
 * @author 余勇
 * @date 2019年12月19日 15:04:00
 * post 表相关操作
 */
public interface PostMapper {

	/**
	 * 查询post的数量
	 */
	int selectCountOfPosts();
}
