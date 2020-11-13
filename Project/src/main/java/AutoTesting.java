import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;


import java.io.*;
import java.util.*;


public class AutoTesting {

    public static void main(String[] args) throws IOException, ClassHierarchyException, IllegalArgumentException, InvalidClassFileException, CancelException {
        String[] tasks = {"0-CMD","1-ALU","2-DataLog","3-BinaryHeap","4-NextDay","5-More Triangle"};

//        String type = "class";// 1为类级 2为方法级
//        String projectName = tasks[0];
//        String projectTarget = "F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\target";
//        String changeInfoPath = "F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\change_info.txt";
        if(args.length!=3){System.out.println("参数数量不对");return;}
        String type = (args[0].equals("-c"))?"class":"method";// 1为类级 2为方法级
        String projectTarget = args[1];
        String changeInfoPath = args[2];

        Set<String> resMethods = MethodTesting.getMethodResult(projectTarget,changeInfoPath);
        Set<String> resClass = ClassTesting.getClassResult(projectTarget,changeInfoPath);

        //-------------------------dot---------------------------
        File file;
        Writer out;
        if(type.equals("class")){
            Map<String,Set<String>> classMap = ClassTesting.getClassMap(projectTarget,changeInfoPath);
            file = new File(projectTarget +"\\class-cfa.dot");
            out = new FileWriter(file);
            out.write("digraph myClass_class {\n");
            for(String key:classMap.keySet()){
                for(String value:classMap.get(key)){
                    out.write("\"" + key + "\""+ " -> " + "\""+ value + "\";\n");
                }
            }
            out.write("}");
            out.close();
        }
        else if(type.equals("method")){
            Map<String,Set<String>> dotMethodMap = MethodTesting.getMethodMap(projectTarget,changeInfoPath);
            file = new File(projectTarget +"\\method-cfa.dot");
            out = new FileWriter(file);
            out.write("digraph myMethod_class {\n");
            for(String key:dotMethodMap.keySet()){
                for(String value:dotMethodMap.get(key)){
                    out.write("\"" + key + "\""+ " -> " + "\""+ value + "\";\n");
                }
            }
            out.write("}");
            out.close();
        }
        //-------------------------txt---------------------------
        // 输出方法粒度的结果
//        System.out.println("----------------------------------------");
//        Set<String> ansMethods = Util.getFileSet("F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\selection-method.txt");
//        if(ansMethods.size()!=resMethods.size()) System.out.println("结果数量不对");
//        for(String methodSignature:resMethods){
//            if(!ansMethods.contains(methodSignature))System.out.println(methodSignature+"不应该在结果里");
//        }
//        for(String methodSignature:ansMethods){
//            if(!resMethods.contains(methodSignature))System.out.println(methodSignature+"应该在结果里");
//        }

        // 输出文件
        if(type.equals("method")){
            file = new File(projectTarget +"\\selection-method.txt");
            out = new FileWriter(file);
            for(String s:resMethods){
                out.write(s+"\n");
            }
            out.close();
        }

        // 输出类粒度的结果
//        System.out.println("----------------------------------------");
//        Set<String> ansClass = Util.getFileSet("F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\selection-class.txt");
//        if(ansClass.size()!=resClass.size()) System.out.println("应该在结果里");
//        for(String c:resClass){
//            if(!ansClass.contains(c))System.out.println(c+"不应该在结果里");
//        }
//        for(String c:ansClass){
//            if(!resClass.contains(c))System.out.println(c+"应该在结果里");
//        }

        // 输出文件
        if(type.equals("class")){
            file = new File(projectTarget +"\\selection-class.txt");
            out = new FileWriter(file);
            for(String s:resClass){
                out.write(s+"\n");
            }
            out.close();
        }
    }
}
