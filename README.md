# Chinese Set Expansion 中文实体集拓展

### Java version: 10.0.2

### Lucene version: 7.7.1

### maven version：3.3.9

可以根据给定类型的种子，从语料库中分析得出同类型的实体

使用前需要将路径改为自己本地的路径。

首先使用IndexBuilder类对应语料库建立索引。接着再使用ChineseSetExpansion进行实体抽取。

**参考论文：**

*Jiaming Shen⋆, Zeqiu Wu⋆, Dongming Lei, Jingbo Shang, Xiang Ren, Jiawei Han."SetExpan: Corpus-Based Set Expansion via Context Feature Selection and Rank Ensemble"*

**语料库：**

来自于THUNLP资源<http://thuctc.thunlp.org/message>

结果演示：

国家名称抽取：

![avatar](https://github.com/PengWei98/ChineseSetExpansion/blob/master/pic/country.png)

省份名称抽取：

![avatar](https://github.com/PengWei98/ChineseSetExpansion/blob/master/pic/province.png)

公司名称抽取：

![avatar](https://github.com/PengWei98/ChineseSetExpansion/blob/master/pic/company.png)



