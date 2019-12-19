package cn.javadog.sd.mybatis.support.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author 余勇
 * @date 2019年11月29日 16:58:00
 * xml的节点描述，是对xpath解析出来的node的封装
 *
 * TODO 这个类丢失不少东西
 */
public class XNode {

	/**
	 * xPathParser解析出来的节点
	 */
	private final Node node;

	/**
	 * 节点的name，都是w3c的标准，具体含义不追究
	 */
	private final String name;

	/**
	 * 节点的body
	 */
	private final String body;
	private final Properties attributes;
	private final Properties variables;
	private final XPathParser xpathParser;

	/**
	 * 构造函数
	 */
	public XNode(XPathParser xpathParser, Node node, Properties variables){
		this.xpathParser = xpathParser;
		this.node = node;
		this.name = node.getNodeName();
		this.variables = variables;
		this.attributes = parseAttributes(node);
		this.body = parseBody(node);
	}

	/**
	 * 创建新节点
	 */
	public XNode newXNode(Node node){
		return new XNode(xpathParser, node, variables);
	}

	public String getName() {
		return this.name;
	}

	/**
	 * 获取父节点
	 */
	public XNode getParent() {
		Node parent = node.getParentNode();
		if (parent == null || !(parent instanceof Element)){
			return null;
		}else {
			return new XNode(xpathParser, parent, variables);
		}
	}

	/**
	 * 基于标识符获取 属性
	 * 举例：如下这样的节点：
	 * <employee id="${id_var}">
 	 * 	<height units="ft">5.8</height>
	 * </employee>
	 * 首先获取 XNode node = parser.evalNode("/employee/height")，再调用此方法，返回的额值为：employee[${id_var}]_height
	 */
	public String getValueBasedIdentifier() {
		StringBuilder builder = new StringBuilder();
		XNode current = this;
		while (current != null){
			if (current != this){
				builder.insert(0, "-");
			}
			// 获取节点的'id'属性值，没有则取'value'属性值，依然没有就取'property'属性值，否则返回null
			String value = current.getStringAttribute("id",
				current.getStringAttribute("value",
					current.getStringAttribute("property", null)));
			if (value != null){
				value.replace('.','-');
				builder.insert(0,"]");
				builder.insert(0,value);
				builder.insert(0,"[");
			}
			builder.insert(0, current.getName());

			current = current.getParent();
		}
		return builder.toString();
	}

	/**
	 * 获取指定的属性
	 */
	public String getStringAttribute(String name) {
		return getStringAttribute(name, null);
	}

	/**
	 * 获取指定的属性，为空时使用默认值
	 */
	public String getStringAttribute(String name, String defaultValue) {
		// 注意，这里attributes不会为空，不需要非空判断
		return attributes.getProperty(name, defaultValue);
	}


	/**
	 * 解析node节点的属性
	 * 举例：如下面的xml，'year'、'month'、'day'就是 'birth_date'节点的属性
	 * <birth_date>
	 *     <year>1970</year>
	 *     <month>6</month>
	 *     <day>15</day>
	 * </birth_date>
	 */
	private Properties parseAttributes(Node n) {
		Properties attributes = new Properties();
		NamedNodeMap namedNodeMap = n.getAttributes();
		if (namedNodeMap != null){
			for (int i = 0; i < namedNodeMap.getLength(); i++) {
				Node attribute = namedNodeMap.item(i);
				String value = PropertyParser.parse(attribute.getNodeValue(), variables);
				attributes.put(attribute.getNodeName(), value);
			}
		}
		return attributes;
	}

	/**
	 * 获取节点的内容，对于多级节点，如<a><b>tt</b></a>, 会递归调用直到拿到非空值
	 * todo 具体使用的场景，单看逻辑简单，单对于标签之前的空字符串貌似有风险，等待测试！
	 */
	private String parseBody(Node node) {
		String data = getBodyData(node);
		if (data == null){
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				data = getBodyData(child);
				if (data != null){
					break;
				}
			}
		}
		return data;
	}

	/**
	 * 获取节点的body，节点 node 与 元素 element之间的关系不深究
	 * note 有意思的是node没有非空判断，说明框架逻辑能保证不为空，因此为空判断与否看设计的把控力
	 */
	private String getBodyData(Node child) {
		if (child.getNodeType() == Node.CDATA_SECTION_NODE || child.getNodeType() == Node.TEXT_NODE) {
			String data = ((CharacterData) child).getData();
			//todo 解析占位符
			return data;
		}
		return null;
	}

	/* 调用xpathParser完成相应的解析 */

	public String evalString(String expression) {
		return xpathParser.evalString(expression, node);
	}

	public Boolean evalBoolean(String expression) {
		return xpathParser.evalBoolean(expression, node);
	}

	public Double evalDouble(String expression) {
		return xpathParser.evalDouble(expression, node);
	}

	public List<XNode> evalNodes(String expression) {
		return xpathParser.evalNodes(expression, node);
	}

	public XNode evalNode(String expression) {
		return xpathParser.evalNode(expression, node);
	}

	/**
	 * 获取字节点
	 */
	public List<XNode> getChildren() {
		List<XNode> children = new ArrayList<>();
		NodeList nodeList = node.getChildNodes();
		if (nodeList != null) {
			for (int i = 0, n = nodeList.getLength(); i < n; i++) {
				Node node = nodeList.item(i);
				if (node.getNodeType() == Node.ELEMENT_NODE) {
					children.add(new XNode(xpathParser, node, variables));
				}
			}
		}
		return children;
	}

	/**
	 * 将字节点列表转成Properties
	 */
	public Properties getChildrenAsProperties() {
		Properties properties = new Properties();
		for (XNode child : getChildren()) {
			String name = child.getStringAttribute("name");
			String value = child.getStringAttribute("value");
			if (name != null && value != null) {
				properties.setProperty(name, value);
			}
		}
		return properties;
	}

	/*获取各种属性*/
	public Node getNode() {
		return node;
	}

	public String getStringBody() {
		return getStringBody(null);
	}

	public String getStringBody(String def) {
		if (body == null) {
			return def;
		} else {
			return body;
		}
	}

	public Boolean getBooleanBody() {
		return getBooleanBody(null);
	}

	public Boolean getBooleanBody(Boolean def) {
		if (body == null) {
			return def;
		} else {
			return Boolean.valueOf(body);
		}
	}

	public Integer getIntBody() {
		return getIntBody(null);
	}

	public Integer getIntBody(Integer def) {
		if (body == null) {
			return def;
		} else {
			return Integer.parseInt(body);
		}
	}

	public Long getLongBody() {
		return getLongBody(null);
	}

	public Long getLongBody(Long def) {
		if (body == null) {
			return def;
		} else {
			return Long.parseLong(body);
		}
	}

	public Double getDoubleBody() {
		return getDoubleBody(null);
	}

	public Double getDoubleBody(Double def) {
		if (body == null) {
			return def;
		} else {
			return Double.parseDouble(body);
		}
	}

	public Float getFloatBody() {
		return getFloatBody(null);
	}

	public Float getFloatBody(Float def) {
		if (body == null) {
			return def;
		} else {
			return Float.parseFloat(body);
		}
	}

	public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name) {
		return getEnumAttribute(enumType, name, null);
	}

	public <T extends Enum<T>> T getEnumAttribute(Class<T> enumType, String name, T def) {
		String value = getStringAttribute(name);
		if (value == null) {
			return def;
		} else {
			return Enum.valueOf(enumType, value);
		}
	}

	public Boolean getBooleanAttribute(String name) {
		return getBooleanAttribute(name, null);
	}

	public Boolean getBooleanAttribute(String name, Boolean def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Boolean.valueOf(value);
		}
	}

	public Integer getIntAttribute(String name) {
		return getIntAttribute(name, null);
	}

	public Integer getIntAttribute(String name, Integer def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Integer.parseInt(value);
		}
	}

	public Long getLongAttribute(String name) {
		return getLongAttribute(name, null);
	}

	public Long getLongAttribute(String name, Long def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Long.parseLong(value);
		}
	}

	public Double getDoubleAttribute(String name) {
		return getDoubleAttribute(name, null);
	}

	public Double getDoubleAttribute(String name, Double def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Double.parseDouble(value);
		}
	}

	public Float getFloatAttribute(String name) {
		return getFloatAttribute(name, null);
	}

	public Float getFloatAttribute(String name, Float def) {
		String value = attributes.getProperty(name);
		if (value == null) {
			return def;
		} else {
			return Float.parseFloat(value);
		}
	}

	/**
	 * 调试时作用很大
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("<");
		builder.append(name);
		for (Map.Entry<Object, Object> entry : attributes.entrySet()) {
			builder.append(" ");
			builder.append(entry.getKey());
			builder.append("=\"");
			builder.append(entry.getValue());
			builder.append("\"");
		}
		List<XNode> children = getChildren();
		if (!children.isEmpty()) {
			builder.append(">\n");
			for (XNode node : children) {
				builder.append(node.toString());
			}
			builder.append("</");
			builder.append(name);
			builder.append(">");
		} else if (body != null) {
			builder.append(">");
			builder.append(body);
			builder.append("</");
			builder.append(name);
			builder.append(">");
		} else {
			builder.append("/>");
		}
		builder.append("\n");
		return builder.toString();
	}
}
