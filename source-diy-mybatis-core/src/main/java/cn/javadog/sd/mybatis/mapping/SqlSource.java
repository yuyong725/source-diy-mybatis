package cn.javadog.sd.mybatis.mapping;

/**
 *
 *
 * Represents the content of a mapped statement read from an XML file or an annotation. 
 * It creates the SQL that will be passed to the database out of the input parameter received from the user.
 *
 * @author Clinton Begin
 */
/**
 * @author 余勇
 * @date 2019-12-13 20:44
 * SQL 来源接口。它代表从 Mapper XML 或方法注解上，读取的一条 SQL 内容。
 * 它根据用户，也就是程序员调用时提供的参数，返回一个 SQL对象，就是 BoundSql
 */
public interface SqlSource {

  /**
   * 根据传入的参数对象，返回 BoundSql 对象
   *
   * @param parameterObject 参数对象
   * @return BoundSql 对象
   */
  BoundSql getBoundSql(Object parameterObject);

}
