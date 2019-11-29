package cn.javadog.sd.mybatis.support.parsing;

import java.util.Properties;

/**
 * @author 余勇
 * @date 2019年11月29日 19:49:00
 * 占位符解析
 *
 * note
 * 		1. mybatis支持
 */
public class PropertyParser {

	/**
	 * 前缀，相关属性在 mybatis-config.xml 中配置，其实用的好就是四两拨千斤
	 */
	private static final String KEY_PREFIX = "cn.javadog.sd.mybatis.support.parsing.";

	/**
	 * 是否允许默认值
	 * 如 SELECT * FROM ${tableName:users} 中的 users 就是 tableName的默认值
	 */
	private static final String KEY_ENABLE_DEFAULT_VALUE = KEY_PREFIX + "enable-default-value";

	/**
	 * 默认的分隔符，与{@link #KEY_ENABLE_DEFAULT_VALUE} 搭配使用，代表的是{tableName:users}的 ':' 分隔符
	 */
	private static final String KEY_DEFAULT_VALUE_SEPARATOR = KEY_PREFIX + "default-value-separator";

	/**
	 * 默认不支持默认值
	 */
	private static final String ENABLE_DEFAULT_VALUE = "false";

	/**
	 * 默认分割符为 ':'
	 */
	private static final String DEFAULT_VALUE_SEPARATOR = ":";

	/**
	 * 关闭默认构造，不让初始化，只让使用静态方法
	 */
	private PropertyParser(){}

	/**
	 * 真正暴露出来的唯一静态方法，用于将字符串中的占位符替换成properties提供的值
	 * 如 value=${key}, variables=props.setProperty("key", "awesome"); 那么解析后value=awesome
	 */
	public static String parse(String content, Properties variables) {
		// token 处理器
		variableTokenHandler tokenHandler = new variableTokenHandler(variables);
		// token 解析器，先"去皮"，再交给"处理器"处理，"上皮"和"下皮"是写死的
		GenericTokenParser tokenParser = new GenericTokenParser("${", "}", tokenHandler);
		// 交给tokenParser去解析
		return tokenParser.parse(content);
	}

	private static class variableTokenHandler implements TokenHandler {

		/**
		 * 供查找的 properties
		 */
		private final Properties variables;

		/**
		 * 是否支持默认值。默认为{@link #ENABLE_DEFAULT_VALUE}
		 */
		private final boolean enableDefaultValue;

		/**
		 * 分割符。默认为{@link #DEFAULT_VALUE_SEPARATOR}
		 */
		private final String defaultValueSeparator;

		private variableTokenHandler(Properties variables) {
			this.variables = variables;
			this.enableDefaultValue = Boolean.parseBoolean(getPropertyValue(KEY_ENABLE_DEFAULT_VALUE,
				ENABLE_DEFAULT_VALUE));
			this.defaultValueSeparator = getPropertyValue(KEY_DEFAULT_VALUE_SEPARATOR, DEFAULT_VALUE_SEPARATOR);
		}

		private String getPropertyValue(String key, String defaultValue){
			return (variables == null) ? defaultValue : variables.getProperty(key, defaultValue);
		}

		/**
		 * 处理占位符的核心逻辑，看起来很简单，但一定要捋一捋，有点绕的
		 * TODO 为什么最后一步还在外面包了一层，如果进来的已经"去皮"了，那么对于那些没找到匹配的值，为什么不再最后加一层屁
		 */
		@Override
		public String handleToken(String content) {
			// 保证严谨，做非空判断，因为测试可能传空
			if (variables != null) {
				String key = content;
				if (enableDefaultValue) {
					int separatorIndex = content.indexOf(defaultValueSeparator);
					String defaultValue = null;
					// key可能是长度为0的空字符串，避免多一种情形，增加复杂度
					if (separatorIndex >= 0){
						key = content.substring(0, separatorIndex);
						defaultValue = content.substring(separatorIndex + defaultValueSeparator.length());
					}
					// 有默认值则优先替换，不存在时返回默认值;
					// note 这个判断非常重要！虽然properties.getProperty 在找不到时也是返回null，但此处如果找不到又没有默认值，必须出去"加皮"
					if (defaultValue != null) {
						return variables.getProperty(key, defaultValue);
					}
				}
				// 未开启默认值，直接替换
				if (variables.containsKey(key)) {
					return variables.getProperty(key);
				}
			}
			// 没有variables，"加皮"返回
			return "${" + content + "}";
		}
	}
}
