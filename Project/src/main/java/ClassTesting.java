import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ClassTesting {
    /**
     * 获得方法级 受影响的测试用例
     * @param projectTarget
     * @param changeInfoPath
     * @return 受影响的测试用例
     */
    public static Set<String> getClassResult(String projectTarget, String changeInfoPath) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {

        String testDirPath = projectTarget + "\\test-classes\\net\\mooctest"; // 测试文件夹
        // 存储发生变化的内部类
        Set<String> changeClass = new HashSet<String>();
        // 存储相关的test方法（方法级）
        Set<String> resClass = new HashSet<String>();
        // 读取change_info.txt
        FileReader changeInfoFile = new FileReader(changeInfoPath);
        BufferedReader bf = new BufferedReader(changeInfoFile);
        String line = null;
        while ((line = bf.readLine()) != null) {
            changeClass.add(line.split(" ")[0].trim());
        }
        // 求出受到影响的test方法
        CHACallGraph cg = Util.getGraph(testDirPath);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if(!Util.isMethodValid(method,".<init>()V"))continue;// 去掉init的情况
                for(CallSiteReference c: method.getCallSites()){
                    String className = c.getDeclaredTarget().getDeclaringClass().getName().toString();
                    if(changeClass.contains(className)) {
                        resClass.add(Util.getMethodFallName(method));
                        break;
                    }
                }
            }
        }
        return resClass;
    }

    /**
     * 获得类级别的 dot文件中hashMap
     * @param projectTarget
     * @param changeInfoPath
     * @return
     */
    public static Map<String,Set<String>> getClassMap(String projectTarget, String changeInfoPath) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        String srcDirPath = projectTarget + "\\classes\\net\\mooctest"; // 代码文件夹
        String testDirPath = projectTarget + "\\test-classes\\net\\mooctest"; // 测试文件文件夹
        // 类级映射关系
        Map<String,Set<String>> classMap = new HashMap<String, Set<String>>();

        CHACallGraph cg = Util.getGraph(srcDirPath,testDirPath);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if(!Util.isMethodValid(method,".<init>()V"))continue;// 去掉init的情况
                if(!method.getSignature().contains("mooctest"))continue;        // 只要包含mooctest的情况
                String methodClassName = method.getDeclaringClass().getName().toString();

                for(CallSiteReference c: method.getCallSites()){
                    String className = c.getDeclaredTarget().getDeclaringClass().getName().toString();
                    if(!className.contains("mooctest")||className.contains("$"))continue;
                    if(classMap.containsKey(className)){
                        classMap.get(className).add(methodClassName);
                    }else{
                        Set<String> addedSet = new HashSet<String>();
                        addedSet.add(methodClassName);
                        classMap.put(className,addedSet);
                    }
                }
            }
        }
        return classMap;
    }
}
