package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.DocumentResult;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ListServiceImpl implements ListService{

    @Autowired
    private JestClient jestClient;

    public static final String ES_INDEX="gmall";

    public static final String ES_TYPE="SkuInfo";

    @Override
    public void saveSkuLsInfo(SkuLsInfo skuLsInfo) {

        //保存数据到es中
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();
        try {
            DocumentResult documentResult = jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //创建dsl语句
        String query=makeQueryStringForSearch(skuLsParams);
        //进行查询
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //处理返回值
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);

        return skuLsResult;
    }

    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();

        // 依次注入值
        // 总条数--------------------------------------------
        skuLsResult.setTotal(searchResult.getTotal());
        // 总页数--------------------------------------------
        Long totalPages = (searchResult.getTotal()+skuLsParams.getPageSize()-1)/skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
        // 属性值id集合-------------------------------------------
        List<String> attrValueList = new ArrayList<>();
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        if (buckets!=null && buckets.size()>0){
            for (TermsAggregation.Entry bucket : buckets) {
                String valueId = bucket.getKey();
                attrValueList.add(valueId);
            }
        }
        skuLsResult.setAttrValueIdList(attrValueList);
        // SkuLsInfo集合--------------------------------------------
        ArrayList<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        // 从es中得到skuLsInfo 的结果集 先给skuLsInfoList 赋值
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        if (hits!=null && hits.size()>0){
            for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
                SkuLsInfo skuLsInfo = hit.source;
                // 将skuLsInfo 中skuName 换成高亮的skuName
                if (hit.highlight!=null && hit.highlight.size()>0){
                    List<String> list = hit.highlight.get("skuName");
                    // 高亮名称
                    String skuNameHL = list.get(0);
                    skuLsInfo.setSkuName(skuNameHL);
                }
                skuLsInfoList.add(skuLsInfo);
            }
        }

        skuLsResult.setSkuLsInfoList(skuLsInfoList);

        return skuLsResult;
    }

    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        // 构建查询器
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        // 创建bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        // 创建一个查询条件
        // skuName匹配
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            // must --- match
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            // 将must 添加到 bool
            boolQueryBuilder.must(matchQueryBuilder);
        }
        // 设置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        // 设置高亮的字段，以及前后缀
        highlightBuilder.field("skuName");
        // 将设置好的高亮对象放入到查询器中
        highlightBuilder.preTags("<span style='color:red'>");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlight(highlightBuilder);

        // 创建过滤条件 三级分类ID
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            // filter -- term
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }

        // 创建过滤条件 平台属性值ID
        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环遍历
            for (String valueId : skuLsParams.getValueId()) {
                // filter -- term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);

            }
        }

        // 设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.ASC);
        // 设置分页
        int from = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());
        // 聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");

        // 将聚合设置的字段放入查询构造器
        searchSourceBuilder.aggregation(groupby_attr);
        // 调用方法 query
        searchSourceBuilder.query(boolQueryBuilder);
        String query = searchSourceBuilder.toString();

        System.out.println("query: " + query);

        return query;
    }

}
