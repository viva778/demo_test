package com.supcon.orchid.fooramework.util;

import com.supcon.orchid.fooramework.support.Converter;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.orm.dao.ExtendedGenericDao;
import com.supcon.orchid.orm.entities.IEntity;
import com.supcon.orchid.tree.TreeDaoImpl;
import com.supcon.orchid.tree.TreeNode;
import com.supcon.orchid.utils.OrchidUtils;
import lombok.SneakyThrows;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.data.util.CastUtils.cast;

public class Dbs {
    
    private static ExtendedGenericDao<?, Long> sqlDao;

    private static ExtendedGenericDao<?, Long> getDao(){
        return sqlDao!=null?sqlDao:(sqlDao=cast(Springs.getBean("sqlDao")));
    }

    /**
     * 将sql结果转为基本类型stream
     * @param sql SQL
     * @param basicType 数据类型
     * @param params SQL参数
     * @return 结果流
     */
    public static <T> Stream<T> stream(String sql, Class<T> basicType, Object... params){
        NativeQuery<?> query = getDao().createNativeQuery(sql,params);
        return query
                .list()
                .stream()
                .map(v-> Converter.getConverter(basicType).apply(v));
    }

    /**
     * 将sql结果转为stream
     * @param sql SQL
     * @param converter 结果转换器
     * @param params SQL参数
     * @return 结果流
     */
    public static <T> Stream<T> stream(String sql, Function<Object,T> converter, Object... params){
        NativeQuery<?> query = getDao().createNativeQuery(sql,params);
        return query
                .list()
                .stream()
                .map(converter);
    }

    /**
     * 得到sql第一条结果
     * @param sql SQL
     * @param basicType 数据类型
     * @param params SQL参数
     * @return 唯一结果
     */
    public static <T> T first(String sql, Class<T> basicType, Object...params){
        List<?> list = getDao().createNativeQuery(sql,params).setMaxResults(1).list();
        if(!list.isEmpty()) {
            return Converter.getConverter(basicType).apply(list.get(0));
        } else {
            return null;
        }
    }

    /**
     * 得到sql第一条结果
     * @param sql SQL
     * @param converter 结果转换器
     * @param params SQL参数
     * @return 唯一结果
     */
    public static <T> T first(String sql, Function<Object,T> converter, Object...params){
        List<?> list = getDao().createNativeQuery(sql,params).setMaxResults(1).list();
        if(!list.isEmpty()) {
            return converter.apply(list.get(0));
        } else {
            return null;
        }
    }

    /**
     * 查询数据是否存在
     * @param tableName 表名
     * @param condition SQL条件
     * @param params SQL参数
     * @return 是否存在
     */
    public static boolean exist(String tableName,String condition, Object...params) {
        return !getDao()
                .createNativeQuery("SELECT 1 FROM " + tableName + " WHERE " + condition, params)
                .setMaxResults(1)
                .list()
                .isEmpty();
    }

    /**
     * 统计数据个数
     * @param tableName 表名
     * @param condition SQL条件
     * @param params SQL参数
     * @return 数据个数
     */
    public static int count(String tableName,String condition, Object...params) {
        return Optional.ofNullable(first(
                "SELECT COUNT(*) FROM "+tableName+" WHERE "+condition,
                Integer.class,
                params
        )).orElse(0);
    }


    /**
     * 将第一列和第二列的数据统计为MAP并返回
     * @param sql SQL
     * @param keyType KEY对应基本类型
     * @param valueType VALUE对应基本类型
     * @param params SQL参数
     * @return MAP结果
     */
    public static <K, V> Map<K, V> binaryMap(String sql, Class<K> keyType, Class<V> valueType, Object...params){
        NativeQuery<?> query = getDao().createNativeQuery(sql,params);
        return query
                .list()
                .stream()
                .map(arr->(Object[])arr)
                .filter(arr->arr[1]!=null)
                .collect(
                        Collectors.toMap(
                                arr->Converter.getConverter(keyType).apply(arr[0]),
                                arr->Converter.getConverter(valueType).apply(arr[1]),
                                (existing, replacement) -> existing
                        )
                );
    }

    /**
     * sql结果转map列表 注意不同数据库大小写不一致
     * @param sql SQL
     * @param params SQL参数
     * @return 结果MAP列表,KEY为列明，VALUE为值
     */
    public static <X> List<Map<String, X>> aliasToMapList(String sql, Object...params){
        NativeQuery<?> query = getDao().createNativeQuery(sql,params);
        return cast(query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).getResultList());
    }

    /**
     * sql第一条结果转map 注意不同数据库大小写不一致
     * @param sql SQL
     * @param params SQL参数
     * @return 结果MAP,KEY为列明，VALUE为值
     */
    public static <X> Map<String, X> aliasToMapFirst(String sql, Object...params){
        NativeQuery<?> query = getDao().createNativeQuery(sql,params);
        List<Map<String, X>> list = cast(query.setMaxResults(1).setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP).getResultList());
        return !list.isEmpty() ?list.get(0):Collections.emptyMap();
    }

    /**
     * 将第一条SQL结果的第一列，第二列转为PAIR并返回
     * @param sql SQL
     * @param firstType FIRST类型
     * @param secondType SECOND类型
     * @param params SQL参数
     * @return PAIR结果
     */
    public static <F,S> Pair<F,S> pair(String sql, Class<F> firstType, Class<S> secondType, Object...params){
        NativeQuery<?> query = getDao().createNativeQuery(sql,params);
        List<?> list = query.setMaxResults(1).list();
        if(!list.isEmpty() && list.get(0) instanceof Object[]){
            Object[] data = (Object[]) list.get(0);
            if(data.length>=2){
                return Pair.of(
                        Converter.getConverter(firstType).apply(data[0]),
                        Converter.getConverter(secondType).apply(data[1])
                );
            }
        }
        return null;
    }

    /**
     * 将SQL结果的第一列，第二列转为PAIR并返回
     * @param sql SQL
     * @param firstType FIRST类型
     * @param secondType SECOND类型
     * @param params SQL参数
     * @return PAIR结果
     */
    public static <F,S> List<Pair<F,S>> pairList(String sql, Class<F> firstType, Class<S> secondType, Object...params){
        NativeQuery<?> query = getDao().createNativeQuery(sql,params);
        List<?> list = query.list();
        if(!list.isEmpty() && list.get(0) instanceof Object[]){
            Function<Object,F> fMapper = Converter.getConverter(firstType);
            Function<Object,S> sMapper = Converter.getConverter(secondType);
            return list.stream().map(data->Pair.of(
                    fMapper.apply(((Object[])data)[0]),
                    sMapper.apply(((Object[])data)[1])
            )).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 执行SQL语句
     * @param sql SQL
     * @param params SQL参数
     */
    public static void execute(String sql,Object...params){
        getDao().createNativeQuery(sql,params).executeUpdate();
    }


    /**
     * 根据SQL条件载入实体
     * @param entityClass 实体类型
     * @param condition SQL条件
     * @param params SQL参数
     * @return 实体结果
     */
    @SneakyThrows
    public static <T> T load(Class<T> entityClass, String condition, Object... params) {
        String tableName = (String)entityClass.getField("TABLE_NAME").get(null);
        NativeQuery<T> query = getDao().createNativeQuery("select * from " + tableName + " where " + condition,entityClass,params);
        List<T> list = query.setMaxResults(1).list();
        return !list.isEmpty() ?list.get(0):null;
    }

    /**
     * 根据ID载入实体
     * @param entityClass 实体类型
     * @param id 数据ID
     * @return 实体结果
     */
    @SneakyThrows
    public static <PK extends Serializable,T extends IEntity<PK>> T load(Class<T> entityClass, PK id) {
        return getDao().getSessionFactory().getCurrentSession().get(entityClass,id);
    }

    /**
     * 重新从数据库加载数据
     * @param entity 原实体
     */
    @SuppressWarnings("unchecked")
    public static <T extends IEntity<?>> T reload(T entity){
        return entity!=null&&entity.getId()!=null?(T) Dbs.load(entity.getClass(),entity.getId()):null;
    }

    /**
     * 从实体中加载属性（如果为空则会重新加载
     * @param entity 实体
     * @param getter 属性获取器
     * @return 属性值
     */
    public static <T extends IEntity<?>,R> R getProp(T entity, Function<T,R> getter) {
        if(entity!=null){
            R value = getter.apply(entity);
            if(value!=null && (!(value instanceof IEntity) || ((IEntity<?>)value).getId()!=null) ){
                //有值，且当为实体时ID不为空
                return value;
            }
            if(entity.getId()!=null){
                //未获取到值，重新加载对象
                return getter.apply(Dbs.reload(entity));
            }
        }
        return null;
    }

    /**
     *
     * 从实体中加载属性（如果为空则会重新加载
     * @param entity 实体
     * @param getter1 获取器1
     * @param getter2 获取器2
     * @return 属性值
     */
    public static <T extends IEntity<?>,R extends IEntity<?>, S> S getProp(T entity, Function<T,R> getter1, Function<R,S> getter2) {
        R rv = getProp(entity,getter1);
        if(rv!=null){
            return getProp(rv,getter2);
        }
        return null;
    }


    /**
     * 根据SQL条件查询实体列表
     * @param entityClass 实体类
     * @param condition SQL条件
     * @param params SQL参数
     * @return 实体列表
     */
    @SneakyThrows
    public static <T> List<T> findByCondition(Class<T> entityClass, String condition, Object... params) {
        String tableName = (String)entityClass.getField("TABLE_NAME").get(null);
        NativeQuery<T> query = getDao().createNativeQuery("select * from " + tableName + " where " + condition,entityClass,params);
        return query.list();
    }

    private static final int MAX_COUNT = 500;

    /**
     * 获取拼接的IN条件，以500为最大分割界限
     *  如列名为ID，大小为900时，返回
     *  (ID IN (?...500...?) OR ID IN(?...400...?))
     * @param columnName 列名
     * @param count 个数
     * @return SQL条件
     */
    public static String inCondition(String columnName,int count){
        if(count!=0){
            StringBuilder builder = new StringBuilder();
            if(count>MAX_COUNT){
                int repeatCnt = count/MAX_COUNT;
                int lastCnt = count%MAX_COUNT;
                String head = "(";
                String tail = ")";
                String padHead = "("+columnName+" IN (";
                String pad500 = String.join(",", Collections.nCopies(MAX_COUNT,"?"));
                String padLast = String.join(",",Collections.nCopies(lastCnt,"?"));
                String padTail = "))";
                String delimiter = " OR ";
                builder.append(head);
                builder.append(padHead).append(pad500).append(padTail);
                for(int i=1;i<repeatCnt;i++){
                    builder.append(delimiter).append(padHead).append(pad500).append(padTail);
                }
                if(lastCnt!=0){
                    builder.append(delimiter).append(padHead).append(padLast).append(padTail);
                }
                builder.append(tail);
            } else {
                builder.append(columnName).append(" IN (").append(String.join(",",Collections.nCopies(count,"?"))).append(")");
            }
            return builder.toString();
        } else {
            return "1=0";
        }
    }

    /**
     * 获取拼接的NOT IN条件，以500为最大分割界限
     *  如列名为ID，大小为900时，返回
     *  (ID NOT IN (?...500...?) AND ID NOT IN(?...400...?))
     * @param columnName 列名
     * @param count 个数
     * @return SQL条件
     */
    public static String notInCondition(String columnName,int count){
        if(count!=0){
            StringBuilder builder = new StringBuilder();
            if(count>MAX_COUNT){
                int repeatCnt = count/MAX_COUNT;
                int lastCnt = count%MAX_COUNT;
                String head = "(";
                String tail = ")";
                String padHead = "("+columnName+" NOT IN (";
                String pad500 = String.join(",", Collections.nCopies(MAX_COUNT,"?"));
                String padLast = String.join(",",Collections.nCopies(lastCnt,"?"));
                String padTail = "))";
                String delimiter = " AND ";
                builder.append(head);
                builder.append(padHead).append(pad500).append(padTail);
                for(int i=1;i<repeatCnt;i++){
                    builder.append(delimiter).append(padHead).append(pad500).append(padTail);
                }
                if(lastCnt!=0){
                    builder.append(delimiter).append(padHead).append(padLast).append(padTail);
                }
                builder.append(tail);
            } else {
                builder.append(columnName).append(" NOT IN (").append(String.join(",",Collections.nCopies(count,"?"))).append(")");
            }
            return builder.toString();
        } else {
            return "1=1";
        }
    }

    /**
     * 保存树节点
     * @param entity 树节点实体
     */
    @SneakyThrows
    public static <T extends TreeNode<T, PK>, PK extends Serializable> void saveTreeNode(T entity){
        //重写了平台TreeDao方法
        Class<T> clazz = cast(entity.getClass());
        Class<PK> pkClass = cast(clazz.getMethod("getParentId").getReturnType());
        //设置父节点
        PK negative_1 = pkClass.getConstructor(String.class).newInstance("-1");
        T parent = entity.getParentId()!=null&&!negative_1.equals(entity.getParentId())?
                load(clazz,entity.getParentId()):null;
        if (parent!=null) {
            entity.setParent(parent);
        } else if(entity.getParentId()==null){
            entity.setParentId(negative_1);
        }

        if (null == entity.getId()) {
            //保存新增数据
            entity.setLeaf(true);
            save(entity);
        } else {
            //递归修改子节点树信息
            recursiveUpdateFullPathName(entity);
        }

        //修改父节点树信息
        if (parent!=null&&!Boolean.FALSE.equals(parent.isLeaf())) {
            parent.setLeaf(false);
            merge(parent);
        }

        //保存层级信息
        if (null != parent) {
            entity.setLayRec(parent.getLayRec() + "-" + entity.getId());
            entity.setFullPathName(parent.getFullPathName() + "/" + OrchidUtils.getMainDisplayValue(entity));
            entity.setLayNo(parent.getLayNo() + 1);
        } else {
            entity.setLayRec(entity.getId().toString());
            entity.setFullPathName(OrchidUtils.getMainDisplayValue(entity));
            entity.setLayNo(1);
        }
        merge(entity);
        flush();

        Set<String> ks = getTreeKeys().get(clazz.getName());
        if (null != ks && !ks.isEmpty()) {
            ks.forEach(getTreeCache()::evict);
        }
    }

    private static <T extends TreeNode<T, PK>, PK extends Serializable> void recursiveUpdateFullPathName(T entity) {
        Class<T> clazz = cast(entity.getClass());
        List<T> children = findByCondition(clazz,"VALID=1 AND PARENT_ID=?",entity.getId());
        for (T child : children) {
            child.setFullPathName(entity.getFullPathName() + "/" + OrchidUtils.getMainDisplayValue(child));
            update(child);
            recursiveUpdateFullPathName(child);
        }
    }

    private static Cache getTreeCache(){
        return Springs.getBean(CacheManager.class).getCache("_tree");
    }

    private static Map<String, Set<String>> getTreeKeys(){
        return cast(Reflects.getValue(TreeDaoImpl.class,"keys"));
    }

    //------------------------------------------DAO层方法------------------------------------------

    public static void flush(){
        getDao().flush();
    }

    public static void clear(){
        getDao().clear();
    }

    public static void evict(IEntity<?> entity){
        getDao().evict(cast(entity));
    }

    public static void save(IEntity<?> entity){
        getDao().save(cast(entity));
    }

    public static void update(IEntity<?> entity){
        getDao().update(cast(entity));
    }

    public static void merge(IEntity<?> entity){
        getDao().merge(cast(entity));
    }

    public static void refresh(IEntity<?> entity){
        getDao().refresh(cast(entity));
    }

    public static <X> NativeQuery<X> createNativeQuery(String sqlQuery,  Object... objects){
        return cast(getDao().createNativeQuery(sqlQuery,objects));
    }
}
