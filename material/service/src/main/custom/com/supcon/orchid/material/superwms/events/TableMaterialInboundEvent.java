package com.supcon.orchid.material.superwms.events;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.ec.entities.Property;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.material.daos.MaterialBatchInfoDao;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.PostSqlUpdater;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.material.superwms.constants.BizTypeCode;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseCheckResult;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseCheckState;
import com.supcon.orchid.material.superwms.constants.systemcode.StorageState;
import com.supcon.orchid.material.superwms.services.TableInboundService;
import com.supcon.orchid.material.superwms.util.MaterialExceptionThrower;
import com.supcon.orchid.material.superwms.util.adptor.AdaptorCenter;
import com.supcon.orchid.orm.entities.CodeEntity;
import com.supcon.orchid.services.BAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TableMaterialInboundEvent {

    @Autowired
    private TableInboundService inboundTableService;
    @Autowired
    private MaterialBatchInfoDao batchInfoDao;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    //----------------------------------其他入库-----------------------------------START
    @Signal("OtherInAfterSubmit")
    private void otherInAfterSubmit(MaterialOtherInSingle table) {
        inboundTableService.standardInboundEvent(BizTypeCode.OTHER_STORAGE, table, table.getWorkFlowVar());
        // 修正标准入库逻辑中批次信息的检验状态和是否可用
        if (!table.getInspectRequired()) {
            List<MaterialInSingleDetail> inSingleDetailList = Dbs.findByCondition(MaterialInSingleDetail.class,
                    "VALID=1 AND IN_SINGLE=?", table.getId());

            inSingleDetailList.stream()
                    .filter(in -> Boolean.TRUE.equals(in.getGood().getIsCheck()) && BaseBatchType.isEnable(in.getGood().getIsBatch()))
                    .collect(Collectors.toList())
                    .forEach(inSingleDetail -> {
                        String batchText = inSingleDetail.getBatchText();
                        BaseSetMaterial good = inSingleDetail.getGood();

                        String hql = "from " + MaterialBatchInfo.JPA_NAME + " p where p.valid = true and p.batchNum = ?0 and p.materialId = ?1";
                        List<Property> list = batchInfoDao.findByHql(hql, new Object[]{batchText, good});

                        List<MaterialBatchInfo> batchInfoList = Dbs.findByCondition(MaterialBatchInfo.class,
                                "VALID=1 AND BATCH_NUM=? AND MATERIAL_ID=?", batchText, good.getId());
                        for (MaterialBatchInfo materialBatchInfo : batchInfoList) {
                            SystemCode checkState = materialBatchInfo.getCheckState();
                            // 待检改为无需检验
                            if (new SystemCode(BaseCheckState.TO_CHECK).equals(checkState)) {
                                materialBatchInfo.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
                            }
                            // 不可用改为可用
                            Boolean isAvailable = materialBatchInfo.getIsAvailable();
                            if (!isAvailable) {
                                materialBatchInfo.setIsAvailable(true);
                            }
                        }
                    });
        }
    }

    //其他入库单表体保存，不启用批号则清空，避免平台自动生成
    @Signal("AfterSaveOtherInDetail")
    private void afterSaveMaterOtherIn(MaterialInSingleDetail detail) {
        if (!BaseBatchType.isEnable(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch))) {
            MaterialInSingleDetail saveDetail = Dbs.reload(detail);
            if (Strings.valid(saveDetail.getBatchText())) {
                PostSqlUpdater.getAutoWritableEntity(saveDetail).setBatchText(null);
            }
        }
    }
    //----------------------------------其他入库-----------------------------------END


    //----------------------------------生产入库-----------------------------------START
    @Signal("ProduceInAfterSubmit")
    private void produceInAfterSubmit(MaterialProduceInSingl table) {
        inboundTableService.standardInboundEvent(BizTypeCode.PRODUCE_STORAGE, table, table.getWorkFlowVar());
    }

    //----------------------------------生产入库-----------------------------------END


    //----------------------------------销售退货入库-----------------------------------START
    @Signal("SaleReturnInAfterSubmit")
    private void saleReturnAfterSubmit(MaterialSaleReturn table) {
        inboundTableService.standardInboundEvent(BizTypeCode.SALE_RETURN, table, table.getWorkFlowVar());
    }

    @Signal("InboundDetail")
    private void afterSaleReturnSubmit(MaterialSaleReturnGood detail, BigDecimal inQuan) {
        //生效之后直接回填销售出库单明细的退货数量
        if (detail != null && detail.getSaleOutDetail() != null) {
            Dbs.execute("update MATER_SALE_OUT_DETAILS set RETURN_NUM= RETURN_NUM + ?  where id=? AND VALID = 1 ", inQuan, detail.getSaleOutDetail().getId());
        }
    }


    //----------------------------------销售退货入库-----------------------------------END


    //----------------------------------生产退料入库单-----------------------------------START
    @Signal("ProdReturnInAfterSubmit")
    private void prodReturnAfterSubmit(MaterialProdReturn table) {
        inboundTableService.standardInboundEvent(BizTypeCode.PRODUCE_RETURN, table, table.getWorkFlowVar());
    }

    @Signal("InboundDetail")
    private void afterProdReturnSubmit(MaterialProdReturnDeta detail, BigDecimal inQuan) {
        if (detail != null && detail.getProdOutDetail() != null) {
            //生效之后直接回填生产出库单退料数量
            Dbs.execute("update MATER_PRODUCE_OUT_DETAS set BACK_QUANTITY= BACK_QUANTITY + ?  where id=? AND VALID = 1 ", inQuan, detail.getProdOutDetail().getId());
        }
    }
    //----------------------------------生产退料入库单-----------------------------------END

    //----------------------------------采购入库-----------------------------------START
    @Signal("PurchaseInAfterSubmit")
    private void purchaseInAfterSubmit(MaterialPurchInSingle table) {
        //检验托盘是否存在 通过容器查询容器明细
        inboundTableService.standardInboundEvent(BizTypeCode.PURCHASE_STORAGE, table, table.getWorkFlowVar());
    }

    //首次提交，绑定容器和明细
    @Signal("PurchaseInFirstCommit")
    private void purchaseInFirstCommit(MaterialPurchInSingle table, List<MaterialPurchInPart> details) {
        //查询容器明细
        List<MaterialPurchInSubDtl> subDtlList = Dbs.findByCondition(
                MaterialPurchInSubDtl.class,
                "VALID=1 AND PURCHASE_IN=?",
                table.getId()
        ).stream().map(PostSqlUpdater::getAutoWritableEntity).collect(Collectors.toList());
        checkContainer(subDtlList);
        subDtlList.forEach(subDtl -> {
            //根据uuid和明细绑定
            details.stream()
                    .filter(val -> val.getUuid().equals(subDtl.getPurchaseInDetailUuid()))
                    .findAny()
                    .ifPresent(subDtl::setPurchaseInDetail);
        });
    }

    @Signal("PurchaseInEffecting")
    private void purchaseInEffecting(MaterialPurchInSingle table, List<MaterialPurchInPart> details) {

        if (Strings.valid(table.getPurArrivalNo())) {
            //校验物料是否检验合格
            details.forEach(detail -> {
                if (!BaseBatchType.isEnable(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch)) &&
                        Boolean.TRUE.equals(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsCheck)) &&
                        !BaseCheckResult.isQualified(detail.getCheckResult())
                ) {
                    // 物料{}不启用批次，只有检验合格才能入库！
                    throw new BAPException(InternationalResource.get("material.custom.onlyqualifiedPurchaseIsReceived",
                            (Object) detail.getGood().getName()));
                }
            });
        }
        //生效时将容器的货位更新
        //查询容器明细
        List<MaterialPurchInSubDtl> subDtlList = Dbs.findByCondition(
                MaterialPurchInSubDtl.class,
                "VALID=1 AND PURCHASE_IN=?",
                table.getId()
        );
        checkContainer(subDtlList);
        //将容器对应的货位更新到容器台账
        subDtlList.forEach(subDtl -> {
            BaseSetStoreSet place = Dbs.getProp(subDtl.getPurchaseInDetail(), MaterialPurchInPart::getPlaceSet);
            MaterialContainerFile container = PostSqlUpdater.getAutoWritableEntity(subDtl.getContainer());
            container.setPlaceSet(place);
        });
    }

    /**
     * 检验托盘是否存在 通过容器查询容器明细
     */
    private void checkContainer(List<MaterialPurchInSubDtl> subDtlList) {
        for (MaterialPurchInSubDtl subDtl : subDtlList) {
            //查找容器台账明细中的数据
            MaterialContainerParts load = Dbs.load(
                    MaterialContainerParts.class,
                    "VALID=1 AND CONTAINER=? AND MATER_INFO = ? AND MATER_QTY = ? ",
                    subDtl.getContainer().getId(), subDtl.getMaterial(), subDtl.getQuantity().longValueExact()
            );
            if (load == null) {
                String containerName = Dbs.getProp(subDtl.getContainer(), MaterialContainerFile::getName);
                throw new BAPException("\" " + containerName + "\"容器未绑定关联到货明细或数量不相同，请重新选确认。");
            }
        }
    }

    //入库时，回填状态、数量至到货单明细
    @Signal("InboundDetail")
    private void pushbackInStockState(MaterialPurchInPart detail, BigDecimal inQuan) {
        if (detail != null && detail.getSrcPartId() != null) {
            Dbs.execute(
                    "UPDATE " + MaterialPurArrivalPart.TABLE_NAME + " SET IN_STATE=?,VALID_QUAN=? WHERE ID=?",
                    StorageState.IN_STOCK, inQuan, detail.getSrcPartId()
            );
            if (detail.getPurchaseId() != null) {
                //回填到订单明细的未入库数量 == 未入库数量 - 入库数量
                Dbs.execute(
                        "UPDATE " + MaterialPurchasePart.TABLE_NAME + " SET NO_WARE_NUM= NO_WARE_NUM - ? WHERE ID=?",
                        inQuan, detail.getPurchaseId()
                );
            }


        }
    }

    //----------------------------------采购入库-----------------------------------END

    //----------------------------------废料入库-----------------------------------START
    @Signal("WasteInAfterSubmit")
    private void wasteInAfterSubmit(MaterialWasteInSingle table) {
        inboundTableService.standardInboundEvent(BizTypeCode.WASTE_STORAGE, table, table.getWorkFlowVar());
    }

    //废料入库单表体保存，不启用批号则清空，避免平台自动生成
    @Signal("AfterSaveWasteInDetail")
    private void afterSaveWasteIn(MaterialWasteInDetail detail) {
        if (!BaseBatchType.isEnable(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsBatch))) {
            MaterialWasteInDetail saveDetail = Dbs.reload(detail);
            if (Strings.valid(saveDetail.getBatchText())) {
                PostSqlUpdater.getAutoWritableEntity(saveDetail).setBatchText(null);
            }
        }
    }
    //----------------------------------废料入库-----------------------------------END


    //----------------------------------按件管理-----------------------------------START
    @Signal("StockIncreased")
    private void increaseByPiece(MaterialStandingcrop stock) {
        //按件管理现存量只能为1
        if (BaseBatchType.BY_PIECE.equals(Dbs.getProp(stock.getGood(), BaseSetMaterial::getIsBatch, CodeEntity::getId))) {
            if (stock.getOnhand().compareTo(BigDecimal.ONE) != 0) {
                MaterialExceptionThrower.piece_batch_stock_only_can_be_one(Dbs.getProp(stock.getGood(), BaseSetMaterial::getName) + "/" + stock.getBatchText());
            }
        }
    }
    //----------------------------------按件管理-----------------------------------END

    //----------------------------------入库SE-----------------------------------START
    @Signal(value = ".*InSuperEdit", signal_as_param = true)
    private void inboundSuperEdit(AbstractEcFullEntity table, String signal) {
        if (table.getStatus() != null && table.getStatus() == 99) {
            String prefix = signal.substring(0, signal.length() - "SuperEdit".length());
            inboundTableService.quickEffect(AdaptorCenter.getBizTypeContext(prefix).getBizType(), table);
        }
    }
    //----------------------------------入库SE-----------------------------------END
}
