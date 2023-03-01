package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.ec.entities.abstracts.AbstractAuditCidEntity;
import com.supcon.orchid.foundation.entities.SystemCode;
import com.supcon.orchid.i18n.InternationalResource;
import com.supcon.orchid.orm.entities.IEntity;
import lombok.SneakyThrows;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Consumer;

public class Entities {

    /**
     * 获取实体数据库表名
     * @param entityClazz 实体类
     * @return 数据库表名
     */
    public static String getTableName(Class<?> entityClazz) {
        return (String) Reflects.getValue(entityClazz,"TABLE_NAME");
    }

    /**
     * 获取实体编码
     * @param entityClazz 实体类
     * @return 实体编码
     */
    public static String getEntityCode(Class<?> entityClazz) {
        return (String) Reflects.getValue(entityClazz,"ENTITY_CODE");
    }

    /**
     * 获取实体SERVICE对象
     * @param entityClazz 实体类
     * @return SERVICE对象
     */
    @SneakyThrows
    public static Object getEntityService(Class<?> entityClazz) {
        //1.获取service名称
        int index = entityClazz.getName().lastIndexOf(".entities.");
        assert index!=-1;
        String serviceName = entityClazz
                .getName()
                .substring(0,index)
                .concat(entityClazz
                        .getName()
                        .substring(index)
                        .replaceFirst("entities","services")
                ).concat("Service");
        //2.获取Service对象
        Class<?> serviceClazz = Class.forName(serviceName);
        return Springs.getBean(serviceClazz);
    }

    /**
     * 根据ID创建简单实体
     * @param entityClazz 实体类
     * @param id ID值
     * @return 只有ID的实体
     */
    @SneakyThrows
    public static <I extends Serializable,E extends IEntity<I>> E ofId(Class<E> entityClazz, I id) {
        E entity = entityClazz.newInstance();
        entity.setId(id);
        return entity;
    }

    /**
     * 获取实体名
     * @param entityClazz 实体类
     */
    public static String getEntityName(Class<?> entityClazz){
        String packName = entityClazz.getName().substring(0,entityClazz.getName().lastIndexOf(".entities."));
        int moduleNameLength = packName.length()-(packName.lastIndexOf(".")+1);
        return entityClazz.getSimpleName().substring(moduleNameLength);
    }

    /**
     * 获取实体保存方法
     * @param entityClazz 实体类
     * @return 保存方法的Consumer对象
     */
    public static Consumer<? super AbstractAuditCidEntity> getSaver(Class<? extends AbstractAuditCidEntity> entityClazz){
        Object service = getEntityService(entityClazz);
        String entityName = getEntityName(entityClazz);
        return entity-> Reflects.call(service,"save"+entityName,entity,null);
    }


    @SneakyThrows
    public static void translateSystemCode(Object entity){
        List<Field> fields =  Reflects.getFields(entity.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            if(SystemCode.class.equals(field.getType())){
                SystemCode code = (SystemCode) field.get(entity);
                if(code!=null){
                    code.setValue(InternationalResource.get(code.getValue()));
                }
            }
        }
    }

    public static void translateSystemCode(SystemCode systemCode){
        if(systemCode!=null){
            systemCode.setValue(InternationalResource.get(systemCode.getValue()));
        }
    }

}
