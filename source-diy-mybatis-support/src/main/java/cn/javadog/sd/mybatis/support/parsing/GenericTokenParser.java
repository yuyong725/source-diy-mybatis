package cn.javadog.sd.mybatis.support.parsing;

import java.util.logging.Handler;

/**
 * @author 余勇
 * @date 2019年11月29日 22:21:00
 * 通用的Token解析器
 * 这个类不像TokenHandler抽离成一个接口再去实现，因为在mybatis就解析${}占位符用到了
 */
public class GenericTokenParser {

	/**
	 * token的上皮
	 */
	private final String openToken;

	/**
	 * token的下皮
	 */
	private final String closeToken;

	/**
	 * 用来处理去皮后的token的处理器
	 */
	private final TokenHandler handler;

	public GenericTokenParser(String openToken, String closeToken, TokenHandler Handler){
		this.openToken = openToken;
		this.closeToken = closeToken;
		this.handler = Handler;
	}

	/**
	 * 核心解析逻辑，包括多次去皮，如 pre_${username}_${password}, 需要先解析${username}，假如值是Tom，再解析${password}，假如值是123。
	 * 最终的结果就是pre_Tom_123.
	 * note 注意，支持多次去皮，但不支持嵌套去皮，如${username_${password}}, 实际的解析过程是根据 'username_${password' 去匹配
	 * 整体逻辑虽然有一点绕，因为要考虑多种场景，但提供在纸上模拟各种场景的方式，是可以想明白的，考验的是算法的逻辑
	 */
	public String parse(String text) {
		// 空字符串直接返回
		if (text == null || text.isEmpty()) {
			return "";
		}

		// 寻找最初的 openToken 的位置
		int start = text.indexOf(openToken);
		// 找不到直接返回
		if (start == -1){
			return text;
		}

		char[] src = text.toCharArray();
		int offset = 0;
		final StringBuilder builder = new StringBuilder();
		// expression指的去了皮的内容，如text是${name}，那么expression就是name
		StringBuilder expression = null;
		while (start > -1) {
			// 转义字符需要注意，如果openToken前一个字符是'\'，忽略，截取字符串时，角标需要注意
			if (start > 0 && src[start - 1] == '\\') {
				// 添加 offset 和 openToken 之间的内容， 添加到 builder 中
				// note 注意，这里会append(openToken)，因为将openToken当成了一个普通的字符，key可以包含它，比如'${na\${me}',
				//  这时候我们应该去properties里找key是'na${me'的值，也是说遇到转义了的openToken，我们不需要去找对应的closeToken
				builder.append(src, offset, start - offset - 1).append(openToken);
				offset = start + openToken.length();
			}else {
				if (expression == null) {
					expression = new StringBuilder();
				} else  {
					expression.setLength(0);
				}
				// 添加 offset 和 openToken 之间的内容， 添加到 builder 中，与转义字符那种场景一样，只是添加的内容不用考虑转义字符
				builder.append(src, offset, start - offset);
				// 修改 offset
				offset = start + openToken.length();
				// 寻找对应的closeToken
				int end = text.indexOf(closeToken, offset);
				while (end > -1){
					// 同样，先排除转义的场景
					if (end > offset && src[end - 1] == '\\') {
						expression.append(src, offset, end - offset - 1).append(closeToken);
						offset = end + closeToken.length();
						// 继续寻找真正的closeToken
						end = text.indexOf(closeToken, offset);
					} else {
						expression.append(src, offset, end - offset);
						// 修改offset，实际上按照逻辑不需要，因为下面的逻辑会再去使用同样的方式赋值一次
						offset = end + closeToken.length();
						break;
					}
				}
				// 拼接内容
				if (end == -1){
					// 没找到closeToken，直接拼接, 会将openToken也拼接进来!
					builder.append(src, start, src.length - start);
					// 修改offset，实际已经到底了，不会再去找了
					offset = src.length;
				} else {
					// 找到了，就将expression交给TokenHandler去处理
					builder.append(handler.handleToken(expression.toString()));
					// 修改offset，继续往下查找新的openToken
					offset = end + closeToken.length();
				}
			}
			// 继续寻找新的openToken
			start = text.indexOf(openToken, offset);
		}

		// 拼接最后一个closeToken后面的部分
		if (offset < src.length) {
			builder.append(src, offset, src.length - offset);
		}
		return builder.toString();
	}


}
