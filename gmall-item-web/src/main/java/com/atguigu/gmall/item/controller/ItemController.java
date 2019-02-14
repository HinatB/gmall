package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;

@Controller
public class ItemController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;


    @RequestMapping("{skuId}.html")
    //@LoginRequire(autoRedirect=true)
    public String skuInfoPage(@PathVariable("skuId") String skuId, Model model){
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        List<SkuSaleAttrValue> skuSaleAttrValueList = manageService.getSkuSaleAttrValueListBySpu(skuInfo.getSpuId());

        String valueIdsKey = "";
        // 存放 属性值id|属性值id：skuid的map
        HashMap<String, String> valuesSkuMap = new HashMap<>();

        // 遍历skuSaleAttrValueList 拼json串
        for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
            SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
            if (valueIdsKey.length()!=0){
                // 不是第一次
                valueIdsKey +="|";
            }
            valueIdsKey += skuSaleAttrValue.getSaleAttrValueId();
            if (skuSaleAttrValueList.size()==(i+1)||!skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                // 符合第一个条件说明到表底了 符合第二个条件说明当前skuid对应的sale_attr_value_id存储完毕
                // 放进map中
                valuesSkuMap.put(valueIdsKey, skuSaleAttrValue.getSkuId());
                // 把valueIdsKey重置
                valueIdsKey = "";
            }
        }
        System.out.println("valueIdsKey:  "+valueIdsKey);
        String valuesSkuJson  = JSON.toJSONString(valuesSkuMap);
        System.out.println("valuesSkuJson: "+valuesSkuJson);
        model.addAttribute("valuesSkuJson", valuesSkuJson);

        model.addAttribute("skuInfo", skuInfo);
        List<SpuSaleAttr> spuSaleAttrList = manageService.getSpuSaleAttrListCheckBySku(skuInfo);
        model.addAttribute("spuSaleAttrList",spuSaleAttrList);
        listService.incrHotScore(skuId);
        return "item";
    }

}
