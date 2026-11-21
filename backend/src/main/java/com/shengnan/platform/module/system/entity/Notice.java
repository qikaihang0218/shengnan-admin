package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.shengnan.platform.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/** 全员通知 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sp_notice")
public class Notice extends BaseEntity {
    private String title;
    private String content;
    private Integer noticeType;   // 1通知 2公告
    private Boolean isTop;
    private Integer status;       // 0草稿 1已发布
    private LocalDateTime publishTime;

    /** 非表字段：当前账号是否已读 */
    @TableField(exist = false)
    private Boolean read;
    /** 非表字段：类型名称 */
    @TableField(exist = false)
    private String typeName;
}
