package com.shengnan.platform.module.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.system.entity.Notice;
import com.shengnan.platform.module.system.entity.NoticeRead;
import com.shengnan.platform.module.system.mapper.NoticeMapper;
import com.shengnan.platform.module.system.mapper.NoticeReadMapper;
import com.shengnan.platform.security.LoginPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 全员通知：管理端 CRUD/发布，用户端查看已发布通知与已读状态 */
@Service
public class NoticeService {

    private final NoticeMapper noticeMapper;
    private final NoticeReadMapper readMapper;

    public NoticeService(NoticeMapper noticeMapper, NoticeReadMapper readMapper) {
        this.noticeMapper = noticeMapper;
        this.readMapper = readMapper;
    }

    /* ---------------- 管理端 ---------------- */

    public PageResult<Notice> page(int pageNum, int pageSize, Integer status, String title) {
        IPage<Notice> page = noticeMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<Notice>lambdaQuery()
                        .eq(status != null, Notice::getStatus, status)
                        .like(title != null && !title.trim().isEmpty(), Notice::getTitle, title)
                        .orderByDesc(Notice::getIsTop)
                        .orderByDesc(Notice::getPublishTime)
                        .orderByDesc(Notice::getId));
        return PageResult.of(page);
    }

    public Notice get(Long id) {
        Notice n = noticeMapper.selectById(id);
        if (n == null) throw new BusinessException("通知不存在");
        return n;
    }

    public void create(Notice body) {
        body.setId(null);
        normalize(body);
        if (body.getStatus() == null) body.setStatus(0);
        if (body.getStatus() == 1 && body.getPublishTime() == null) body.setPublishTime(LocalDateTime.now());
        noticeMapper.insert(body);
    }

    public void update(Notice body) {
        if (body.getId() == null) throw new BusinessException("缺少通知ID");
        Notice old = noticeMapper.selectById(body.getId());
        if (old == null) throw new BusinessException("通知不存在");
        normalize(body);
        // 从草稿改为已发布时补给发布时间；已是发布状态则保留原发布时间
        if (Integer.valueOf(1).equals(body.getStatus())) {
            if (old.getPublishTime() == null) body.setPublishTime(LocalDateTime.now());
        } else {
            body.setPublishTime(null);
        }
        noticeMapper.updateById(body);
    }

    /** 发布/撤回：status 1发布 0撤回 */
    public void changeStatus(Long id, int status) {
        Notice n = get(id);
        n.setStatus(status);
        n.setPublishTime(status == 1 ? LocalDateTime.now() : null);
        noticeMapper.updateById(n);
    }

    public void remove(Long id) {
        if (noticeMapper.deleteById(id) == 0) throw new BusinessException("通知不存在");
        readMapper.delete(Wrappers.<NoticeRead>lambdaQuery().eq(NoticeRead::getNoticeId, id));
    }

    /* ---------------- 用户端 ---------------- */

    /** 当前账号可见的已发布通知，带已读标记；置顶优先 */
    public List<Notice> published() {
        Long accountId = currentAccountId();
        List<Notice> list = noticeMapper.selectList(Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getStatus, 1)
                .orderByDesc(Notice::getIsTop)
                .orderByDesc(Notice::getPublishTime)
                .orderByDesc(Notice::getId));
        Set<Long> readIds = readIds(accountId);
        list.forEach(n -> {
            n.setRead(readIds.contains(n.getId()));
            n.setTypeName(n.getNoticeType() != null && n.getNoticeType() == 2 ? "公告" : "通知");
        });
        return list;
    }

    /** 未读数量 */
    public int unreadCount() {
        Long accountId = currentAccountId();
        List<Notice> published = noticeMapper.selectList(Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getStatus, 1)
                .select(Notice::getId));
        if (published.isEmpty()) return 0;
        Set<Long> readIds = readIds(accountId);
        int count = 0;
        for (Notice n : published) if (!readIds.contains(n.getId())) count++;
        return count;
    }

    /** 标记单条已读（幂等） */
    @Transactional
    public void markRead(Long noticeId) {
        Long accountId = currentAccountId();
        Notice n = noticeMapper.selectById(noticeId);
        if (n == null || !Integer.valueOf(1).equals(n.getStatus())) throw new BusinessException("通知不存在或未发布");
        markReadInternal(noticeId, accountId);
    }

    /** 全部标记已读 */
    @Transactional
    public void markAllRead() {
        Long accountId = currentAccountId();
        List<Notice> published = noticeMapper.selectList(Wrappers.<Notice>lambdaQuery()
                .eq(Notice::getStatus, 1)
                .select(Notice::getId));
        Set<Long> readIds = readIds(accountId);
        for (Notice n : published) {
            if (!readIds.contains(n.getId())) markReadInternal(n.getId(), accountId);
        }
    }

    private void markReadInternal(Long noticeId, Long accountId) {
        Long exists = readMapper.selectCount(Wrappers.<NoticeRead>lambdaQuery()
                .eq(NoticeRead::getNoticeId, noticeId)
                .eq(NoticeRead::getAccountId, accountId));
        if (exists != null && exists > 0) return;
        NoticeRead r = new NoticeRead();
        r.setNoticeId(noticeId);
        r.setAccountId(accountId);
        r.setReadTime(LocalDateTime.now());
        readMapper.insert(r);
    }

    private Set<Long> readIds(Long accountId) {
        List<NoticeRead> records = readMapper.selectList(Wrappers.<NoticeRead>lambdaQuery()
                .eq(NoticeRead::getAccountId, accountId));
        if (records.isEmpty()) return Collections.emptySet();
        return records.stream().map(NoticeRead::getNoticeId).collect(Collectors.toCollection(HashSet::new));
    }

    private Long currentAccountId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginPrincipal) {
            return ((LoginPrincipal) auth.getPrincipal()).getAccountId();
        }
        throw new BusinessException("未登录");
    }

    private void normalize(Notice body) {
        if (body.getTitle() == null || body.getTitle().trim().isEmpty()) throw new BusinessException("标题不能为空");
        if (body.getNoticeType() == null) body.setNoticeType(1);
        if (body.getIsTop() == null) body.setIsTop(false);
    }
}
