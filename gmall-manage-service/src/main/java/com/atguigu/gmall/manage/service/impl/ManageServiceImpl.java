package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service
public class ManageServiceImpl implements ManageService {

    @Autowired
    private BaseCatalog1Mapper baseCatalog1Mapper;
    @Autowired
    private BaseCatalog2Mapper baseCatalog2Mapper;
    @Autowired
    private BaseCatalog3Mapper baseCatalog3Mapper;
    @Autowired
    private BaseAttrInfoMapper baseAttrInfoMapper;
    @Autowired
    private BaseAttrValueMapper baseAttrValueMapper;
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SkuInfoMapper skuInfoMapper;
    @Autowired
    private SkuAttrValueMapper skuAttrValueMapper;
    @Autowired
    private SkuImageMapper skuImageMapper;
    @Autowired
    private SkuSaleAttrValueMapper skuSaleAttrValueMapper;

    @Override
    public List<BaseCatalog1> getCatalog1() {
        return baseCatalog1Mapper.selectAll();
    }

    @Override
    public List<BaseCatalog2> getCatalog2(String catalog1Id) {
        BaseCatalog2 baseCatalog2 = new BaseCatalog2();
        baseCatalog2.setCatalog1Id(catalog1Id);
        return baseCatalog2Mapper.select(baseCatalog2);
    }

    @Override
    public List<BaseCatalog3> getCatalog3(String catalog2Id) {
        BaseCatalog3 baseCatalog3 = new BaseCatalog3();
        baseCatalog3.setCatalog2Id(catalog2Id);
        return baseCatalog3Mapper.select(baseCatalog3);
    }

    @Override
    public List<BaseAttrInfo> getAttrList(String catalog3Id) {
        /*BaseAttrInfo baseAttrInfo = new BaseAttrInfo();
        baseAttrInfo.setCatalog3Id(catalog3Id);
        return baseAttrInfoMapper.select(baseAttrInfo);*/
        return baseAttrInfoMapper.getBaseAttrInfoListByCatalog3Id(Long.parseLong(catalog3Id));
    }

    @Override
    public void saveAttrInfo(BaseAttrInfo baseAttrInfo) {
        // 先操作baseattrInfo
        if (baseAttrInfo.getId()!=null && baseAttrInfo.getId().length()>0){
            // id不为空 是修改
            baseAttrInfoMapper.updateByPrimaryKeySelective(baseAttrInfo);
        } else {
            // id 为空 是增添

            baseAttrInfo.setId(null);

            baseAttrInfoMapper.insertSelective(baseAttrInfo);
        }

        // 再操作BaseAttrValue
        // 先清空 再保存
        //清空
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        baseAttrValueMapper.delete(baseAttrValue);

        // 保存
        // 集合不为空
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
        if (attrValueList!=null && attrValueList.size()>0){
            // 遍历 插入属性值
            for (BaseAttrValue attrValue : attrValueList) {
                if (attrValue.getId().length()==0){
                    attrValue.setId(null);
                }
                attrValue.setAttrId(baseAttrInfo.getId());
                baseAttrValueMapper.insertSelective(attrValue);
            }
        }

    }

    @Override
    public BaseAttrInfo getattrInfo(String attrId) {
        BaseAttrInfo baseAttrInfo = baseAttrInfoMapper.selectByPrimaryKey(attrId);
        BaseAttrValue baseAttrValue = new BaseAttrValue();
        baseAttrValue.setAttrId(baseAttrInfo.getId());
        List<BaseAttrValue> baseAttrValueList = baseAttrValueMapper.select(baseAttrValue);
        baseAttrInfo.setAttrValueList(baseAttrValueList);
        return baseAttrInfo;
    }

    @Override
    public List<SpuInfo> getSpuInfoList(SpuInfo spuInfo) {
        return spuInfoMapper.select(spuInfo);
    }

    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        return baseSaleAttrMapper.selectAll();
    }

    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {

        // 存SpuInfo的信息 spuInfoMapper------------------------------
        //private String id;
        //private String spuName;
        //private String description;
        //private  String catalog3Id;
        if (spuInfo.getId()==null || spuInfo.getId().length()==0){
            // 新增
            spuInfo.setId(null);
            spuInfoMapper.insertSelective(spuInfo);
        } else {
            // 修改
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }

        //保存图片信息 先删除 再插入------------------------------
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage);

        if (spuImageList != null && spuImageList.size()>0){
            for (SpuImage image : spuImageList) {
                image.setId(null);
                image.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(image);
            }
        }

        // 保存销售属性 先删除 再添加------------------------------
        // 根据spuid删除
        SpuSaleAttr spuSaleAttr = new SpuSaleAttr();
        spuSaleAttr.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr);

        // 添加
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();

        //String spuId;
        //String saleAttrId;
        //String saleAttrName;
        for (SpuSaleAttr saleAttr : spuSaleAttrList) {
            saleAttr.setId(null);
            saleAttr.setSpuId(spuInfo.getId());
            spuSaleAttrMapper.insertSelective(saleAttr);
            // 销售属性值-------------------------------------
            List<SpuSaleAttrValue> spuSaleAttrValueList = saleAttr.getSpuSaleAttrValueList();
            if (spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
                for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                    spuSaleAttrValue.setId(null);
                    spuSaleAttrValue.setSpuId(spuInfo.getId());
                    spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                }
            }
        }

    }

    @Override
    public List<SpuImage> getSpuImageList(String spuId) {
        SpuImage spuImage = new SpuImage();
        spuImage.setSpuId(spuId);
        return spuImageMapper.select(spuImage);
    }

    @Override
    public List<SpuSaleAttr> getSpuSaleAttrList(String spuId) {
        return spuSaleAttrMapper.selectSpuSaleAttrList(Long.parseLong(spuId));
    }

    @Override
    public void saveSkuInfo(SkuInfo skuInfo) {
        // 保存skuInfo
        if (skuInfo.getId()==null || skuInfo.getId().length()==0){
            // 添加 SkuInfo
            skuInfo.setId(null);
            skuInfoMapper.insertSelective(skuInfo);
        } else {
            skuInfoMapper.updateByPrimaryKeySelective(skuInfo);
        }

        // 保存skuImage 先删除后添加
        SkuImage skuImage = new SkuImage();
        skuImage.setSkuId(skuInfo.getId());
        skuImageMapper.delete(skuImage);
        //id;自增
        //String skuId;后台获取
        List<SkuImage> skuImageList = skuInfo.getSkuImageList();
        if (skuImageList!=null && skuImageList.size()>0){
            for (SkuImage image : skuImageList) {
                image.setId(null);
                image.setSkuId(skuInfo.getId());
                skuImageMapper.insertSelective(image);
            }
        }


        // 保存平台属性 先删除后添加
        SkuAttrValue skuAttrValue = new SkuAttrValue();
        skuAttrValue.setSkuId(skuInfo.getId());
        skuAttrValueMapper.delete(skuAttrValue);

        List<SkuAttrValue> skuAttrValueList = skuInfo.getSkuAttrValueList();
        if (skuAttrValueList!=null && skuAttrValueList.size()>0){
            for (SkuAttrValue attrValue : skuAttrValueList) {
                attrValue.setId(null);
                attrValue.setSkuId(skuInfo.getId());
                skuAttrValueMapper.insertSelective(attrValue);
            }
        }


        // 保存销售属性 先删除后添加
        SkuSaleAttrValue skuSaleAttrValue = new SkuSaleAttrValue();
        skuSaleAttrValue.setSkuId(skuInfo.getId());
        skuSaleAttrValueMapper.delete(skuSaleAttrValue);

        List<SkuSaleAttrValue> skuSaleAttrValueList = skuInfo.getSkuSaleAttrValueList();
        if (skuSaleAttrValueList!=null && skuSaleAttrValueList.size()>0){
            for (SkuSaleAttrValue saleAttrValue : skuSaleAttrValueList) {
                saleAttrValue.setId(null);
                saleAttrValue.setSkuId(skuInfo.getId());
                skuSaleAttrValueMapper.insertSelective(saleAttrValue);
            }
        }
    }
}
