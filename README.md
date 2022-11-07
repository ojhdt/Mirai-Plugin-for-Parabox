# Mirai Plugin for Parabox

[![Release](https://img.shields.io/github/v/release/ojhdt/Mirai-Plugin-for-Parabox)](https://github.com/ojhdt/Mirai-Plugin-for-Parabox/releases)
![stars](https://img.shields.io/github/stars/ojhdt/Mirai-Plugin-for-Parabox)
![license](https://img.shields.io/github/license/ojhdt/Mirai-Plugin-for-Parabox)

## 介绍

本项目为 [Parabox](https://github.com/Parabox-App/Parabox) 的 [Mirai](https://github.com/mamoe/mirai) 扩展。为 Parabox 提供 mirai 消息源接入。使用前请先阅读 Parabox 用户文档。

扩展使用 ``Kotlin`` 编写，基于 ``mirai-core`` 实现。

## 消息类型支持情况

|消息类型|接收|发送|
|-|-|-|
|文本|✓|✓|
|图片（jpg，png，gif）|✓|✓|
|语音|✓|✓|
|文件|✓|✕|
|引用回复|✓|✓|
|@某人|✓|✓|

>``闪照`` 作为普通图片处理。``表情`` 映射为 emoji 后作为普通文本处理。对方消息撤回不作处理。

## 快速开始

1. 前往 [Release](https://github.com/ojhdt/Mirai-Plugin-for-Parabox/releases) 获取最新版本。 
2. 下载对应版本的 Parabox 主端。
3. 配置账号信息，并根据提示完成登陆验证。

若存在问题，请参考 [疑难解答](./FAQ.md)。

## 声明

鉴于项目的特殊性，开发团队可能在任何时间停止更新或删除项目。

## 开源许可

[AGPL-3.0](https://github.com/ojhdt/Mirai-Plugin-for-Parabox/blob/main/LICENSE)