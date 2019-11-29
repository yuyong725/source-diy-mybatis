### 解析器模块

#### 删减部分
* `XPathParser`解析器的xml验证
    * 涉及`validation`与`entityResolver`属性
        * `validation`: xml格式校验是否开启，默认开启
        * `entityResolver`: xml文档头部指定的DTD或XSD文件，用于对像xml格式进行校验，但离线情况下下载不下来会出现xml校验失败的情况，通过此类实现使用本地DTD的效果
    * 与主功能无关，因此去掉
