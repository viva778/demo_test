package com.supcon.orchid.material.superwms.util.adptor;

import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.material.superwms.constants.BizTypeCode;
import com.supcon.orchid.material.superwms.constants.QrTypeCode;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseSourceType;
import com.supcon.orchid.material.superwms.util.adptor.in.*;
import com.supcon.orchid.material.superwms.util.adptor.out.*;

public class AdaptorRegister {

    @Signal("startup")
    private static void register(){
        //#################################################---出库---#################################################
        //------------------------------------------其他出库单------------------------------------------------
        AdaptorCenter.registerOutboundTypeAdaptor(BizTypeCode.OTHER_SHIPMENT, OtherOutTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.OTHER_SHIPMENT,
                "OtherOut",
                MaterialOtherOutSingle.class,
                "OUT_SINGLE",
                MaterialOutSingleDetai.class,
                "APPLI_QUANTITY",
                "OUT_QUANTITY",
                "otherOut",
                "material_1.0.0_otherOutSingle_otherOutEdit",
                "material_1.0_otherOutSingle_list",
                "material_1.0_otherOutSingle_views", null,
                null,
                new OutTypeContext() {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.outOther");
                    }
                }));

        //------------------------------------------采购退货出库单------------------------------------------------
        AdaptorCenter.registerOutboundTypeAdaptor(BizTypeCode.PURCHASE_RETURN, PurchaseReturnTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.PURCHASE_RETURN,
                "PurchaseReturnOut",
                MaterialPurReturn.class,
                "PUR_RETURN",
                MaterialPurReturnPart.class,
                "APPLY_NUM",
                "RETURN_NUM",
                "returnedPurchaseFlw",
                "material_1.0.0_purchaseReturn_purReturnEdit",
                "material_1.0_purchaseReturn_purReturnList",
                "material_1.0_purchaseReturn_purReturnView", null,
                null,
                new OutTypeContext() {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.outPurchaseReturn");
                    }
                }));

        //------------------------------------------销售出库单------------------------------------------------
        AdaptorCenter.registerOutboundTypeAdaptor(BizTypeCode.SALE_SHIPMENT, SaleOutTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.SALE_SHIPMENT,
                "SaleOut",
                MaterialSaleOutSingle.class,
                "OUT_SINGLE",
                MaterialSaleOutDetail.class,
                "APPLI_QUANLITY",
                "OUT_QUANTITY",
                "saleOutFlw",
                "material_1.0.0_saleOut_saleOutEdit",
                "material_1.0_salDelivery_salDeliList",
                "material_1.0_salDelivery_salDeiliView", null,
                null,
                new OutTypeContext() {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.outSale");
                    }
                }));

        //------------------------------------------生产出库单------------------------------------------------
        AdaptorCenter.registerOutboundTypeAdaptor(BizTypeCode.PRODUCE_SHIPMENT, ProduceOutTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.PRODUCE_SHIPMENT,
                "ProduceOut",
                MaterialProduceOutSing.class,
                "OUT_SINGLE",
                MaterialProduceOutDeta.class,
                "APPLI_QUANLITY",
                "OUT_QUANTITY",
                "produceOutSingleFlw",
                "material_1.0.0_produceOutSingle_produceOutSingleEdit",
                "material_1.0_produceOutSingle_produceOutSingleList",
                "material_1.0_produceOutSingle_produceOutSingleView", null,
                null,
                new OutTypeContext() {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.outProduce");
                    }
                }));

        //------------------------------------------废料出库单------------------------------------------------
        AdaptorCenter.registerOutboundTypeAdaptor(BizTypeCode.WASTE_SHIPMENT, WasteOutTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.WASTE_SHIPMENT,
                "WasteOut",
                MaterialWasteOutSingle.class,
                "OUT_SINGLE_ID",
                MaterialWasteOutDetail.class,
                "APPLY_NUMBER",
                "OUT_NUMBER",
                "garbageOutFlw",
                "material_1.0.0_wasteOutSingle_garbageOutEdit",
                "material_1.0.0_wasteOutSingle_garbageOutList",
                "material_1.0.0_wasteOutSingle_garbageOutView",
                "废料出库单",
                null,null,
                new OutTypeContext() {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.outWaste");
                    }
                }
        ));

        //#################################################---入库---#################################################

        //------------------------------------------销售退货单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.SALE_RETURN, SaleReturnTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.SALE_RETURN,
                "SaleReturnIn",
                MaterialSaleReturn.class,
                "SALE_RETURN",
                MaterialSaleReturnGood.class,
                "APPLY_NUM",
                "RETURN_NUM",
                "saleReturnFlw",
                "material_1.0.0_saleReturn_saleReturnEdit",
                "material_1.0_saleReturn_saleReturnList",
                "material_1.0_saleReturn_saleReturnView", BaseSourceType.OTHER_IN,
                null,
                new InTypeContext(false, false) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inSaleReturn");
                    }
                }));

        //------------------------------------------采购入库单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.PURCHASE_STORAGE, PurchaseInTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.PURCHASE_STORAGE,
                "PurchaseIn",
                MaterialPurchInSingle.class,
                "IN_SINGLE",
                MaterialPurchInPart.class,
                "APPLY_QUANTITY",
                "IN_QUANTITY",
                "purchaseInsingleFlw",
                "material_1.0.0_purchaseInSingles_purchaseInsingleEdit",
                "material_1.0_purchaseInSingles_purchaseInSingleList",
                "material_1.0_purchaseInSingles_purchaseInsingleView", BaseSourceType.PURCHASE_IN,
                QrTypeCode.PURCHASE,
                new InTypeContext(false, false) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inPurchase");
                    }
                }));

        //------------------------------------------生产入库单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.PRODUCE_STORAGE, ProduceInTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.PRODUCE_STORAGE,
                "ProduceIn",
                MaterialProduceInSingl.class,
                "IN_SINGLE",
                MaterialProduceInDetai.class,
                "APPLI_QUANLITY",
                "IN_QUANTITY",
                "productInSingleFlw",
                "material_1.0.0_produceInSingles_productInSingleEdit",
                "material_1.0_produceInSingles_productInSingleList",
                "material_1.0_produceInSingles_productInSingleView", BaseSourceType.TASK_REPORT,
                QrTypeCode.TASK_REPORT,
                new InTypeContext(false, true) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inProduce");
                    }
                }));

        //------------------------------------------生产退料入库单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.PRODUCE_RETURN, ProduceReturnTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.PRODUCE_RETURN,
                "ProdReturnIn",
                MaterialProdReturn.class,
                "BACK_MAIN",
                MaterialProdReturnDeta.class,
                "APPLY_NUM",
                "RETURN_NUM",
                "produceBackSinglesProFlw",
                "material_1.0.0_produceBackSingles_productBackEdit",
                "material_1.0_produceBackSingles_productBackList",
                "material_1.0_produceBackSingles_productBackView", null,
                null,
                new InTypeContext(false, false) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inProduceReturn");
                    }
                }));

        //------------------------------------------其他入库单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.OTHER_STORAGE, OtherInTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.OTHER_STORAGE,
                "OtherIn",
                MaterialOtherInSingle.class,
                "IN_SINGLE",
                MaterialInSingleDetail.class,
                "APPLI_QUANLITY",
                "IN_QUANTITY",
                "otherInFlw",
                null, null, null,
                BaseSourceType.OTHER_IN,
                QrTypeCode.OTHER_STORAGE,
                new InTypeContext(false, true) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inOther");
                    }
                }));

        //------------------------------------------废料入库单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.WASTE_STORAGE, WasteInTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.WASTE_STORAGE,
                "WasteIn",
                MaterialWasteInSingle.class,
                "WASTE_IN_SINGLE",
                MaterialWasteInDetail.class,
                "APPLY_NUM",
                "IN_QUANLITY",
                "hazardousInFlw",
                null, null, null,
                null,
                null,
                new InTypeContext(false, false) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inWaste");
                    }
                }));

        //#################################################---其他---#################################################

        //------------------------------------------调拨单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.ALLOCATION, AllocationInTypeAdaptor.class);
        AdaptorCenter.registerOutboundTypeAdaptor(BizTypeCode.ALLOCATION, AllocationOutTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.ALLOCATION,
                "Allocation",
                MaterialAppropriation.class,
                "APP_HEADER",
                MaterialAppDetail.class,
                "APPLI_QUANTITY",
                "APP_NUM",
                "appFlow",
                "material_1.0.0_appropriation_appEdit",
                "material_1.0_appropriation_appList",
                "material_1.0_appropriation_appView",
                null,null,
                new InTypeContext(false, false) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inAllocation");
                    }
                },
                new OutTypeContext() {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.outAllocation");
                    }
                }
        ));
        //------------------------------------------货位调整单------------------------------------------------
        AdaptorCenter.registerInboundTypeAdaptor(BizTypeCode.PLACE_ADJUST, PlaceAdjustInTypeAdaptor.class);
        AdaptorCenter.registerOutboundTypeAdaptor(BizTypeCode.PLACE_ADJUST, PlaceAdjustOutTypeAdaptor.class);
        AdaptorCenter.registerBizTypeContext(new BizTypeContext(
                BizTypeCode.PLACE_ADJUST,
                "PlaceAdjust",
                MaterialPlaceAjustInfo.class,
                "PLACE_AJUST_INFO",
                MaterialPlaceAjustPart.class,
                "AJUST_AMOUNT",
                "AJUST_AMOUNT",
                "placeAdjustFlw",
                null,null,null,
                null,null,
                new InTypeContext(false, false) {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.inAdjust");
                    }
                },
                new OutTypeContext() {
                    @Override
                    public String getDescription() {
                        return InternationalResource.get("material.description.outAdjust");
                    }
                }
        ));

        //------------------------------------------采购到货------------------------------------------------
        AdaptorCenter.registerTableTypeAdaptor(BizTypeCode.PURCHASE_ARRIVAL,PurchaseArrivalTypeAdaptor.class);
    }
}
