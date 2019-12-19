package cn.javadog.sd.mybatis.example.mapper;

import cn.javadog.sd.mybatis.annotations.Select;
import cn.javadog.sd.mybatis.session.RowBounds;

public interface AuthorMapperWithRowBounds {

  @Select("select id, username, password, email, bio, favourite_section from author where id = #{id}")
  void selectAuthor(int id, RowBounds bounds1, RowBounds bounds2);

}
