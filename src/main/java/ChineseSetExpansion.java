import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChineseSetExpansion {

    static final String indexDir = "/Users/pengwei/IdeaProjects/myLucene/THUCNewsIndex"; //lucene建得索引文件的路径

    static final int maxLength = 3; //实体最大长度，使用时可修改

    static final int minLength = 2; //实体最短长度，使用时可修改

    static final String regex = "{" + minLength + "," + maxLength + "}"; //用于匹配的正则表达式

    static final int windowSize = 3; //滑动窗口大小，使用时可修改

    static final int times = 20; //迭代次数

    static HashMap<String, Integer> undeterminedContexts = new HashMap<String, Integer>();

    static HashSet<String> contextSet = new HashSet<String>();

    static Set<String> entitySet = new HashSet<String>(); //实体集

    static Map<String, Integer> undeterminedEntities = new HashMap<String, Integer>(); //待定的实体集，要把出现次数最多的部分添加到最终的实体集里

    static Map<String, String> relation = new HashMap<String, String>();

    public static void searchContext(String indexDir, String q) throws Exception {

        Directory dir = FSDirectory.open(Paths.get(indexDir)); //获取要查询的路径，也就是索引所在的位置
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);


        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        for (int i = 0; i < q.length(); i++) {
            builder.add(new Term("contents", q.substring(i, i + 1)));
        }
        PhraseQuery query = builder.build();

        TopDocs docs = searcher.search(query, 100);//开始查询，查询前10条数据，将记录保存在docs中

        for (ScoreDoc scoreDoc : docs.scoreDocs) { //取出每条查询结果
            Document doc = searcher.doc(scoreDoc.doc); //scoreDoc.doc相当于docID,根据这个docID来获取文档
//            System.out.println(doc.get("fullPath")); //fullPath是刚刚建立索引的时候我们定义的一个字段
            FileInputStream inputStream = new FileInputStream(doc.get("fullPath"));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));


            String line;
            while ((line = bufferedReader.readLine()) != null) {

                line = line.replace((char) 12288, ' ');
                line = line.replaceAll("\\u00A0", "");
                line = line.replaceAll("[\\pP‘’“”]", "");
                line = line.replaceAll("\\d+", "");
                line = line.replaceAll(" ", "");
//                System.out.println(line);
                int start = line.indexOf(q);
                int end = start + q.length() - 1;
//                System.out.println(start);
//                System.out.println(end);
                if (start != -1) { //start==-1时，表明该实体不是该行子串
                    String context = getContext(q, line, start, end, windowSize);
//                    System.out.println(context);
                    if (undeterminedContexts.containsKey(context)) {
                        Integer x = undeterminedContexts.get(context);
                        undeterminedContexts.put(context, x + 1);
                    } else {
                        undeterminedContexts.put(context, 1);
                    }
                }
            }
        }
        reader.close();
    }

    //返回的上下文的形式：上文+.+下文
    public static String getContext(String q, String line, int start, int end, int size) {
//        String entity;
        String contextAbove;
        String contextBelow;
        if (start < size) {
            contextAbove = new String(line.substring(0, start));
            contextAbove = "^" + contextAbove;
        } else {
            contextAbove = new String(line.substring((start - size), start));
        }

        if (end + size >= line.length()) {
            contextBelow = new String(line.substring(end + 1));
            contextBelow = contextBelow + "$";
        } else {
            contextBelow = new String(line.substring(end + 1, end + 1 + size));
        }
//        System.out.println(contextBelow);
        return contextAbove + "." + regex + "?" + contextBelow;
    }

    public static <K, V> void rankMapByValue(Map<K, V> map, Set<String> set, int top, final Comparator<V> valueComparator) {
        List<Map.Entry<K, V>> list = new ArrayList<Map.Entry<K, V>>(
                map.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return valueComparator.compare(o2.getValue(), o1.getValue());
            }
        });
        int j = 0;
        int count = 0;
        while (count < top) {
            if (j == list.size()) {
                break;
            }
            if (set.contains(list.get(j).getKey())) {
                j++;
            } else {
                boolean flag = true;
                String entity = (String) list.get(j).getKey();
                for(String str : set){
                    if(entity.indexOf(str) != -1){
                        flag = false;
                        break;
                    }
                }
                if(flag){
                    set.add(entity);
                    count++;
                }
                j++;
            }
        }
    }

    public static void searchEntities(String indexDir, String context) throws Exception {
        Directory dir = FSDirectory.open(Paths.get(indexDir)); //获取要查询的路径，也就是索引所在的位置
        IndexReader reader = DirectoryReader.open(dir);
        IndexSearcher searcher = new IndexSearcher(reader);

        String newContext = context.replaceAll("\\.\\" + regex + "\\?", "").replaceAll("\\^", "").replaceAll("\\$", "");
//        System.out.println("newContext:" + newContext);

        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        newContext = newContext.replaceAll("\\s*", "");
        for (int i = 0; i < newContext.length(); i++) {
            String p = ((Character) newContext.charAt(i)).toString();
            builder.add(new Term("contents", p));
        }
        builder.setSlop(maxLength);
        PhraseQuery query = builder.build();



        TopDocs docs = searcher.search(query, 1000);//开始查询，查询前条数据，将记录保存在docs中


        for (ScoreDoc scoreDoc : docs.scoreDocs) { //取出每条查询结果
            Document doc = searcher.doc(scoreDoc.doc); //scoreDoc.doc相当于docID,根据这个docID来获取文档
//            System.out.println(doc.get("fullPath")); //fullPath是刚刚建立索引的时候我们定义的一个字段
            FileInputStream inputStream = new FileInputStream(doc.get("fullPath"));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = line.replace((char) 12288, ' ');
                line = line.replaceAll("\\u00A0", "");
                line = line.replaceAll("[\\pP‘’“”]", "");
                line = line.replaceAll("\\d+", "");

                line = line.replaceAll(" ", "");
                context = context.replaceAll(" ", "");

                getEntities(context, line);
            }
        }
        reader.close();
    }


    public static void getEntities(String context, String line) {
//        System.out.println("context:" + context);
//        System.out.println("line:" + line);
        Pattern r = Pattern.compile(context);
        Matcher m = r.matcher(line);
        String[] contexts = context.split("\\.\\" + regex + "\\?");
        int l1 = contexts[0].length();
        int l2 = contexts[1].length();
        while (m.find()) {
//            System.out.println("I find:" + line);
            int start = m.start() + l1;
            int end = m.end() - l2;
            if (context.startsWith("^")) {
                start--;
            }
            if (context.endsWith("$")) {
                end++;
            }
            int length = line.substring(start, end).length();
            if (length <= maxLength && length >= minLength) {
                String undeterminedEntity = line.substring(start, end);
//                System.out.println(undeterminedEntity);
//                System.out.println(line);
                if (!entitySet.contains(undeterminedEntity)) {
                    if (undeterminedEntities.containsKey(undeterminedEntity)) {
                        Integer count = undeterminedEntities.get(undeterminedEntity);
                        if (!relation.get(undeterminedEntity).equals(context)) {
                            count *= 2;
                            relation.put(undeterminedEntity, context);
//                            System.out.println("HAHAHAHAHAHA:" + undeterminedEntity);
                        }
                        undeterminedEntities.put(undeterminedEntity, count + 1);
                    } else {
                        undeterminedEntities.put(undeterminedEntity, 1);
                        relation.put(undeterminedEntity, context);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        //初识时添加的实体
        entitySet.add("韩国");
        entitySet.add("法国");
        entitySet.add("加拿大");
        entitySet.add("埃及");
        entitySet.add("新西兰");

        System.out.println("第0次迭代：" + entitySet);

        for (int i = 0; i < times; i++) {
            for (String entity : entitySet) {
                try {
                    searchContext(indexDir, entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            rankMapByValue(undeterminedContexts, contextSet, 20, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Integer.compare(o1, o2);
                }
            });
            for (String context : contextSet) {
                try {
                    searchEntities(indexDir, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            rankMapByValue(undeterminedEntities, entitySet, 4, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Integer.compare(o1, o2);
                }
            });
            System.out.println("第" + (i + 1) + "次迭代：" + entitySet);
//            undeterminedContexts.clear();
            undeterminedEntities.clear();
            relation.clear();
        }
    }
}


