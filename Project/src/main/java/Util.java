import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ShrikeBTMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Util {
    public static void printDot(String filePath, Map<String,Set<String>> map) throws IOException {
        File file = new File(filePath);
        Writer out = new FileWriter(file);
        out.write("digraph myMethod_class {\n");
        for(String key:map.keySet()){
            for(String value:map.get(key)){
                out.write("\"" + key + "\""+ " -> " + "\""+ value + "\";\n");
            }
        }
        out.write("}");
        out.close();
    }
    /**
     * 获得文件的分析域
     * @param dirPath 文件夹名称
     * @return 分析域
     */
    private static AnalysisScope getScope(String... dirPath) throws IOException, InvalidClassFileException {
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
     * 获得图
     * @param dirPath 文件夹名
     * @return 图
     */
    static CHACallGraph getGraph(String... dirPath) throws CancelException, IOException, InvalidClassFileException, ClassHierarchyException {
        AnalysisScope scope = getScope(dirPath);
        ClassHierarchy cha = ClassHierarchyFactory.makeWithRoot(scope);
        Iterable<Entrypoint> eps = new AllApplicationEntrypoints(scope, cha);
        CHACallGraph cg = new CHACallGraph(cha);
        cg.init(eps);
        return cg;
    }

    /**
     * 获得文件里每一行组成的Set
     * @param filePath 文件路径
     * @return 文件里每一行组成的集合
     */
    public static Set<String> getFileSet(String filePath) throws IOException {
        Set<String> res = new HashSet<String>();
        FileReader changeInfoFile = new FileReader(filePath);
        BufferedReader bf = new BufferedReader(changeInfoFile);
        String line = null;
        while ((line = bf.readLine()) != null) {
            if(line.length()==0)continue;
            res.add(line.trim());
        }
        return res;
    }

    public static String getMethodFallName(ShrikeBTMethod method){
        return method.getDeclaringClass().getName().toString() + " " +  method.getSignature();
    }

    public static String getCallSiteFallName(CallSiteReference c){
        return c.getDeclaredTarget().getDeclaringClass().getName().toString() + " " +  c.getDeclaredTarget().getSignature();
    }

    /**
     * 排除不合法的字符
     * @param method
     * @param invalidStr
     * @return 方法是否合法
     */
    public static Boolean isMethodValid(ShrikeBTMethod method,String... invalidStr){
        boolean containFlag = true;
        for(String s:invalidStr){
            if(method.getSignature().contains(s)){
                containFlag=false;
                break;
            }
        }
        return "Application".equals(method.getDeclaringClass().getClassLoader().toString()) &&
                !method.getSignature().contains("<init>") &&
                containFlag;

    }

}
