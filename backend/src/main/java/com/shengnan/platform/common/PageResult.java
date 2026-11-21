package com.shengnan.platform.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 统一分页结果 */
@Data
public class PageResult<T> implements Serializable {
    private List<T> records;
    private long total;
    private long pageNum;
    private long pageSize;

    public static <E, T> PageResult<T> of(IPage<E> page, Function<E, T> converter) {
        PageResult<T> r = new PageResult<>();
        r.records = page.getRecords().stream().map(converter).collect(Collectors.toList());
        r.total = page.getTotal();
        r.pageNum = page.getCurrent();
        r.pageSize = page.getSize();
        return r;
    }

    public static <E> PageResult<E> of(IPage<E> page) {
        return of(page, e -> e);
    }
}
