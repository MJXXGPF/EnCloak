import javax.swing.filechooser.FileSystemView;

import soot.G;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class UpdateIndex{

	//读取SGXindex文件存入List
    public static List<String> getReaderIndex() throws IOException {
    	G.v().out.println(">>>>>enter get reader index");
        List<String> readerIndex = new ArrayList<>();
        //BufferedReader是可以按行读取文件
        FileInputStream inputStream = new FileInputStream("/tmp/SGXindex");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String str = null;
        while((str = bufferedReader.readLine()) != null)
        {
            readerIndex.add(str);
        }
        //close
        inputStream.close();
        bufferedReader.close();

        return readerIndex;
    }

    //将List中的数据写入SGXindex文件中
    public static void WriterIndex(List<String> readList,String filename) throws IOException {
        File file = new File(filename);
        if(!file.isFile()){
            file.createNewFile();
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
        for(String l:readList){
            bw.write(l+"\r\n");
        }
        bw.close();

    }

    public static void main(String[] args) throws IOException {

        String  filename = "/tmp/testInput";

        List<String> readList = getReaderIndex();
        for(String s:readList){
            System.out.println(s);
        }
        //System.out.println("direct access:"+readList.get(3));
        WriterIndex(readList,filename);

    }
}

