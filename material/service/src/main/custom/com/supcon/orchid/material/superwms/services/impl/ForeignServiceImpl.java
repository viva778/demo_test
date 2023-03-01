package com.supcon.orchid.material.superwms.services.impl;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.BaseSet.entities.BaseSetStoreSet;
import com.supcon.orchid.BaseSet.entities.BaseSetWarehouse;
import com.supcon.orchid.fooramework.util.Dates;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Maps;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.material.entities.MaterialBatchInfo;
import com.supcon.orchid.material.services.MaterialBatchInfoService;
import com.supcon.orchid.material.services.MaterialCheckService;
import com.supcon.orchid.material.services.MaterialStandingcropService;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseBatchType;
import com.supcon.orchid.material.superwms.constants.systemcode.BaseCheckState;
import com.supcon.orchid.material.superwms.services.ForeignService;
import com.supcon.orchid.material.superwms.services.ModelBatchInfoService;
import com.supcon.orchid.material.superwms.services.ModelStockSummaryService;
import com.supcon.orchid.material.superwms.util.StockOperator;
import com.supcon.orchid.material.superwms.util.factories.BatchInfoFactory;
import com.supcon.orchid.services.BAPException;
import com.supcon.supfusion.framework.cloud.common.context.RpcContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ForeignServiceImpl implements ForeignService {

    @Autowired
    private MaterialCheckService checkService;
    @Autowired
    private MaterialBatchInfoService materialBatchInfoService;

    @Autowired
    private ModelBatchInfoService modelBatchInfoService;
    @Autowired
    private MaterialStandingcropService standingcropService;
    @Autowired
    private ModelStockSummaryService modelStockSummaryService;


    @Override
    @Transactional
    public Map<String, String> syncStandingCrop(Map<String, Object> inOutMap) {
        List<Map<String, Object>> data = (List<Map<String, Object>>) inOutMap.get("data");
        if (data == null || data.isEmpty()) {
            return Maps.immutable(
                    "resFlag", "SUCCESS"
            );
        }
        StringBuilder errorMessage = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            int row = i + 1;
            Map<String, Object> datum = data.get(i);
            // 仓库效验 效验成功返回供仓库对象，失败返回空对象。
            BaseSetWarehouse ware = checkService.checkWare(datum);
            if (ware == null) {
                errorMessage.append(InternationalResource.get("material.custom.code.not.found.warehose", getCurrentLanguage(), row, inOutMap.get("wareCode"))).append(";");
                continue;
            }
            //物料
            BaseSetMaterial baseSetMaterial = checkService.checkGood(datum);
            if (baseSetMaterial == null) {
                errorMessage.append(InternationalResource.get("material.custom.code.not.found.good", getCurrentLanguage(), row, inOutMap.get("goodCode"))).append(";");
                continue;
            }
            Boolean storesetState = ware.getStoresetState();
            //货位
            BaseSetStoreSet baseSetStoreSet = checkService.checkStoreSet(datum);
            if (storesetState && baseSetStoreSet == null) {
                errorMessage.append(InternationalResource.get("material.custom.code.not.found.storeSet", getCurrentLanguage(), row, inOutMap.get("placeSetCode"))).append(";");
                continue;
            }
            //批号
            String batch = checkService.checkInBatch(datum);
            //是否启用批次？
            boolean enableBatch = BaseBatchType.isEnable(Dbs.getProp(baseSetMaterial, BaseSetMaterial::getIsBatch));
            if (enableBatch && batch == null) {
                errorMessage.append(InternationalResource.get("material.custom.code.not.found.batchNull", getCurrentLanguage(), row)).append(";");
                continue;
            }
            if (datum.get("availiNum") == null || !Strings.valid(datum.get("availiNum").toString())) {
                errorMessage.append(InternationalResource.get("material.custom.code.not.found.availiNum", getCurrentLanguage(), row)).append(";");
                continue;
            }
            StockOperator stockOperator = StockOperator.of(baseSetMaterial.getId(), enableBatch ? batch : null, ware.getId(), baseSetStoreSet == null ? null : baseSetStoreSet.getId());
            BigDecimal forzenNum = datum.get("forzenNum") != null && Strings.valid(datum.get("forzenNum").toString()) ? new BigDecimal(datum.get("forzenNum").toString()) : new BigDecimal(0);
            BigDecimal availiNum = new BigDecimal(datum.get("availiNum").toString());
            if (stockOperator.getStock() == null) {
                if (enableBatch) {
                    //生成批次信息
                    MaterialBatchInfo batchInfo = modelBatchInfoService.getBatchInfo(baseSetMaterial.getId(), batch);
                    if (batchInfo == null) {
                        batchInfo = BatchInfoFactory.standingcropBatchInfo(baseSetMaterial, batch);
                        //保存批次信息
                        materialBatchInfoService.saveBatchInfo(batchInfo, null);
                    }
                }
                //更新现存量
                stockOperator.increase(availiNum.add(forzenNum));
                stockOperator.freeze(forzenNum);
                stockOperator.update(s -> {
                    s.setCheckState(new SystemCode(BaseCheckState.UNNECESSARY));
                    s.setCheckDate(Dates.getDateWithoutTime(new Date()));
                    s.setIsAvailable(true);
                    s.setInStoreTime(Dates.getDateWithoutTime(new Date()));
                });
            } else {
                BigDecimal changeOnhand = forzenNum.add(availiNum).subtract(stockOperator.getStock().getOnhand());
                //解冻所有现存量
                stockOperator.restore(stockOperator.getStock().getFrozenQuantity());
                //增加现存量差值
                stockOperator.increase(changeOnhand);
                //冻结新现存量
                stockOperator.freeze(forzenNum);
            }
        }
        if (!errorMessage.toString().isEmpty()) {
            throw new BAPException(errorMessage.toString());
        }
        return Maps.immutable(
                "resFlag", "SUCCESS"
        );
    }

    public String getCurrentLanguage() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
            Cookie[] cookies = request.getCookies();
            if (null != cookies) {
                Optional<Cookie> language = Arrays.stream(cookies).filter(cookie -> "language".equals(cookie.getName())).findAny();
                if (language.isPresent()) {
                    return language.get().getValue();
                }
            }
        }

        Locale locale = RpcContext.getContext().getLanguage();
        if (null == locale) {
            return "zh_CN";
        }
        return locale.toString();
    }

}
