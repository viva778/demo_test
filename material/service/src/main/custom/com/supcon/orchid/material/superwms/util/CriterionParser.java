package com.supcon.orchid.material.superwms.util;

import com.supcon.orchid.ec.entities.abstracts.AbstractAuditCidEntity;
import com.supcon.orchid.fooramework.support.Converter;
import com.supcon.orchid.fooramework.support.Pair;
import com.supcon.orchid.fooramework.support.Wrapper;
import com.supcon.orchid.fooramework.util.Reflects;
import com.supcon.orchid.fooramework.util.Strings;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Restrictions;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CriterionParser {

    public static Criterion getCriterion(String expression, Class<? extends AbstractAuditCidEntity> clazz) {
        Wrapper<Boolean> in_s_quote = new Wrapper<>(false);
        List<Strings.Snapshot> snapshots = Strings.getSnapshots(
                expression,
                Arrays.asList("'", "(", ")", " and ", "&", " or ", "|", "=", "!=", "<>", ">", "<", "<=", ">=", ",")
        ).stream().filter(snapshot -> {
            //过滤掉''及其内部的关键字
            if (snapshot.getPattern().equals("'")) {
                in_s_quote.set(!in_s_quote.get());
                return false;
            } else {
                return !in_s_quote.get();
            }
        }).collect(Collectors.toList());

       return _RecGetCriterion(
               expression,
               clazz,
               snapshots,
               0,
               expression.length()
       );
    }


    private static Criterion _RecGetCriterion(String expression, Class<? extends AbstractAuditCidEntity> clazz, List<Strings.Snapshot> snapshots, int begin_idx, int end_idx) {
        //先进行逻辑表达式拆分
        return _GetLogicCriterion(expression, clazz, snapshots, begin_idx, end_idx);
    }

    private static Criterion _GetLogicCriterion(String expression, Class<? extends AbstractAuditCidEntity> clazz, List<Strings.Snapshot> snapshots, int begin_idx, int end_idx) {
        //先,再or再and 遇到()先跳过
        int bracket_cnt = 0;//括号计数
        Wrapper<Integer> w_last_idx = new Wrapper<>(begin_idx);//上一个索引
        Wrapper<Integer> w_last_priority = new Wrapper<>(1); //上一个标记优先级，越小越高 1-and 2-or ,-3 最大-4
        LinkedList<Pair<Criterion, Integer>> criterion_list = new LinkedList<>();
        //输入优先级，将之前优先级高的条件先结合
        Consumer<Integer> combine_criteria = (priority) -> {
            if (priority > w_last_priority.get()) {
                if (criterion_list.size() > 1) {
                    LinkedList<Criterion> criteria_to_combine = new LinkedList<>();
                    while (criterion_list.size()>0&&criterion_list.peekLast().getSecond()<priority){
                        int last_max_priority = criterion_list.peekLast().getSecond();
                        int nxt_priority = last_max_priority;
                        while (nxt_priority==last_max_priority){
                            criteria_to_combine.addFirst(criterion_list.pollLast().getFirst());
                            if(criterion_list.size()>0){
                                nxt_priority = criterion_list.peekLast().getSecond();
                            } else {
                                nxt_priority = priority;
                                break;
                            }
                        }
                        if(criterion_list.size()>0){
                            criteria_to_combine.add(criterion_list.pollLast().getFirst());
                        }
                        switch (last_max_priority) {
                            case 3:
                            case 1: {
                                criterion_list.add(Pair.of(
                                        Restrictions.and(criteria_to_combine.toArray(new Criterion[0])),
                                        nxt_priority
                                ));
                                break;
                            }
                            case 2: {
                                criterion_list.add(Pair.of(
                                        Restrictions.or(criteria_to_combine.toArray(new Criterion[0])),
                                        nxt_priority
                                ));
                                break;
                            }
                        }
                        criteria_to_combine.clear();
                    }
                }
            }
            w_last_priority.set(priority);
        };
        for (Strings.Snapshot snapshot : snapshots) {
            int priority = 0;
            switch (snapshot.getPattern()) {
                case "(": {
                    bracket_cnt++;
                    break;
                }
                case ")": {
                    bracket_cnt--;
                    break;
                }
                case "&":
                case " and ": {
                    if (bracket_cnt == 0) {
                        priority = 1;
                    }
                    break;
                }
                case "|":
                case " or ": {
                    if (bracket_cnt == 0) {
                        priority = 2;
                    }
                    break;
                }
                case ",":{
                    if (bracket_cnt == 0) {
                        priority = 3;
                    }
                    break;
                }
            }
            if (priority > 0) {
                criterion_list.add(Pair.of(
                        _GetAtomicCriterion(
                                expression,
                                clazz,
                                snapshots.stream().filter(s -> s.getStart() > w_last_idx.get() && s.getEnd() < snapshot.getEnd()).collect(Collectors.toList()),
                                w_last_idx.get(),
                                snapshot.getStart()
                        ),
                        w_last_priority.get()
                ));
                w_last_idx.set(snapshot.getEnd());
                combine_criteria.accept(priority);
            }
        }
        criterion_list.add(Pair.of(
                _GetAtomicCriterion(
                        expression,
                        clazz,
                        snapshots.stream().filter(s -> s.getStart() >= w_last_idx.get() && s.getEnd() <= end_idx).collect(Collectors.toList()),
                        w_last_idx.get(),
                        end_idx
                ),
                w_last_priority.get()
        ));
        combine_criteria.accept(4);
        //调用combine后数据只剩余一条
        return criterion_list.get(0).getFirst();
    }

    private static Criterion _GetAtomicCriterion(String expression, Class<? extends AbstractAuditCidEntity> clazz, List<Strings.Snapshot> snapshots, int begin_idx, int end_idx) {
        //处理原子表达式，遇到()递归到根方法
        int bracket_cnt = 0;//括号计数
        Wrapper<Integer> w_bracket_begin_idx = new Wrapper<>(0);
        for (Strings.Snapshot snapshot : snapshots) {
            switch (snapshot.getPattern()) {
                case "(": {
                    if (bracket_cnt++ == 0) {
                        w_bracket_begin_idx.set(snapshot.getEnd());
                    }
                    break;
                }
                case ")": {
                    if (--bracket_cnt == 0) {
                        return _RecGetCriterion(
                                expression,
                                clazz,
                                snapshots.stream().filter(s -> s.getStart() > w_bracket_begin_idx.get() && s.getEnd() < snapshot.getEnd()).collect(Collectors.toList()),
                                w_bracket_begin_idx.get(),
                                snapshot.getStart()
                        );
                    }
                    break;
                }
                default: {
                    if (bracket_cnt == 0) {
                        String prop = expression.substring(begin_idx, snapshot.getStart()).trim();
                        String val = expression.substring(snapshot.getEnd(), end_idx).trim();
                        boolean null_flag = "null".equals(val);
                        boolean str_flag = !null_flag && val.startsWith("'") && val.endsWith("'");
                        boolean bool_flag = !null_flag && !str_flag && ("true".equals(val) || "false".equals(val));
                        boolean number_flag = !null_flag && !str_flag && !bool_flag && Strings.isNumber(val);
                        boolean prop_flag = !null_flag && !str_flag && !bool_flag && !number_flag;
                        if (str_flag) {
                            //去除单引号
                            val = val.substring(1, val.length() - 1);
                        }
                        if (prop_flag) {
                            //如果找不到对应字段，则将不认定为字段
                            try {
                                Reflects.getFieldByFullPath(clazz, val);
                            } catch (Exception e) {
                                if (e instanceof NoSuchFieldException) {
                                    prop_flag = false;
                                } else {
                                    throw e;
                                }
                            }
                        }
                        switch (snapshot.getPattern()) {
                            case "!=":
                            case "<>": {
                                if (null_flag) {
                                    return Restrictions.isNotNull(prop);
                                }
                                if (prop_flag) {
                                    return Restrictions.neProperty(prop, val);
                                }
                                return Restrictions.ne(prop, Converter.getConverter(Reflects.getFieldByFullPath(clazz, prop).getType()).apply(val));
                            }
                            case "=": {
                                if (null_flag) {
                                    return Restrictions.isNull(prop);
                                }
                                if (prop_flag) {
                                    return Restrictions.eqProperty(prop, val);
                                }
                                return Restrictions.eq(prop, Converter.getConverter(Reflects.getFieldByFullPath(clazz, prop).getType()).apply(val));
                            }
                            case "<=": {
                                if (prop_flag) {
                                    return Restrictions.leProperty(prop, val);
                                }
                                return Restrictions.le(prop, Converter.getConverter(Reflects.getFieldByFullPath(clazz, prop).getType()).apply(val));
                            }
                            case ">=": {
                                if (prop_flag) {
                                    return Restrictions.geProperty(prop, val);
                                }
                                return Restrictions.ge(prop, Converter.getConverter(Reflects.getFieldByFullPath(clazz, prop).getType()).apply(val));
                            }
                            case "<": {
                                if (prop_flag) {
                                    return Restrictions.ltProperty(prop, val);
                                }
                                return Restrictions.lt(prop, Converter.getConverter(Reflects.getFieldByFullPath(clazz, prop).getType()).apply(val));
                            }
                            case ">": {
                                if (prop_flag) {
                                    return Restrictions.gtProperty(prop, val);
                                }
                                return Restrictions.gt(prop, Converter.getConverter(Reflects.getFieldByFullPath(clazz, prop).getType()).apply(val));
                            }
                        }
                    }
                }
            }
        }
        throw new IllegalArgumentException("argument mismatch");
    }

}