package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseAttrInfo;
import com.atguigu.gmall.bean.BaseCatalog1;
import com.atguigu.gmall.bean.BaseCatalog2;
import com.atguigu.gmall.bean.BaseCatalog3;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class ManageController {

    @Reference
    private ManageService manageService;

    @RequestMapping("/index")
    public String index(){
        return "index";
    }

    // attrListPage
    @RequestMapping("/attrListPage")
    public String attrListPage(){
        return "attrListPage";
    }

    // getCatalog1
    @RequestMapping("/getCatalog1")
    @ResponseBody
    public List<BaseCatalog1> getCatalog1(){
        List<BaseCatalog1> catalog1 = manageService.getCatalog1();
        return catalog1;
    }

    //  getCatalog2?catalog1Id=
    @RequestMapping("/getCatalog2")
    @ResponseBody
    public List<BaseCatalog2> getCatalog2(String catalog1Id){
        return manageService.getCatalog2(catalog1Id);
    }

    //  getCatalog3?catalog2Id=
    @RequestMapping("/getCatalog3")
    @ResponseBody
    public List<BaseCatalog3> getCatalog3(String catalog2Id){
        return manageService.getCatalog3(catalog2Id);
    }

    //  attrInfoList?catalog3Id=
    @RequestMapping("/attrInfoList")
    @ResponseBody
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.getAttrList(catalog3Id);
    }

    //  "saveAttrInfo",attrJson,
    @RequestMapping("/saveAttrInfo")
    @ResponseBody
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
    }

}
