package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SkuManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("/saveSku")
    @ResponseBody
    public String saveSku(SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return "ok";
    }
}
