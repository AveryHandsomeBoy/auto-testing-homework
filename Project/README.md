# 自动化测试
### 运行结果
运行命令为
java -jar testSelection.jar -m <project_target> <change_info>

注意：project_target为目录名，change_info为文件位置（即结尾是change_info.txt）。两者均为绝对路径

在projectTarget目录下生成文件
- 若第二个参数是 -m : 生成method-cfa.dot,selection-method.txt
- 若第二个参数是 -c : 生成class-cfa.dot,selection-class.txt

P.S. 
- 本项目打包成jar后，请到jar包/META-INF/MANIFEST.MF文件中检查是否有
Main-Class: AutoTesting这一项，若没有请添加，不然会提示缺少运行主类（缺少项目入口）
- 我本地的jdk地址是C:/Program Files/Java/jdk1.8.0_261 虽然我在jar包里改了jdk位置但是可能还是有问题。
如果遇到问题可以联系一下我，辛苦助教了！

### 基本思路
待定
