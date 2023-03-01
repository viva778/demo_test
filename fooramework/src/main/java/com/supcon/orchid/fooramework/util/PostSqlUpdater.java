package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.ec.entities.abstracts.AbstractAuditCidEntity;
import com.supcon.orchid.fooramework.support.Converter;
import com.supcon.orchid.fooramework.support.Pair;
import lombok.SneakyThrows;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在afterSubmit时，缓存数据可能不会写入数据库
 * 使用这个工具类手动将变化过的数据写入数据库
 */
public class PostSqlUpdater {
    private final AbstractAuditCidEntity entity_snapshot;

    private final AbstractAuditCidEntity entity;

    public PostSqlUpdater(AbstractAuditCidEntity entity){
        assert entity!=null;
        //进行深拷贝，等待更新
        this.entity_snapshot = Beans.getCopy(entity);
        this.entity = entity;
    }

    /**
     * 只更新部分字段
     * @param entity 实体
     * @param fieldNames 使用逗号隔开的字段名
     */
    @SneakyThrows
    public static void updateByFields(AbstractAuditCidEntity entity, String fieldNames){
        if(entity!=null&&entity.getId()!=null){
            Map<String,Field> name$field = Reflects.getFields(entity.getClass()).stream().collect(Collectors.toMap(
                    Field::getName,
                    field -> field
            ));
            //字段列名映射
            Map<String,String> field$column = _GetFieldColumnMap(entity.getClass());
            Set<String> columns = new LinkedHashSet<>();
            List<Object> params = new LinkedList<>();

            for (String fieldName : fieldNames.split(",")) {
                int dotIdx = fieldName.indexOf(".");
                String mainFieldName = dotIdx<0?fieldName:fieldName.substring(0,dotIdx);
                Field field = name$field.get(mainFieldName);
                if(field!=null){
                    field.setAccessible(true);
                    Object val = field.get(entity);
                    _SetupColumnParam(val,field$column,columns,params,fieldName);
                }
            }
            //生成sql并更新
            if(!columns.isEmpty()){
                if(!columns.contains("DELETE_TIME=?")&&columns.add("MODIFY_TIME=?")){
                    params.add(new Date());
                }
                Dbs.execute(
                        "UPDATE "+ Entities.getTableName(entity.getClass())+" SET "+String.join(", ",columns)+" WHERE ID=? ",
                        Elements.toArray(params,entity.getId())
                );
            }
        }
    }

    @SneakyThrows
    public void update(){
        //1.将原实体和改变后的实体进行比对，更新不一样的字段
        List<Field> fields = Reflects.getFields(entity.getClass());

        LinkedHashSet<String> columns = new LinkedHashSet<>();
        LinkedList<Object> params = new LinkedList<>();
        //字段列名映射
        Map<String,String> field$column = _GetFieldColumnMap(entity.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            Object newValue = field.get(entity);
            Object oldValue = field.get(entity_snapshot);
            if(!Objects.equals(newValue, oldValue)){
                _SetupColumnParam(newValue,field$column,columns,params,field.getName());
            }
        }

        //2.生成sql并更新
        if(!columns.isEmpty()){
            if(!columns.contains("DELETE_TIME=?")&&columns.add("MODIFY_TIME=?")){
                params.add(new Date());
            }
            Dbs.execute(
                    "UPDATE "+ Entities.getTableName(entity.getClass())+" SET "+String.join(", ",columns)+" WHERE ID=? ",
                    Elements.toArray(params,entity.getId())
            );
        }
    }

    private static void _SetupColumnParam(Object val, Map<String,String> field$column, Set<String> columns, List<Object> params, String fieldName){
        //如果不相同，则取寻找对应列名并更新
        String updColumn;
        Object updValue;
        String column = field$column.get(fieldName);
        if(column!=null){
            updColumn = column;
            updValue = val;
            if("VALID".equalsIgnoreCase(column)&& !Converter.booleanConverter(val)){
                if(columns.add("DELETE_TIME=?")){
                    params.add(new Date());
                }
            }
        } else {
            String key = Strings.match(field$column.keySet(),fieldName+"\\..*");
            if(key!=null){
                updColumn = field$column.get(key);
                String refKey = key.split("\\.")[1];
                updValue = val!=null?Reflects.getValue(val,refKey):null;
            } else {
                updColumn = null;
                updValue = null;
            }
        }
        if(updColumn!=null){
            if(updValue!=null){
                if(columns.add(updColumn.toUpperCase()+"=?")){
                    params.add(_SqlTypeAdapt(updValue));
                }
            } else {
                columns.add(updColumn.toUpperCase()+"=NULL");
            }
        }
    }

    private static Object _SqlTypeAdapt(Object value) {
        if(value instanceof Boolean) {
            return ((boolean)value)?1:0;
        }
        return value;
    }

    private static final Map<Class<?>,Map<String,String>> clazzFcmMap = new HashMap<>();

    /**
     * 获取字段-列名映射
     * @param clazz 实体类
     * @return 映射
     */
    private static Map<String,String> _GetFieldColumnMap(Class<?> clazz){
        return clazzFcmMap.computeIfAbsent(clazz,k->{
            List<Method> methods = Reflects.getMethods(clazz);
            //1.创建字段-列名映射
            Map<String,String> fcm = methods.stream().map(method->{
                //只有以get或is开头的方法才有可能有列名标注
                if(method.getName().startsWith("get")){
                    return Pair.of(method,Strings.lowerCaseFirst(method.getName().substring(3)));
                } else if(method.getName().startsWith("is")){
                    return Pair.of(method,Strings.lowerCaseFirst(method.getName().substring(2)));
                } else {
                    return null;
                }
            }).filter(Objects::nonNull).map(pair->{
                Method method = pair.getFirst();
                String name = pair.getSecond();
                if(method.isAnnotationPresent(Column.class)){
                    //存在列名直接返回 xxx-列名
                    return Pair.of(name,Reflects.getValueFromAnnotation(method, Column.class,Column::name));
                } else if(method.isAnnotationPresent(JoinColumn.class)){
                    //存在引用则返回 xxx.引用字段-列名
                    JoinColumn annotation = method.getAnnotation(JoinColumn.class);
                    //xxx.ID->xxx.id
                    String joinField = Strings.camelIze(annotation.referencedColumnName());
                    String compound = name+"."+(Strings.valid(joinField)?joinField:"id");
                    return Pair.of(compound,annotation.name());
                }
                return null;
            }).filter(Objects::nonNull).filter(pair->Strings.valid(pair.getSecond())).collect(Collectors.toMap(
                    Pair::getFirst,
                    Pair::getSecond
            ));
            //2.增加AttributeOverrides中的映射
            AttributeOverride[] aos = Reflects.getValueFromAnnotation(clazz, AttributeOverrides.class,AttributeOverrides::value);
            for (AttributeOverride ao : aos) {
                fcm.put(ao.name(),ao.column().name());
            }
            return fcm;
        });
    }


    /**
     * 标记实体类值，后续根据manualUpdate方法更新
     * @param entity 实体
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractAuditCidEntity> T getManualWritableEntity(T entity){
        if(entity!=null){
            String cache_key = entity.getClass().getSimpleName()+":"+entity.getId();
            PostSqlUpdater updater = TransactionCaches.computeIfAbsent(cache_key,k-> new PostSqlUpdater(Beans.getCopy(entity)));
            return (T) updater.entity;
        } else {
            return null;
        }
    }

    /**
     * 与之前的标记进行对比，如果不同则进行更新
     * @param entity 实体
     */
    public static void manualUpdate(AbstractAuditCidEntity entity){
        PostSqlUpdater updater = TransactionCaches.get(entity.getClass().getSimpleName()+":"+entity.getId());
        if(updater!=null){
            updater.update();
        }
    }

    /**
     * 标记并在提交前进行保存变化
     * @param entity 实体
     */
    public static <T extends AbstractAuditCidEntity> T getAutoWritableEntity(T entity){
        if(entity!=null){
            T copy = getManualWritableEntity(entity);
            TransactionCaches.computeIfAbsent("__post_sql_list__",k->{
                Transactions.appendEventBeforeCommit(()->{
                    List<AbstractAuditCidEntity> entities = TransactionCaches.get("__post_sql_list__");
                    entities.forEach(PostSqlUpdater::manualUpdate);
                });
                return new LinkedList<>();
            }).add(copy);
            return copy;
        } else {
            return null;
        }
    }
}