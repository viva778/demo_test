package com.supcon.orchid.material.superwms.util.adptor;

import com.supcon.orchid.ec.entities.abstracts.AbstractEcFullEntity;
import com.supcon.orchid.ec.entities.abstracts.AbstractEcPartEntity;
import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.data.util.CastUtils.cast;

public class AdaptorCenter {

    private static final Map<String, TableTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity>> ADAPTOR_MAP = new HashMap<>();

    @SneakyThrows
    public static void registerTableTypeAdaptor(String bizType, Class<? extends TableTypeAdaptor<?,?>> clazz){
        ADAPTOR_MAP.put(bizType,cast(clazz.newInstance()));
    }

    public static TableTypeAdaptor<AbstractEcFullEntity,AbstractEcPartEntity> getTypeAdaptor(String bizType){
        return ADAPTOR_MAP.get(bizType);
    }

    private static final Map<String, InboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity>> INBOUND_MAP = new HashMap<>();

    @SneakyThrows
    public static void registerInboundTypeAdaptor(String bizType, Class<? extends InboundTypeAdaptor<?,?>> clazz){
        InboundTypeAdaptor<?,?> adaptor = clazz.newInstance();
        INBOUND_MAP.put(bizType,cast(adaptor));
        ADAPTOR_MAP.put(bizType,cast(adaptor));
    }

    public static InboundTypeAdaptor<AbstractEcFullEntity,AbstractEcPartEntity> getInboundTypeAdaptor(String bizType){
        return INBOUND_MAP.get(bizType);
    }



    private static final Map<String, OutboundTypeAdaptor<AbstractEcFullEntity, AbstractEcPartEntity>> OUTBOUND_MAP = new HashMap<>();

    @SneakyThrows
    public static void registerOutboundTypeAdaptor(String bizType, Class<? extends OutboundTypeAdaptor<?,?>> clazz){
        OutboundTypeAdaptor<?,?> adaptor = clazz.newInstance();
        OUTBOUND_MAP.put(bizType,cast(adaptor));
        ADAPTOR_MAP.put(bizType,cast(adaptor));
    }

    public static OutboundTypeAdaptor<AbstractEcFullEntity,AbstractEcPartEntity> getOutboundTypeAdaptor(String bizType){
        return OUTBOUND_MAP.get(bizType);
    }


    private static final Map<String, BizTypeContext> BIZ_TYPE_MAP = new HashMap<>();

    public static void registerBizTypeContext(BizTypeContext context){
        BIZ_TYPE_MAP.put(context.getBizType(),context);
        BIZ_TYPE_MAP.put(context.getPrefix(),context);
    }

    public static BizTypeContext getBizTypeContext(String bizTypeOrPrefix){
        return BIZ_TYPE_MAP.get(bizTypeOrPrefix);
    }
}
