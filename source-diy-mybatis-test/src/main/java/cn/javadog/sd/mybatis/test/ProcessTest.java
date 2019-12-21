package cn.javadog.sd.mybatis.test;

import java.util.List;

import cn.javadog.sd.mybatis.example.domain.Author;
import cn.javadog.sd.mybatis.example.mapper.AuthorMapper;
import cn.javadog.sd.mybatis.session.SqlSession;
import org.junit.Test;

/**
 * @author 余勇
 * @date 2019年12月20日 16:26:00
 *
 * 启动流程统计
 */
public class ProcessTest extends BaseDataTest {

	@Test
	public void justForDebugProcess(){
		SqlSession sqlSession = sqlSessionFactory.openSession();

		AuthorMapper mapper = sqlSession.getMapper(AuthorMapper.class);
		List<Author> authors = mapper.selectAllAuthors();
		System.out.println(authors);
	}

}
