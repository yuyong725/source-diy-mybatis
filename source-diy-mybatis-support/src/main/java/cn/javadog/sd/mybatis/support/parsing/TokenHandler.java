package cn.javadog.sd.mybatis.support.parsing;

/**
 * @author 余勇
 * @date 2019年11月29日 20:22:00
 * Token处理器，常用的场景就是占位符的处理
 * note mybatis里这不是一个FunctionalInterface，这里扩展一下，可能意义不大，因为解析一般很复杂，实现类内部应该很多方法
 * todo 视情况删除 @FunctionalInterface标记
 *
 */
@FunctionalInterface
public interface TokenHandler {

	/**
	 * 使用特定的规则处理字符串
	 */
	String handleToken(String content);

}
