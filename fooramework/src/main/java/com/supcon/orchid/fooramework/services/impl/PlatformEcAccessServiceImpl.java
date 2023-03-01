package com.supcon.orchid.fooramework.services.impl;

import com.supcon.orchid.ec.services.ViewServiceFoundation;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.util.ArrayOperator;
import com.supcon.orchid.fooramework.util.Dbs;
import com.supcon.orchid.fooramework.util.Strings;
import com.supcon.orchid.fooramework.services.PlatformEcAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PlatformEcAccessServiceImpl implements PlatformEcAccessService {

    @Override
    @Transactional
    public String getModelCodeByDgCode(String dgCode){
        return Dbs.first(
                "SELECT TARGETMODEL_CODE FROM EC_DATA_GRID WHERE CODE=?",
                String.class,
                dgCode
        );
    }

    @Override
    @Transactional
    public String getModelCodeByViewCode(String viewCode){
        return Dbs.first(
                "SELECT ASS_MODEL_CODE FROM EC_VIEW WHERE CODE=?",
                String.class,
                viewCode
        );
    }

    @Override
    @Transactional
    public Map<String,String> findMultiFieldsInfo(String viewCode, String methodName){
        //如果methodName存在dg，则需要根据dg code查询
        int dgIdx = methodName!=null?methodName.lastIndexOf("dg"):-1;
        Map<String,String> code$config = dgIdx!=-1?
                Dbs.binaryMap(
                        "SELECT CODE,CONFIG FROM EC_FIELD WHERE VALID=1 AND DATAGRID_CODE=? AND FIELD_KEY LIKE ?",
                        String.class,String.class,
                        viewCode+methodName.substring(dgIdx),"attrMap%"
                ):Dbs.binaryMap(
                "SELECT CODE,CONFIG FROM EC_FIELD WHERE VALID=1 AND VIEW_CODE=? AND FIELD_KEY LIKE ?",
                String.class,String.class,
                viewCode,"attrMap%"
        );
        return code$config.entrySet().stream().map(entry->{
            //取出propertyName，与code做映射
            String config = entry.getValue();
            List<String> contents = Strings.findPatternBetween(config,"\"propertyName\":",",");
            if(contents.isEmpty()){
                return null;
            } else {
                return Pair.of(contents.get(0).trim().replace("\"",""),entry.getKey());
            }
        }).filter(Objects::nonNull).collect(Collectors.toMap(
                Pair::getFirst,
                Pair::getSecond
        ));
    }

    @Autowired
    private ViewServiceFoundation viewServiceFoundation;

    private static final String RESULT_PREFIX = "result.";

    @Override
    @Transactional
    public String getDgIncludeWithFieldMapper(String dgCode, Map<String, String> mapper){
        String viewIncludes = viewServiceFoundation.generateProjectDataGridResultField(dgCode);
        return String.join(",",ArrayOperator.of(viewIncludes.split(",")).map(String.class,field->{
            if(field.startsWith(RESULT_PREFIX)){
                String innerField = field.substring(RESULT_PREFIX.length());
                int dotIdx = innerField.indexOf(".");
                String target = dotIdx>0?innerField.substring(0,dotIdx):innerField;
                if(mapper.containsKey(target)){
                    return innerField.replace(target,mapper.get(target));
                }
            }
            return null;
        }).filter(Objects::nonNull).concat("id").get());
    }
}
