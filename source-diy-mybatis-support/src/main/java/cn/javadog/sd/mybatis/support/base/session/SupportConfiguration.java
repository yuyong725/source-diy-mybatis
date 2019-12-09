package cn.javadog.sd.mybatis.support.base.session;

import cn.javadog.sd.mybatis.support.base.mapping.IMappedStatement;
import cn.javadog.sd.mybatis.support.cache.Cache;
import cn.javadog.sd.mybatis.support.reflection.factory.ObjectFactory;
import cn.javadog.sd.mybatis.support.reflection.meta.MetaObject;
import cn.javadog.sd.mybatis.support.type.TypeAliasRegistry;
import cn.javadog.sd.mybatis.support.type.TypeHandlerRegistry;

/**
 * @author 余勇
 * @date 2019年12月09日 18:20:00
 *
 * Configuration抽象接口，用于抽离相关行为给 基础支持层 和 核心处理层 使用
 */
public interface SupportConfiguration {

	/**
	 * 获取对象工厂
	 */
	ObjectFactory getObjectFactory();

	/**
	 * 获取类型别名注册表
	 */
	TypeAliasRegistry getTypeAliasRegistry();

	/**
	 * 获取类型处理器注册表
	 */
	TypeHandlerRegistry getTypeHandlerRegistry();

	/**
	 * 是否有指定statementId的MappedStatement
	 */
	boolean hasStatement(String statementId);

	/**
	 * 获取指定statementId的MappedStatement
	 */
	IMappedStatement getMappedStatement(String statementId);

	/**
	 * 是否开启使用真实的参数名
	 */
	boolean isUseActualParamName();

	/**
	 * 创建对应的MetaObject
	 */
	MetaObject newMetaObject(Object collection);

	/**
	 * 获取指定namespace的cache
	 */
	Cache getCache(String namespace);

	/**
	 * 指定资源能否找到
	 */
	boolean isResourceLoaded(String resource);

	/**
	 * 添加加载过的资源
	 */
	void addLoadedResource(String resource);

	/**
	 * 添加缓存
	 */
	void addCache(Cache cache);
}
