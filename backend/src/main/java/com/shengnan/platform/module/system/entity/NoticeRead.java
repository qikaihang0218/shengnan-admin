package com.shengnan.platform.module.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 通知已读记录 */
@Data
@TableName("sp_notice_read")
public class NoticeRead {
    private Long id;
    private Long noticeId;
    private Long accountId;
    private LocalDateTime readTime;
}
