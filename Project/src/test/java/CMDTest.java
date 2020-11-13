import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.CancelException;
import org.junit.Test;


import java.io.IOException;
import java.util.Set;

import static org.junit.Assert.assertEquals;


public class CMDTest {
    String[] tasks = {"0-CMD","1-ALU","2-DataLog","3-BinaryHeap","4-NextDay","5-More Triangle"};

    String projectName = tasks[3];// 修改索引测试不同的用例

    String dataPath = "F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\";
    String projectTarget = "F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\target\\";
    String changeInfoPath = "F:\\学习资料\\大三上\\自动化测试\\大作业\\ClassicAutomatedTesting\\"+projectName+"\\data\\change_info.txt";

    @Test
    public void testMethod() throws IOException, ClassHierarchyException, InvalidClassFileException, CancelException {
        Set<String> testSet;
        String fileName = "selection-method.txt";
        // 读取文件
        testSet = Util.getFileSet(dataPath+fileName);
        // 判断集合大小和里面的文件是否一致
        Set<String> res = MethodTesting.getMethodResult(projectTarget,changeInfoPath);
        assertEquals(res.size(),testSet.size());
        for(String s:res){
            assert(testSet.contains(s));
        }
    }
    @Test
    public void testClass() throws IOException, ClassHierarchyException, InvalidClassFileException, CancelException {
        Set<String> testSet;
        String fileName = "selection-class.txt";
        // 读取文件
        testSet = Util.getFileSet(dataPath+fileName);
        // 判断集合大小和里面的文件是否一致
        Set<String> res = ClassTesting.getClassResult(projectTarget,changeInfoPath);
        assertEquals(res.size(),testSet.size());
        for(String s:res){
            assert(testSet.contains(s));
        }
    }
}
