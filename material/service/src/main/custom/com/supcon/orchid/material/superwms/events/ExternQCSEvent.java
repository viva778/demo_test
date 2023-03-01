package com.supcon.orchid.material.superwms.events;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.ec.services.MsModuleRelationService;
import com.supcon.orchid.fooramework.util.Maps;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.material.entities.*;
import com.supcon.orchid.fooramework.annotation.signal.Signal;
import com.supcon.orchid.fooramework.support.ModuleRequestContext;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.ModuleHttpClient;
import com.supcon.orchid.fooramework.util.Transactions;
import com.supcon.orchid.material.superwms.config.MaterialSystemConfig;
import com.supcon.orchid.material.superwms.constants.OtherInCheckOption;
import com.supcon.orchid.material.superwms.constants.QCSInspect;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseRedBlue;
import com.supcon.orchid.material.superwms.entities.dto.QCSCheckRequestDTO;
import com.supcon.orchid.material.superwms.util.builder.QCSCheckRequestDTOBuilder;
import com.supcon.orchid.services.BAPException;
import com.supcon.orchid.support.Result;
import org.apache.commons.lang.BooleanUtils;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = Exception.class)
public class ExternQCSEvent {

    @Autowired
    private MaterialSystemConfig materialSystemConfig;
    @Autowired
    private MsModuleRelationService msModuleRelationService;
    @Autowired
    ExternQCSEvent self;
    private final Logger log = LoggerFactory.getLogger(getClass());

    //生成请检单
    @Signal(value = "PurchaseArrivalEffecting", priority = 1)
    public void generateCheckRequest(MaterialPurArrivalInfo table, List<MaterialPurArrivalPart> details) {
        if (BaseRedBlue.BLUE.equals(table.getRedBlue().getId()) && Boolean.TRUE.equals(materialSystemConfig.getGenerateCheckRequest())) {
            List<MaterialPurArrivalPart> detailsNeedCheck = details
                    .stream()
                    .filter(detail -> Boolean.TRUE.equals(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsCheck)))
                    .collect(Collectors.toList());
            if (!detailsNeedCheck.isEmpty()) {
                Assert.isTrue(
                        msModuleRelationService.checkModuleStatus(QCSInspect.QCS_MODULE_NAME),
                        InternationalResource.get("material.custom.moduleIsNotPublished", QCSInspect.QCS_MODULE_NAME)
                );
                List<QCSCheckRequestDTO> dtoList = detailsNeedCheck.stream().map(detail -> QCSCheckRequestDTOBuilder
                        .aQCSCheckRequestDTO()
                        .withCreateDeptId(table.getCreateDepartmentId())
                        .withCreatePositionId(table.getCreatePositionId())
                        .withCreateStaffId(table.getCreateStaffId())
                        .withVendorId(table.getVendor().getId())
                        .withSourceTableId(table.getId())
                        .withSourcTableNo(table.getTableNo())
                        .withSourceId(detail.getId())
                        .withProdId(detail.getGood().getId())
                        .withBatchCode(detail.getBatch())
                        .withQuantity(detail.getArrivalQuan())
                        .build()).collect(Collectors.toList());
                //设置事务结束后调用QCS，避免死锁
                Transactions.appendEventAfterCommit(() -> self.saveCheckApply(dtoList, MaterialPurchInPart.TABLE_NAME, "SRC_PART_ID"));
//                saveCheckApply(dtoList, MaterialPurchInPart.TABLE_NAME, "SRC_PART_ID");
            }
        }
    }

    //采购入库单调用接口生成请检单
    @Signal(value = "PurchInGenerateCheck")
    public void generateCheckRequestPurchIn(MaterialPurchInSingle table, List<MaterialPurchInPart> details) {
        if (BaseRedBlue.BLUE.equals(table.getRedBlue().getId()) && Boolean.TRUE.equals(materialSystemConfig.getGenerateCheckRequest())) {
            List<MaterialPurchInPart> detailsNeedCheck = details
                    .stream()
                    .filter(detail -> Boolean.TRUE.equals(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsCheck)))
                    .collect(Collectors.toList());
            if (!detailsNeedCheck.isEmpty()) {
                Assert.isTrue(
                        msModuleRelationService.checkModuleStatus(QCSInspect.QCS_MODULE_NAME),
                        InternationalResource.get("material.custom.moduleIsNotPublished", QCSInspect.QCS_MODULE_NAME)
                );
                List<QCSCheckRequestDTO> dtoList = detailsNeedCheck.stream().map(detail -> QCSCheckRequestDTOBuilder
                        .aQCSCheckRequestDTO()
                        .withCreateDeptId(table.getCreateDepartmentId())
                        .withCreatePositionId(table.getCreatePositionId())
                        .withCreateStaffId(table.getCreateStaffId())
                        .withVendorId(table.getVendor() == null ? null : table.getVendor().getId())
                        .withSourceTableId(table.getId())
                        .withSourcTableNo(table.getTableNo())
                        .withSourceId(detail.getId())
                        .withProdId(detail.getGood().getId())
                        .withBatchCode(detail.getBatch())
                        .withQuantity(detail.getApplyQuantity())
                        .build()).collect(Collectors.toList());
                //设置事务结束后调用QCS，避免死锁
                Transactions.appendEventAfterCommit(() -> self.saveCheckApply(dtoList, MaterialPurchInPart.TABLE_NAME, "ID"));
//                saveCheckApply(dtoList, MaterialPurchInPart.TABLE_NAME, "SRC_PART_ID");
            }
        }
    }

    // 其它入库生效发起请检单
    @Signal("OtherInEffecting")
    public void generateCheckRequestOtherIn(MaterialOtherInSingle table, List<MaterialInSingleDetail> details) {
        Boolean inspectRequired = table.getInspectRequired();
        String otherInCheckOptions = materialSystemConfig.getOtherInCheckOptions();
        if (BaseRedBlue.BLUE.equals(table.getRedBlue().getId())
                && BooleanUtils.isTrue(inspectRequired)
                && !OtherInCheckOption.NO_CHECK.equals(otherInCheckOptions)) {
            List<MaterialInSingleDetail> detailsNeedCheck = details
                    .stream()
                    .filter(detail -> Boolean.TRUE.equals(Dbs.getProp(detail.getGood(), BaseSetMaterial::getIsCheck)))
                    .collect(Collectors.toList());
            if (!detailsNeedCheck.isEmpty()) {
                Assert.isTrue(msModuleRelationService.checkModuleStatus(QCSInspect.QCS_MODULE_NAME),
                        InternationalResource.get("material.custom.moduleIsNotPublished", QCSInspect.QCS_MODULE_NAME));
                List<QCSCheckRequestDTO> dtoList = detailsNeedCheck.stream().map(detail -> {
                    // 区分来料和其他请检
                    return QCSCheckRequestDTOBuilder
                            .aQCSCheckRequestDTO()
                            .withBusiType(OtherInCheckOption.OTHER_CHECK.equals(otherInCheckOptions) ?
                                    QCSInspect.BUSINESS_TYPE_OTHER : QCSInspect.BUSINESS_TYPE_MATERIAL)
                            .withTableType(OtherInCheckOption.OTHER_CHECK.equals(otherInCheckOptions) ?
                                    QCSInspect.TABLE_TYPE_OTHER : QCSInspect.TABLE_TYPE_MATERIAL)
                            .withCreateDeptId(table.getCreateDepartmentId())
                            .withCreatePositionId(table.getCreatePositionId())
                            .withCreateStaffId(table.getCreateStaffId())
                            .withVendorId(table.getVendor() == null ? null : table.getVendor().getId())
                            .withSourceTableId(table.getId())
                            .withSourcTableNo(table.getTableNo())
                            .withSourceId(detail.getId())
                            .withProdId(detail.getGood().getId())
                            .withBatchCode(detail.getBatchText())
                            .withQuantity(detail.getAppliQuanlity())
                            .build();
                }).collect(Collectors.toList());
                //设置事务结束后调用QCS，避免死锁
                Transactions.appendEventAfterCommit(() -> self.saveCheckApply(dtoList, MaterialInSingleDetail.TABLE_NAME, "ID"));
//                saveCheckApply(dtoList, MaterialInSingleDetail.TABLE_NAME, "ID");
            }
        }
    }


    @Autowired
    private PlatformTransactionManager manager;

    /**
     * @param dtoList   请求实体，对应入库单抽象实体
     * @param tableName 入库单明细表名
     * @param srcIField 对应需要添加的入库单明细的id字段名称
     */
    public void saveCheckApply(List<QCSCheckRequestDTO> dtoList, String tableName, String srcIField) {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);// 事物隔离级别，开启新事务
        //从spring容器对象中获取DataSourceTransactionManager，这个根据配置文件中配置的id名（transactionManager）
        //获取事务状态对象
        TransactionStatus transactionStatus = manager.getTransaction(def);
        log.info("dtoList:" + JSON.toJSONString(dtoList));
        log.info("tableName:" + JSON.toJSONString(tableName));
        log.info("srcIField:" + JSON.toJSONString(srcIField));
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            dtoList.forEach(dto -> {
                String exchange = ModuleHttpClient.exchange(ModuleRequestContext.builder()
                                .moduleName(QCSInspect.QCS_MODULE_NAME)
                                .path(QCSInspect.CREATE_INSPECT_URL)
                                .method(HttpMethod.POST)
                                .httpHeaders(httpHeaders)
                                .body(JSON.toJSONString(dto))
                                .connectRequestTimeout(5 * 1000)
                                .log(true)
                                .build(),
                        String.class
                );
                JSONObject jsonObject = JSON.parseObject(exchange);
                String checkId = jsonObject.getJSONObject("data").getString("Id");
                Dbs.execute("UPDATE " + tableName + " SET CHECK_APPLY_ID = ? WHERE " + srcIField + " = ?", checkId, dto.getSourceId());
            });
            //提交事务
            manager.commit(transactionStatus);
        } catch (Exception e) {
            //回滚事务
            manager.rollback(transactionStatus);
            throw new BAPException("请检请求错误：" + e);
        }
    }
}
