package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SkuManageController {

    @Reference
    private ManageService manageService;
    @Reference
    private ListService listService;

    @RequestMapping("/saveSku")
    @ResponseBody
    public String saveSku(SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return "ok";
    }

    @RequestMapping("/onSale")
    @ResponseBody
    public String onSale(String skuId){
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        SkuInfo skuInfo = manageService.getSkuInfo(skuId);
        // 拷贝属性
        BeanUtils.copyProperties(skuInfo, skuLsInfo);
        listService.saveSkuLsInfo(skuLsInfo);
        return "ok";
    }

}
