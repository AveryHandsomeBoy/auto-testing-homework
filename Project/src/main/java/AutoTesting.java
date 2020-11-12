import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

import java.io.*;
import java.util.*;


public class AutoTesting {
    // 方法级 map
    private static Map<String, Set<String>> methodMap = new HashMap<String, Set<String>>();

    // 存储发生变化的方法
    private static Set<String> changeMethods = new HashSet<String>();

    /**
     * 获得文件的分析域
     * @param dirPath 文件夹名称
     * @return 分析域
     * @throws IOException
     * @throws InvalidClassFileException
     */
    private static AnalysisScope getScope(String... dirPath) throws IOException,InvalidClassFileException{
        // 将分析域存到文件中
        File exFile = new FileProvider().getFile("exclusion.txt");
        AnalysisScope scope = AnalysisScopeReader.readJavaScope(
                "scope.txt", /*Path to scope file*/
                exFile, /*Path to exclusion file*/
                AutoTesting.class.getClassLoader()
        );
        for(String s:dirPath){
            // 把文件夹下的.class文件加入
            File[] files = new File(s).listFiles();
            assert files != null;
            for (File file: files) {
                if(file.getName().endsWith(".class"))
                    scope.addClassFileToScope(ClassLoaderReference.Application, file );
            }
        }
        return scope;
    }

    /**
     * 搜索相关子节点
     * @param fallName
     */
    private static void dfs(String fallName){
        if(!changeMethods.contains(fallName))changeMethods.add(fallName);
        else return;
        if(!methodMap.containsKey(fallName))return;
        for(String s:methodMap.get(fallName)){
            dfs(s);
        }
    }

    /**
     * 获得图
     * @param dirPath 文件夹名
     * @return 图
     * @throws CancelException
     */
    private static CHACallGraph getGraph(String... dirPath) throws CancelException, IOException, InvalidClassFileException, ClassHierarchyException {
        AnalysisScope scope = getScope(dirPath);
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        CHACallGraph cg = new CHACallGraph(cha);
        cg.init(eps);
        return cg;
    }

    /**
     * 获得方法级 受影响的测试用例
     * @param projectTarget
     * @param changeInfoPath
     * @return
     * @throws CancelException
     * @throws ClassHierarchyException
     * @throws InvalidClassFileException
     * @throws IOException
     */
    public static Set<String> getMethodResult(String projectTarget,String changeInfoPath) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        String srcDirPath = projectTarget + "classes\\net\\mooctest"; // 代码文件夹
        String testDirPath = projectTarget + "test-classes\\net\\mooctest"; // 测试文件夹
        // 存储相关的test方法（方法级）
        Set<String> resMethods = new HashSet<String>();
        // 获得方法的文件分析域
        CHACallGraph srcCg = getGraph(srcDirPath);
        // 填充methodMap
        for(CGNode node: srcCg){
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application"
                        .equals(method.getDeclaringClass().getClassLoader().toString())) {
                    String nodeFallName = Util.getMethodFallName(method);
                    for(CallSiteReference c: method.getCallSites()){
                        String fallName = Util.getCallSiteFallName(c);
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
        }
        // 求出最开始受影响的方法
        Set<String> changeMethodInfo = Util.getFileSet(changeInfoPath);
        // 递归遍历，得出所有受影响的方法，存在map中
        for(String s:changeMethodInfo) dfs(s);

        // 求出受到影响的test方法
        CHACallGraph cg = getGraph(testDirPath);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application"
                        .equals(method.getDeclaringClass().getClassLoader().toString())) {
                    if(method.getSignature().contains(".<init>()V"))continue;// 去除init的情况
                    for(CallSiteReference c: method.getCallSites()){
                        String fallName = Util.getCallSiteFallName(c);
                        if(changeMethods.contains(fallName)){
                            resMethods.add(Util.getMethodFallName(method));
                        }
                    }
                }
            }
        }
        return resMethods;
    }

    /**
     * 获得方法级 受影响的测试用例
     * @param projectTarget
     * @param changeInfoPath
     * @return
     * @throws CancelException
     * @throws ClassHierarchyException
     * @throws InvalidClassFileException
     * @throws IOException
     */
    public static Set<String> getClassResult(String projectTarget,String changeInfoPath) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {

        String testDirPath = projectTarget + "test-classes\\net\\mooctest"; // 测试文件夹
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
        CHACallGraph cg = getGraph(testDirPath);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application"
                        .equals(method.getDeclaringClass().getClassLoader().toString())) {
                    if(method.getSignature().contains(".<init>()V"))continue;// 去除init的情况
                    for(CallSiteReference c: method.getCallSites()){
                        String className = c.getDeclaredTarget().getDeclaringClass().getName().toString();
                        if(changeClass.contains(className)) {
                            resClass.add(Util.getMethodFallName(method));
                            break;
                        }
                    }
                }
            }
        }
        return resClass;
    }
    public static Map<String,Set<String>> getMap(String projectTarget,String changeInfoPath) throws CancelException, ClassHierarchyException, InvalidClassFileException, IOException {
        String srcDirPath = projectTarget + "classes\\net\\mooctest"; // 代码文件夹
        String testDirPath = projectTarget + "test-classes\\net\\mooctest"; // 测试文件文件夹
        // 类级映射关系
        Map<String,Set<String>> classMap = new HashMap<String, Set<String>>();

        CHACallGraph cg = getGraph(srcDirPath,testDirPath);
        for(CGNode node: cg) {
            if(node.getMethod() instanceof ShrikeBTMethod) {
                ShrikeBTMethod method = (ShrikeBTMethod) node.getMethod();
                if("Application"
                        .equals(method.getDeclaringClass().getClassLoader().toString())) {
                    if(method.getSignature().contains(".<init>()V"))continue;// 去除init的情况
                    if(method.getSignature().contains("$"))continue;
                    if(!method.getSignature().contains("mooctest"))continue;// 去除init的情况
                    String methodClassName = method.getDeclaringClass().getName().toString();
                    for(CallSiteReference c: method.getCallSites()){
                        String className = c.getDeclaredTarget().getDeclaringClass().getName().toString();
                        if(!className.contains("mooctest"))continue;
                        if(className.contains("$"))continue;
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
        }
        return classMap;
    }

    public static void main(String[] args) throws IOException, ClassHierarchyException, IllegalArgumentException, InvalidClassFileException, CancelException {
        String[] tasks = {"0-CMD","1-ALU","2-DataLog","3-BinaryHeap","4-NextDay","5-More Triangle"};

        String projectName = tasks[0];
        String projectTarget = "F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\target\\";
        String changeInfoPath = "F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\change_info.txt";
        String outputDir = "C:\\Users\\18125\\Desktop\\";
        Set<String> resMethods = getMethodResult(projectTarget,changeInfoPath);
        Set<String> resClass = getClassResult(projectTarget,changeInfoPath);

        // todo 输出.dot文件
        // 输出类
        Map<String,Set<String>> classMap = getMap(projectTarget,changeInfoPath);
        File dotFile = new File(outputDir+"class-cfa.dot");
        Writer out = new FileWriter(dotFile);
        out.write("digraph myClass_class {\n");
        for(String key:classMap.keySet()){
            for(String value:classMap.get(key)){
                out.write("\"" + key + "\""+ " -> " + "\""+ value + "\";\n");
            }
        }
        out.write("}");
        out.close();
        // 输出方法
        dotFile = new File(outputDir+"method-cfa.dot");
        out = new FileWriter(dotFile);
        out.write("digraph myMethod_class {\n");
        for(String key:methodMap.keySet()){
            if(key.contains("java"))continue;
            for(String value:methodMap.get(key)){
                if(value.contains("java"))continue;
                out.write("\"" + key + "\""+ " -> " + "\""+ value + "\";\n");
            }
        }
        out.write("}");
        out.close();
        // 输出方法粒度的结果
        System.out.println("----------------------------------------");
        Set<String> ansMethods = Util.getFileSet("F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\selection-method.txt");
        if(ansMethods.size()!=resMethods.size()) System.out.println("结果数量不对");
        for(String methodSignature:resMethods){
            if(!ansMethods.contains(methodSignature))System.out.println(methodSignature+"不应该在结果里");
        }
        for(String methodSignature:ansMethods){
            if(!resMethods.contains(methodSignature))System.out.println(methodSignature+"应该在结果里");
        }
        // 输出文件
        File file = new File(outputDir+"selection-method.txt");
        out = new FileWriter(file);
        for(String s:resMethods){
            out.write(s+"\n");
        }
        out.close();
        // 输出类粒度的结果
        System.out.println("----------------------------------------");
        Set<String> ansClass = Util.getFileSet("F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\selection-class.txt");
        if(ansClass.size()!=resClass.size()) System.out.println("应该在结果里");
        for(String c:resClass){
            if(!ansClass.contains(c))System.out.println(c+"不应该在结果里");
        }
        for(String c:ansClass){
            if(!resClass.contains(c))System.out.println(c+"应该在结果里");
        }
        // 输出文件
        file = new File(outputDir+"selection-class.txt");
        out = new FileWriter(file);
        for(String s:resClass){
            out.write(s+"\n");
        }
        out.close();
    }

}
