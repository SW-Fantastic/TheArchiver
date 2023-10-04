# Archiver

这是一个压缩文件管理器，基于javaFX，CommonCompress，
SevenZipJBinding等开源项目构建的一个能够对压缩文件进行
浏览，解压和修改的压缩文件管理工具。

the project is an archive file manager which can browser , unarchive , create
 archive files.

## 预期的目标

能够实现大部分压缩文件管理的功能。

## 进展

 - [x] 浏览，解压和压缩Zip文件
 - [x] 浏览，解压和压缩7Zip文件
 - [x] 浏览，解压和压缩Tar文件
 - [x] 浏览，解压和压缩Gz文件
 - [x] 浏览，解压和压缩Tar/GZ文件
 - [x] 分卷的Zip压缩文件处理
 - [x] 分卷的7Zip压缩文件处理
 - [ ] 文件预览功能

## 额外说明

你需要下载Release中的sevenzip.zip并放入asset目录才能正常启动项目，
它是7Z的本地类库。