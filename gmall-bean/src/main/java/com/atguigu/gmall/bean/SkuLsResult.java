package com.atguigu.gmall.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 从es中查找到的数据的结果集
 */
public class SkuLsResult implements Serializable {
    /**
     *
     */
    List<SkuLsInfo> skuLsInfoList;
    /**
     *总条数
     */
    long total;
    /**
     *总页数
     */
    long totalPages;
    /**
     *平台属性值集合
     */
    List<String> attrValueIdList;

    public List<SkuLsInfo> getSkuLsInfoList() {
        return skuLsInfoList;
    }

    public void setSkuLsInfoList(List<SkuLsInfo> skuLsInfoList) {
        this.skuLsInfoList = skuLsInfoList;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }

    public List<String> getAttrValueIdList() {
        return attrValueIdList;
    }

    public void setAttrValueIdList(List<String> attrValueIdList) {
        this.attrValueIdList = attrValueIdList;
    }
}
