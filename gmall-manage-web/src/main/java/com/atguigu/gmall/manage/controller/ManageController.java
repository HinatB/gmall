package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
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

    //  getAttrValueList?attrId=  列出编辑 平台属性值getAttrValueList?attrId=46
    @RequestMapping("/getAttrValueList")
    @ResponseBody
    public List<BaseAttrValue> getAttrValueListByattrId(String attrId){

        BaseAttrInfo baseAttrInfo = manageService.getattrInfo(attrId);
        return baseAttrInfo.getAttrValueList();
    }

    //  spuListPage 跳转spuListPage页面
    @RequestMapping("/spuListPage")
    public String toSpuListPage(){
        return "spuListPage";
    }

    // getSpuInfoList?ctg3Id= 得到supinfo集合
    @RequestMapping("/getSpuInfoList")
    @ResponseBody
    public List<SpuInfo> getSpuInfoList(String ctg3Id){
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(ctg3Id);
        List<SpuInfo> spuInfoList = manageService.getSpuInfoList(spuInfo);
        return spuInfoList;
    }


}
