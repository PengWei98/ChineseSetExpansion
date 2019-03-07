import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;

/**
 * 建立索引的类
 *
 */
public class IndexBuilder{

    private IndexWriter writer; //写索引实例

    //构造方法，实例化IndexWriter
    public IndexBuilder(String indexDir) throws Exception {
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        Analyzer analyzer = new StandardAnalyzer(); //标准分词器，会自动去掉空格啊，is a the等单词
        IndexWriterConfig config = new IndexWriterConfig(analyzer); //将标准分词器配到写索引的配置中
//        IndexWriterConfig config = new IndexWriterConfig();
        writer = new IndexWriter(dir, config); //实例化写索引对象
    }

    //关闭写索引
    public void close() throws Exception {
        writer.close();
    }


    public int indexAll(String dataDir) throws Exception {
        File[] files = new File(dataDir).listFiles(); //获取该路径下的所有文件
        for(File file : files) {
            if(file.getName().endsWith(".txt")){
                indexFile(file); //调用下面的indexFile方法，对每个文件进行索引
            }
        }
        int num = writer.numDocs();
        System.out.println(num);
        return num; //返回索引的文件数
    }


    //索引指定的文件
    private void indexFile(File file) throws Exception {
//        System.out.println("索引文件的路径：" + file.getCanonicalPath());
        Document doc = getDocument(file); //获取该文件的document
        writer.addDocument(doc); //调用下面的getDocument方法，将doc添加到索引中
    }

    //获取文档，文档里再设置每个字段，就类似于数据库中的一行记录
    private Document getDocument(File file) throws Exception{
        Document doc = new Document();
        //添加字段
        doc.add(new TextField("contents", new FileReader(file))); //添加内容
        doc.add(new TextField("fileName", file.getName(), Field.Store.YES)); //添加文件名，并把这个字段存到索引文件里 YES表示储存该原始值，NO表示不储存原始值
        doc.add(new TextField("fullPath", file.getCanonicalPath(), Field.Store.YES)); //添加文件路径
        return doc;
    }

    public static void main(String[] args) {
        String indexDir = "/Users/pengwei/IdeaProjects/myLucene/THUCNewsIndex"; //将索引保存到的路径
        IndexBuilder indexer = null;
        try {
            indexer = new IndexBuilder(indexDir);
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/财经"); //语料库的路径
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/彩票");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/房产");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/股票");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/家居");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/教育");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/科技");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/社会");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/时尚");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/时政");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/体育");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/星座");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/游戏");
            indexer.indexAll("/Users/pengwei/IdeaProjects/myLucene/THUCNews/娱乐");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                indexer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}