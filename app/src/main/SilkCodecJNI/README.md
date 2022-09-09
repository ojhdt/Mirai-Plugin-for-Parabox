# SilkCodecJNI
SILK语音编解码JNI实现

### 代码参考（复制）

- [silk4j](https://github.com/mzdluo123/silk4j)
- [Silk_v3_decoder](https://github.com/fishCoder/Silk_v3_decoder)
- [**silk-v3-decoder**](https://github.com/kn007/silk-v3-decoder)

### 使用
前往[**Releases**](https://github.com/AsCodeDev/SilkCodecJNI/releases)下载对应系统的二进制库文件即可。

### 编译

如果遇到无法编译的问题，请删除所有**#if EMBEDDED_ARM<**下的内容，这样可以让因为错误配置而无法声明的方法强行声明。（
