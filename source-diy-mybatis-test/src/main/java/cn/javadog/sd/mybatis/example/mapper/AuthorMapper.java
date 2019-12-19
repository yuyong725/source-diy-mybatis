package cn.javadog.sd.mybatis.example.mapper;

import java.util.List;

import cn.javadog.sd.mybatis.example.domain.Author;

/**
 * @author 余勇
 * @date 2019-12-19 14:45
 * author表的mapper
 */
public interface AuthorMapper {

  /**
   * 查所有作者
   */
  List<Author> selectAllAuthors();

}
