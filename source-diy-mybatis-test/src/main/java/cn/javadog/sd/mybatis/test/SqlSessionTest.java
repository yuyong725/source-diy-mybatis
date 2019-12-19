package cn.javadog.sd.mybatis.test;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import cn.javadog.sd.mybatis.example.domain.Author;
import cn.javadog.sd.mybatis.session.SqlSession;
import cn.javadog.sd.mybatis.support.exceptions.TooManyResultsException;
import cn.javadog.sd.mybatis.support.transaction.TransactionIsolationLevel;
import org.junit.Test;

public class SqlSessionTest extends BaseDataTest {

  /**
   * 获取会话
   */
  @Test
  public void shouldOpenAndClose() {
    SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE);
    session.close();
  }

  /**
   * 提交
   */
  @Test
  public void shouldCommitAnUnUsedSqlSession() {
    try (SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE)) {
      session.commit(true);
    }
  }

  /**
   * 回滚
   */
  @Test
  public void shouldRollbackAnUnUsedSqlSession() {
    try (SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE)) {
      session.rollback(true);
    }
  }

  /**
   * 查列表
   */
  @Test
  public void shouldSelectAllAuthors() {
    try (SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE)) {
      List<Author> authors = session.selectList("cn.javadog.sd.mybatis.example.mapper.AuthorMapper.selectAllAuthors");
      assertEquals(2, authors.size());
    }
  }

  /**
   * 查列表，返回一条记录
   */
  @Test(expected= TooManyResultsException.class)
  public void shouldFailWithTooManyResultsException() {
    try (SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE)) {
      session.selectOne("cn.javadog.sd.mybatis.example.mapper.AuthorMapper.selectAllAuthors");
    }
  }

  /**
   * 测试mapkey分组
   */
  @Test
  public void shouldSelectAllAuthorsAsMap() {
    try (SqlSession session = sqlSessionFactory.openSession(TransactionIsolationLevel.SERIALIZABLE)) {
      final Map<Integer,Author> authors = session.selectMap("cn.javadog.sd.mybatis.example.mapper.AuthorMapper.selectAllAuthors", "id");
      assertEquals(2, authors.size());
      for(Map.Entry<Integer,Author> authorEntry : authors.entrySet()) {
        assertEquals(authorEntry.getKey(), (Integer) authorEntry.getValue().getId());
      }
    }
  }

  /**
   * 查询数量
   */
  @Test
  public void shouldSelectCountOfPosts() {
    try (SqlSession session = sqlSessionFactory.openSession()) {
      Integer count = session.selectOne("cn.javadog.sd.mybatis.example.mapper.PostMapper.selectCountOfPosts");
      assertEquals(5, count.intValue());
    }
  }


}
