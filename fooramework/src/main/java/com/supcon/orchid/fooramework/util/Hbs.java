package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.ec.entities.abstracts.AbstractAuditCidEntity;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.orm.dao.ExtendedGenericDao;
import com.supcon.orchid.orm.entities.IEntity;
import org.hibernate.criterion.*;
import org.hibernate.transform.Transformers;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.springframework.data.util.CastUtils.cast;

public class Hbs {
    private static ExtendedGenericDao<?, Long> sqlDao;

    private static ExtendedGenericDao<?, Long> getDao(){
        return sqlDao!=null?sqlDao:(sqlDao=cast(Springs.getBean("sqlDao")));
    }

    /**
     * 通过criteria查询实体列表,且只查询指定字段
     * @param entityClass 实体类
     * @param projections 包含字段名
     * @param criterionList criterionList
     * @return 实体列表
     */
    public static <T> List<T> findByCriteriaWithProjections(Class<T> entityClass, String projections, Criterion...criterionList){
        String alias = "_"+ entityClass.getSimpleName().toLowerCase();
        DetachedCriteria dc = DetachedCriteria.forClass(entityClass,alias);
        ProjectionList pList = Projections.projectionList();
        for (String projection : projections.split(",")) {
            pList.add(Projections.property(alias + "." + projection).as(projection));
        }
        dc.setProjection(pList);
        dc.setResultTransformer(Transformers.aliasToBean(entityClass));
        for (Criterion criterion : criterionList) {
            dc.add(criterion);
        }
        return cast(getDao().findByCriteria(dc));
    }

    public static Criterion getSubQueryCriterion(String property, String subProperty, Class<?> subClass, Criterion... subCriterionList){
        DetachedCriteria inCriteria = DetachedCriteria.forClass(subClass);
        inCriteria.setProjection(Property.forName(subProperty));
        for (Criterion criterion : subCriterionList) {
            inCriteria.add(criterion);
        }
        return Property.forName(property).in(inCriteria);
    }
    
    public static <T extends AbstractAuditCidEntity> List<T> findByCriteria(Class<T> entityClass, Criterion... criterionList){
        DetachedCriteria dc = DetachedCriteria.forClass(entityClass);
        dc.setResultTransformer(Transformers.aliasToBean(entityClass));
        for (Criterion criterion : criterionList) {
            dc.add(criterion);
        }
        return cast(getDao().findByCriteria(dc));
    }

    /**
     * 使用includes查询实体，只查询指定字段，包括子对象字段
     * @param entityClass 实体
     * @param includes 包含字段
     * @param criterionList criterionList
     * @return 实体列表
     */
    public static <X extends Serializable,T extends IEntity<X>> List<T> findByCriteriaWithIncludes(Class<T> entityClass, String includes, Criterion... criterionList){
        Pair<String[], Map<String,List<String>>> pair = _PropertyLayer.info(includes.split(","));
        String[] directIncludes = pair.getFirst();
        Map<String,List<String>> fieldName$subIncludes = pair.getSecond();
        //查询直接属性
        List<T> result = Hbs.findByCriteriaWithProjections(entityClass, String.join(",",directIncludes), criterionList);
        if(fieldName$subIncludes!=null){
            //子属性递归
            fieldName$subIncludes.forEach((fieldName,subIncludes)-> _RecQueryEntity(
                    result,
                    Reflects.getField(entityClass,fieldName),
                    subIncludes
            ));
        }
        return result;
    }

    public static <I extends Serializable,E extends IEntity<I>> E loadWithIncludes(Class<E> entityClass, String includes, I id){
        List<E> result = findByIdsWithIncludes(entityClass,includes,Collections.singletonList(id));
        return result.size()>0?result.get(0):null;
    }

    public static <I extends Serializable,E extends IEntity<I>> E loadWithIncludes(Class<E> entityClass, String includes, Criterion... criterionList){
        List<E> result = findByCriteriaWithIncludes(entityClass,includes,criterionList);
        return result.size()>0?result.get(0):null;
    }


    /**
     * 使用ids和includes查询实体，只查询指定字段，包括子对象字段
     * @param entityClass 实体
     * @param includes 包含字段
     * @param ids 实体ID列表
     * @return 实体列表
     */
    public static <I extends Serializable,E extends IEntity<I>> List<E> findByIdsWithIncludes(Class<E> entityClass, String includes, List<I> ids){
        Pair<String[], Map<String,List<String>>> pair = _PropertyLayer.info(includes.split(","));
        String[] directIncludes = pair.getFirst();
        Map<String,List<String>> fieldName$subIncludes = pair.getSecond();
        //查询直接属性
        List<E> result = _GetDirectEntityList(entityClass,String.join(",",directIncludes),ids);
        if(fieldName$subIncludes!=null){
            //子属性递归
            fieldName$subIncludes.forEach((fieldName,subIncludes)-> _RecQueryEntity(
                    result,
                    Reflects.getField(entityClass,fieldName),
                    subIncludes
            ));
        }
        return result;
    }


    private static <IS extends Serializable, ES extends IEntity<IS>,IM extends Serializable, EM extends IEntity<IM>> void _RecQueryEntity(
            List<EM> parentList,
            Field field,
            List<String> includes
    ){
        Pair<String[], Map<String,List<String>>> pair = _PropertyLayer.info(includes.toArray(new String[0]));
        String[] directIncludes = pair.getFirst();
        Map<String,List<String>> fieldName$subIncludes = pair.getSecond();
        //查询主表子表ID对应
        Class<ES> subClass = cast(field.getType());
        Class<IS> subIdClass = cast(Reflects.getField(subClass,"id").getType());
        Class<IM> mainIdClass = cast(Reflects.getField(field.getDeclaringClass(),"id").getType());
        Map<IM, IS> main$sub = Dbs.binaryMap(
                "SELECT ID,"+ Strings.upperUnderLine(field.getName())+" FROM "+ Entities.getTableName(field.getDeclaringClass())+" WHERE "+Dbs.inCondition("ID",parentList.size()),
                mainIdClass,subIdClass,
                parentList.stream().map(IEntity::getId).toArray()
        );
        //查询对应子表信息
        List<ES> result = _GetDirectEntityList(subClass,String.join(",",directIncludes),main$sub.values().stream().filter(Objects::nonNull).distinct().collect(Collectors.toList()));
        //根据id对应关系将结果设置回字段
        if(!result.isEmpty()){
            Map<IS, ES> id$sub = result.stream().collect(Collectors.toMap(
                    IEntity::getId,
                    v->v
            ));
            parentList.forEach(parent->{
                IS subId = main$sub.get(parent.getId());
                if(subId!=null){
                    ES sub = id$sub.get(subId);
                    Reflects.setValue(parent,field,sub);
                }
            });
            //然后递归调用
            if(fieldName$subIncludes!=null){
                fieldName$subIncludes.forEach((fieldName,subIncludes)-> _RecQueryEntity(
                        result,
                        Reflects.getField(field.getType(),fieldName),
                        subIncludes
                ));
            }
        }
    }

    private static <I extends Serializable,E extends IEntity<I>> List<E> _GetDirectEntityList(Class<E> entityClass, String includes, List<I> ids){
        if(!ids.isEmpty()){
            if(!Strings.valid(includes)||"id".equals(includes)){
                return ids.stream().map(id-> Entities.ofId(entityClass,id)).collect(Collectors.toList());
            } else {
                //使用hql查询实体
                return findByCriteriaWithProjections(
                        entityClass,
                        includes,
                        Restrictions.in("id",ids)
                );
            }
        }
        return Collections.emptyList();
    }

}
