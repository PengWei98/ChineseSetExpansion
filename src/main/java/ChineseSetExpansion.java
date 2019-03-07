import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.flexible.standard.parser.StandardSyntaxParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class ChineseSetExpansion {

    static final int maxLength = 4;

    static final int minLength = 2;

    static final String regex = "{" + minLength + "," + maxLength + "}";

    static final int windowSize = 3;

    static HashMap<String, Integer> contexts = new HashMap<String, Integer>();

    static HashSet<String> contextsSet = new HashSet<String>();

    static Set<String> entities = new HashSet<String>(); //实体集

    static Map<String, Integer> undeterminedEntities = new HashMap<String, Integer>(); //待定的实体集，要把出现次数最多的部分添加到最终的实体集里

    static Map<String, String> relation = new HashMap<String, String>();

    public static void search(String indexDir, String q) throws Exception {

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
                    if (contexts.containsKey(context)) {
                        Integer x = contexts.get(context);
                        contexts.put(context, x + 1);
                    } else {
                        contexts.put(context, 1);
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
//                        set.add((String) list.get(j).getKey());
//                        j++;
//                        count++;
                        flag = false;
                        break;
                    }
                }
                if(flag){
                    set.add(entity);
                    count++;
                }
//                set.add((String) list.get(j).getKey());
                j++;
//                count++;
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
                if (!entities.contains(undeterminedEntity)) {
//                    if(undeterminedEntity.indexOf("月日") != -1){
//                        continue;
//                    }
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
        String indexDir = "/Users/pengwei/IdeaProjects/myLucene/THUCNewsIndex";

        entities.add("日本");
        entities.add("法国");
        entities.add("南非");
        entities.add("加拿大");
        entities.add("澳大利亚");

        System.out.println("第0次迭代：" + entities);

        for (int i = 0; i < 10; i++) {
            for (String entity : entities) {
                try {
                    search(indexDir, entity);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            rankMapByValue(contexts, contextsSet, 20, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Integer.compare(o1, o2);
                }
            });
//            System.out.println(contextsSet);
            for (String context : contextsSet) {
                try {
                    searchEntities(indexDir, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            rankMapByValue(undeterminedEntities, entities, 4, new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return Integer.compare(o1, o2);
                }
            });
//            entities.remove("年月日");
            System.out.println("第" + (i + 1) + "次迭代：" + entities);
//            contexts.clear();
            undeterminedEntities.clear();
            relation.clear();
        }
    }
}


