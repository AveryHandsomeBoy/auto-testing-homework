import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ShrikeBTMethod;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Util {
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

}
