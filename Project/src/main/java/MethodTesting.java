import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MethodTesting {
    // 存储发生变化的方法
    private static Set<String> changeMethods = new HashSet<String>();
    // 方法级 map
    private static Map<String, Set<String>> methodMap = new HashMap<String, Set<String>>();

    /**
     * 搜索相关子节点
     * @param fallName
     */
    private static void dfs(String fallName){
        if(!changeMethods.contains(fallName))changeMethods.add(fallName);// 没搜索过的递归搜索
        else return;// 已经搜索过的就不用搜索了
        if(!methodMap.containsKey(fallName))return;
        for(String s:methodMap.get(fallName)){
            dfs(s);
        }
    }

    /**
     * 获得方法级 受影响的测试用例
     * @param projectTarget
     * @param changeInfoPath
     * @return 受影响的测试用例
     */
    public static Set<String> getMethodResult(String projectTarget, String changeInfoPath) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        changeMethods.clear();
        methodMap.clear();
        String srcDirPath = projectTarget + "\\classes\\net\\mooctest"; // 代码文件夹
        String testDirPath = projectTarget + "\\test-classes\\net\\mooctest"; // 测试文件夹
        // 存储相关的test方法（方法级）
        Set<String> resMethods = new HashSet<String>();
        // 获得方法的文件分析域
        CHACallGraph srcCg = Util.getGraph(srcDirPath);
        // 填充methodMap
        for(CGNode node: srcCg){
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if(!Util.isMethodValid(method))continue;
                String nodeFallName = Util.getMethodFallName(method);
                for(CallSiteReference c: method.getCallSites()){
                    String fallName = Util.getCallSiteFallName(c);
                    if(fallName.contains("<init>"))continue;
                    // 加入方法集合
                    if(methodMap.containsKey(fallName)){
                        methodMap.get(fallName).add(nodeFallName);
                    }else{
                        Set<String> addedSet = new HashSet<String>();
                        addedSet.add(nodeFallName);
                        methodMap.put(fallName,addedSet);
                    }
                }
            }
        }

        // 加载最开始受影响的方法
        Set<String> changeMethodInfo = Util.getFileSet(changeInfoPath);
        // 递归遍历，得出所有受影响的方法，存在map中
        for(String s:changeMethodInfo) dfs(s);
        // 求出受到影响的test方法
        CHACallGraph cg = Util.getGraph(testDirPath);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if(!Util.isMethodValid(method,".<init>()V"))continue;// 去掉init的情况
                for(CallSiteReference c: method.getCallSites()){
                    String fallName = Util.getCallSiteFallName(c);
                    if(changeMethods.contains(fallName)){
                        resMethods.add(Util.getMethodFallName(method));
                    }
                }
            }
        }
        return resMethods;
    }



    /**
     * 获得类级别的 dot文件中hashMap
     * @param projectTarget
     * @param changeInfoPath
     * @return
     */
    public static Map<String,Set<String>> getMethodMap(String projectTarget, String changeInfoPath) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        String srcDirPath = projectTarget + "\\classes\\net\\mooctest"; // 代码文件夹
        String testDirPath = projectTarget + "\\test-classes\\net\\mooctest"; // 测试文件文件夹
        // 类级映射关系
        Map<String,Set<String>> dotMethodMap = new HashMap<String, Set<String>>();

        CHACallGraph cg = Util.getGraph(srcDirPath,testDirPath);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if(!Util.isMethodValid(method))continue;
                String nodeMethodName = method.getSignature();
                for(CallSiteReference c: method.getCallSites()){
                    String fallName = c.getDeclaredTarget().getSignature();
                    if(!fallName.contains("mooctest")||fallName.contains("<init>"))continue;

                    if(dotMethodMap.containsKey(fallName)){
                        dotMethodMap.get(fallName).add(nodeMethodName);
                    }else{
                        Set<String> addedSet = new HashSet<String>();
                        addedSet.add(nodeMethodName);
                        dotMethodMap.put(fallName,addedSet);
                    }
                }
            }
        }
        return dotMethodMap;
    }
}
