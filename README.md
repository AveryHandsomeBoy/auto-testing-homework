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
#### 方法级实现思路

##### 1.构建受影响的方法集合

我们定义一个方法映射关系Map，Map结构为<Stirng,Set<String>> key为方法名，value为**调用key方法**的所有方法的集合。（相当于value方法中调用了key方法，因此key方法改变时value中的方法也要被影响）。

把src方法加入分析域并用WALA建图。我们遍历每一个方法，当遍历到方法 f 时，遍历它调用的函数 t1,t2,t3... ,将关系 t1->f 存到hash表中。这样我们就得到了一个伪邻接矩阵（虽然key是用Set来装的）来存调用图。

然后我们遍历所有改变的方法 m1,m2... ，通过Map把Map[m1],Map[m2]中的所有方法（如n1,n2...）都导入受影响的方法集中，再**递归**把Map[n1],Map[n2]导入受影响的方法集中。有重复的方法则停止递归，最终能得到所有受到影响的方法。

##### 2.求出受影响的测试用例

把test的方法加入分析域并用WALA建图。遍历test的方法，若方法中调用了受影响方法集的方法，则将该方法加入受影响的**测试**方法集中。最终求出最后的受影响**测试**方法集。

 #### 类级实现思路

##### 1.构建受影响的类集合

比方法级更简单，只需将变更集(change_info)函数的类存在Set中即可。

##### 2.求出受影响的测试用例

与方法级相同。