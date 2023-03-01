package com.supcon.orchid.material.superwms.util;

import com.supcon.orchid.BaseSet.entities.BaseSetMaterial;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Strings;

public class BatchNumberConvertor {
    public static String getOriginBatchNum(BaseSetMaterial material, String batchNum){
        return Strings.valid(batchNum)?getOriginBatchNum(Dbs.getProp(material,BaseSetMaterial::getCode),batchNum):null;
    }

    public static String getOriginBatchNum(String materialCode, String batchNum){
        return Strings.valid(batchNum)?materialCode+"@"+batchNum:null;
    }

    public static String getBatchNum(String originBatchNum){
        return Strings.valid(originBatchNum)?originBatchNum.substring(originBatchNum.indexOf("@")+1):null;
    }
}
