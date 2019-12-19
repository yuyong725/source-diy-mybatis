package cn.javadog.sd.mybatis.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import cn.javadog.sd.mybatis.session.Configuration;
import cn.javadog.sd.mybatis.support.cache.impl.PerpetualCache;
import org.junit.Test;

/**
 * @author 余勇
 * @date 2019年12月19日 14:04:00
 *
 * 测试缓存，包括：
 * 		* configuration 注册、获取 cache
 *
 */
public class CacheTest extends BaseDataTest {

	/**
	 * 缓存注册，应同时注册包括全路径名+短名字两个缓存
	 * 核心逻辑是 {@link Configuration.StrictMap#put(String, Object)}
	 */
	@Test
	public void shouldResolveBothSimpleNameAndFullyQualifiedName() {
		Configuration c = new Configuration();
		final String fullName = "com.mycache.MyCache";
		final String shortName = "MyCache";
		final PerpetualCache cache = new PerpetualCache(fullName);
		c.addCache(cache);
		assertEquals(cache, c.getCache(fullName));
		assertEquals(cache, c.getCache(shortName));
	}

	/**
	 * 缓存注册获取，不认识的就GG
	 */
	@Test(expected=IllegalArgumentException.class)
	public void shouldFailOverToMostApplicableSimpleName() {
		Configuration c = new Configuration();
		final String fullName = "com.mycache.MyCache";
		final String invalidName = "unknown.namespace.MyCache";
		final PerpetualCache cache = new PerpetualCache(fullName);
		c.addCache(cache);
		assertEquals(cache, c.getCache(fullName));
		assertEquals(cache, c.getCache(invalidName));
	}

	/**
	 * 测试缓存短名称的重复
	 */
	@Test
	public void shouldSucceedWhenFullyQualifiedButFailDueToAmbiguity() {
		Configuration c = new Configuration();

		final String name1 = "com.mycache.MyCache";
		final PerpetualCache cache1 = new PerpetualCache(name1);
		c.addCache(cache1);

		final String name2 = "com.other.MyCache";
		final PerpetualCache cache2 = new PerpetualCache(name2);
		c.addCache(cache2);

		final String shortName = "MyCache";

		assertEquals(cache1, c.getCache(name1));
		assertEquals(cache2, c.getCache(name2));

		try {
			c.getCache(shortName);
		} catch (Exception e) {
			log.error(String.format("成功捕获异常:[%s]", e.getMessage()));
			assertTrue(e.getMessage().contains("ambiguous"));
		}
	}

	/**
	 * 统一缓存重复注册
	 */
	@Test
	public void shouldFailToAddDueToNameConflict() {
		Configuration c = new Configuration();
		final String fullName = "com.mycache.MyCache";
		final PerpetualCache cache = new PerpetualCache(fullName);
		try {
			c.addCache(cache);
			c.addCache(cache);
		} catch (Exception e) {
			assertTrue(e.getMessage().contains("already contains value"));
		}
	}

}
