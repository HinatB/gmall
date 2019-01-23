package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {

    @Reference
    private ListService listService;

    @Reference
    private ManageService manageService;

    @RequestMapping("/list.html")
    public String getList(SkuLsParams skuLsParams, Model model){
        // 设置分页
        skuLsParams.setPageSize(2);

        // 根据参数从es中查找
        SkuLsResult skuLsResult = listService.search(skuLsParams);
        //System.out.println(skuLsResult);
        int totalPages = (int) ((skuLsResult.getTotal() + skuLsParams.getPageSize()-1)/skuLsParams.getPageSize());

        //点击属性时 已经选择的属性值列表
        String urlParam = makeUrlParam(skuLsParams);
        model.addAttribute("urlParam", urlParam);
        // 每一个SkuLsInfo
        List<SkuLsInfo> skuLsInfoList = skuLsResult.getSkuLsInfoList();
        model.addAttribute("skuLsInfoList", skuLsInfoList);


        //面包屑集合
        ArrayList<BaseAttrValue> baseAttrValueArrayList = new ArrayList<>();

        // 平台属性值集合
        List<String> attrValueIdList = skuLsResult.getAttrValueIdList();
        List<BaseAttrInfo> baseAttrInfoList = manageService.getAttrList(attrValueIdList);
        /**
         * 达到点击属性值 对应的属性那一行会消失的效果
         * 在es中查找 filter过滤后 用来过滤的valueId还会被查出来  所以用属性值id查找baseAttrInfoList的话 还会把过滤条件的valueId加进去
         * 所以要把es中查找的valueId集合遍历 如果和skuLsParams中的valueId相等 就删除
         * *********为什么不能直接删除es中查出来的id集合再查数据库************
         */
        // 在放在域中之前删除
        for (Iterator<BaseAttrInfo> iterator = baseAttrInfoList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo = iterator.next();
            //取出每个baseAttrInfo对应的attrValueList
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            // 把每个baseAttrValue的id和参数中的比较
            for (BaseAttrValue baseAttrValue : attrValueList) {
                if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
                    for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                        //把参数中的valueId查出来 逐个对比
                        String valueId = skuLsParams.getValueId()[i];
                        if (baseAttrValue.getId().equals(valueId)){
                            //如果相等 把当前的平台属性去掉
                            iterator.remove();

                            // 点击面包屑（去掉面包屑）时  生成新的url 把原来面包屑的valueId从地址栏上拿掉
                            // 把点击过这个筛选条件之前的url放在面包屑的href中 点击面包屑后返回筛选前的url 达到去掉面包屑的目的
                            // 在生成面包屑的时候就把相应的url放在面包屑中
                            String urlParams = makeUrlParam(skuLsParams, baseAttrValue.getId());

                            // 构建面包屑 把选择的id构建到面包屑中
                            //平台属性 ： 平台属性值
                            BaseAttrValue baseAttrValue1 = new BaseAttrValue();

                            baseAttrValue1.setUrlParam(urlParams);// 点击面包屑时 要跳转的路径
                            baseAttrValue1.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            System.out.println(baseAttrValue1.getValueName());
                            baseAttrValueArrayList.add(baseAttrValue1);
                        }
                    }
                }

            }


        }
        // 分页
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("pageNo", skuLsParams.getPageNo());
        // 把查找的值返回
        model.addAttribute("keyword", skuLsParams.getKeyword());
        model.addAttribute("baseAttrValueArrayList", baseAttrValueArrayList);

        model.addAttribute("baseAttrInfoList", baseAttrInfoList);


        return "list";
    }

    // 点击属性时 已经选择的属性值列表
    private String makeUrlParam(SkuLsParams skuLsParams, String...excludeValueIds) {
        String urlParam = "";
        // http://list.gmall.com/list.html?keyword=小米&catalog3Id=61&valueId=83
        // 拼keyword
        if (skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            urlParam += "keyword=" + skuLsParams.getKeyword();
        }
        //拼Catalog3Id
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){

            if (urlParam.length()>0){
                urlParam += "&";
            }

            urlParam += "catalog3Id=" + skuLsParams.getCatalog3Id();
        }


        if (skuLsParams.getValueId()!=null && skuLsParams.getValueId().length>0){
            // 循环
            for (int i = 0; i < skuLsParams.getValueId().length; i++) {
                 String valueId = skuLsParams.getValueId()[i];
                if (excludeValueIds!=null && excludeValueIds.length>0){
                    //把和valueIds值相同的valueId取消拼接
                    String excludeValueId = excludeValueIds[0];
                    if (valueId.equals(excludeValueId)){
                        //如果相等 取消本次循环 当前valueId就不会拼接到url中
                        continue;
                    }
                }
                 if (urlParam.length()>0){
                     urlParam += "&";
                 }
                 urlParam += "valueId=" + valueId;

            }
        }

        System.out.println(urlParam);
        return urlParam;
    }

}
