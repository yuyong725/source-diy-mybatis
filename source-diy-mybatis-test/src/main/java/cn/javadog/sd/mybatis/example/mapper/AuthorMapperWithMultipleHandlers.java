package cn.javadog.sd.mybatis.example.mapper;


import cn.javadog.sd.mybatis.annotations.Select;
import cn.javadog.sd.mybatis.executor.result.ResultHandler;

public interface AuthorMapperWithMultipleHandlers {

  @Select("select id, username, password, email, bio, favourite_section from author where id = #{id}")
  void selectAuthor(int id, ResultHandler handler1, ResultHandler handler2);

}
