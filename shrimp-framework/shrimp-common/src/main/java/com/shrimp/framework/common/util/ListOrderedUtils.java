package com.shrimp.framework.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author ccb
 */
public class ListOrderedUtils {
    public static <T, V> List<V> sortListByOrderedIds(List<T> orderedIds, List<V> sourceList, Function<V, T> idExtractor) {
        if (CollUtil.isEmpty(orderedIds) || CollUtil.isEmpty(sourceList)) {
            return sourceList;
        }
        Map<T, V> map = sourceList.stream()
                .collect(Collectors.toMap(idExtractor, p -> p));
        return orderedIds.stream()
                .map(map::get)
                .filter(ObjUtil::isNotNull)
                .toList();
    }
}
