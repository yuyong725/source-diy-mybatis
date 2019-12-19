package cn.javadog.sd.mybatis.example.mapper;


import cn.javadog.sd.mybatis.example.domain.Author;

public interface CachedAuthorMapper {
    Author selectAllAuthors();
    Author selectAuthorWithInlineParams(int id);
    void insertAuthor(Author author);
    boolean updateAuthor(Author author);
    boolean deleteAuthor(int id);
}
