package com.shengnan.platform.module.repair.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.shengnan.platform.common.BusinessException;
import com.shengnan.platform.common.PageResult;
import com.shengnan.platform.module.repair.entity.RepairLog;
import com.shengnan.platform.module.repair.entity.RepairOrder;
import com.shengnan.platform.module.repair.mapper.RepairLogMapper;
import com.shengnan.platform.module.repair.mapper.RepairOrderMapper;
import com.shengnan.platform.module.system.entity.Account;
import com.shengnan.platform.module.system.entity.DictItem;
import com.shengnan.platform.module.system.entity.DictType;
import com.shengnan.platform.module.system.mapper.AccountMapper;
import com.shengnan.platform.module.system.mapper.DictItemMapper;
import com.shengnan.platform.module.system.mapper.DictTypeMapper;
import com.shengnan.platform.security.LoginPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** 报修工单：提交、派单、处理、完工、关闭、评价全流程 */
@Service
public class RepairService {

    /** 0待受理 1已派单 2处理中 3已完成 4已关闭 */
    private static final int PENDING = 0, ASSIGNED = 1, PROCESSING = 2, FINISHED = 3, CLOSED = 4;

    private final RepairOrderMapper orderMapper;
    private final RepairLogMapper logMapper;
    private final AccountMapper accountMapper;
    private final DictTypeMapper dictTypeMapper;
    private final DictItemMapper dictItemMapper;

    public RepairService(RepairOrderMapper orderMapper, RepairLogMapper logMapper, AccountMapper accountMapper,
                         DictTypeMapper dictTypeMapper, DictItemMapper dictItemMapper) {
        this.orderMapper = orderMapper;
        this.logMapper = logMapper;
        this.accountMapper = accountMapper;
        this.dictTypeMapper = dictTypeMapper;
        this.dictItemMapper = dictItemMapper;
    }

    /* ---------------- 查询 ---------------- */

    public PageResult<RepairOrder> page(int pageNum, int pageSize, Integer status, Integer priority,
                                        String category, String keyword, Boolean onlyMine) {
        LoginPrincipal me = currentUser();
        LambdaQueryWrapper<RepairOrder> q = Wrappers.<RepairOrder>lambdaQuery();
        q.eq(status != null, RepairOrder::getStatus, status)
         .eq(priority != null, RepairOrder::getPriority, priority)
         .eq(category != null && !category.isEmpty(), RepairOrder::getCategory, category);
        if (keyword != null && !keyword.trim().isEmpty()) {
            String k = keyword.trim();
            q.and(w -> w.like(RepairOrder::getTitle, k)
                    .or().like(RepairOrder::getOrderNo, k)
                    .or().like(RepairOrder::getLocation, k));
        }
        if (Boolean.TRUE.equals(onlyMine)) {
            q.and(w -> w.eq(RepairOrder::getApplicantId, me.getAccountId())
                    .or().eq(RepairOrder::getHandlerId, me.getAccountId()));
        }
        q.orderByDesc(RepairOrder::getPriority)
         .orderByDesc(RepairOrder::getId);
        IPage<RepairOrder> page = orderMapper.selectPage(new Page<>(pageNum, pageSize), q);
        return PageResult.of(page);
    }

    public RepairOrder get(Long id) {
        RepairOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException("工单不存在");
        o.setLogs(logMapper.selectList(Wrappers.<RepairLog>lambdaQuery()
                .eq(RepairLog::getOrderId, id)
                .orderByAsc(RepairLog::getId)));
        return o;
    }

    /** 各状态数量统计 */
    public Map<String, Object> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", orderMapper.selectCount(null));
        m.put("pending", countByStatus(PENDING));
        m.put("assigned", countByStatus(ASSIGNED));
        m.put("processing", countByStatus(PROCESSING));
        m.put("finished", countByStatus(FINISHED));
        m.put("closed", countByStatus(CLOSED));
        return m;
    }

    private Long countByStatus(int status) {
        return orderMapper.selectCount(Wrappers.<RepairOrder>lambdaQuery().eq(RepairOrder::getStatus, status));
    }

    /** 报修类别字典（避免报修用户依赖 dict:list 权限） */
    public List<DictItem> categories() {
        DictType type = dictTypeMapper.selectOne(Wrappers.<DictType>lambdaQuery()
                .eq(DictType::getDictCode, "sys_repair_category").last("LIMIT 1"));
        if (type == null) return java.util.Collections.emptyList();
        return dictItemMapper.selectList(Wrappers.<DictItem>lambdaQuery()
                .eq(DictItem::getTypeId, type.getId())
                .eq(DictItem::getIsEnabled, true)
                .orderByAsc(DictItem::getSortNo));
    }

    /** 可指派的处理人列表 */
    public List<Account> handlers() {
        return accountMapper.selectList(Wrappers.<Account>lambdaQuery()
                .eq(Account::getIsEnabled, true)
                .select(Account::getId, Account::getUsername, Account::getDisplayName, Account::getDepartmentId));
    }

    /* ---------------- 写操作 ---------------- */

    @Transactional
    public Long create(RepairOrder body) {
        if (body.getTitle() == null || body.getTitle().trim().isEmpty()) throw new BusinessException("报修标题不能为空");
        if (body.getLocation() == null || body.getLocation().trim().isEmpty()) throw new BusinessException("报修地点不能为空");
        LoginPrincipal me = currentUser();
        body.setId(null);
        body.setOrderNo(nextOrderNo());
        body.setStatus(PENDING);
        body.setApplicantId(me.getAccountId());
        body.setApplicantName(me.getUsername());
        body.setDeptId(me.getDepartmentId());
        body.setHandlerId(null);
        body.setHandlerName(null);
        body.setAssignTime(null);
        body.setFinishTime(null);
        if (body.getPriority() == null) body.setPriority(1);
        body.setImages(normalizeImages(body.getImages()));
        orderMapper.insert(body);
        writeLog(body.getId(), "创建", null, PENDING, "提交报修申请");
        return body.getId();
    }

    @Transactional
    public void update(RepairOrder body) {
        if (body.getId() == null) throw new BusinessException("缺少工单ID");
        RepairOrder old = orderMapper.selectById(body.getId());
        if (old == null) throw new BusinessException("工单不存在");
        if (old.getStatus() != null && old.getStatus() >= FINISHED) {
            throw new BusinessException("已完工或已关闭的工单不可编辑");
        }
        body.setOrderNo(null);
        body.setStatus(null);
        body.setApplicantId(null);
        body.setApplicantName(null);
        body.setHandlerId(null);
        body.setHandlerName(null);
        body.setAssignTime(null);
        body.setFinishTime(null);
        String imgs = normalizeImages(body.getImages());
        body.setImages(imgs == null ? "" : imgs);
        orderMapper.updateById(body);
        writeLog(old.getId(), "编辑", old.getStatus(), old.getStatus(), "修改工单信息");
    }

    /** 派单：指定处理人与期望完成时间 */
    @Transactional
    public void assign(Long id, Long handlerId, LocalDateTime expectTime) {
        RepairOrder o = get(id);
        if (o.getStatus() != null && o.getStatus() >= FINISHED) throw new BusinessException("工单已结束，不可派单");
        if (handlerId == null) throw new BusinessException("请选择处理人");
        Account handler = accountMapper.selectById(handlerId);
        if (handler == null) throw new BusinessException("处理人不存在");
        int from = o.getStatus() == null ? PENDING : o.getStatus();
        o.setHandlerId(handlerId);
        o.setHandlerName(handler.getDisplayName() == null ? handler.getUsername() : handler.getDisplayName());
        o.setAssignTime(LocalDateTime.now());
        o.setExpectTime(expectTime);
        o.setStatus(ASSIGNED);
        orderMapper.updateById(o);
        writeLog(id, "派单", from, ASSIGNED, "指派给 " + o.getHandlerName());
    }

    /** 开始处理 */
    @Transactional
    public void start(Long id) {
        RepairOrder o = get(id);
        if (o.getStatus() == null || o.getStatus() != ASSIGNED) throw new BusinessException("仅已派单的工单可开始处理");
        o.setStatus(PROCESSING);
        orderMapper.updateById(o);
        writeLog(id, "开始处理", ASSIGNED, PROCESSING, "处理人开始处理");
    }

    /** 完工：填写处理结果 */
    @Transactional
    public void finish(Long id, String resultDesc) {
        RepairOrder o = get(id);
        if (o.getStatus() == null || (o.getStatus() != ASSIGNED && o.getStatus() != PROCESSING)) {
            throw new BusinessException("仅已派单或处理中的工单可完工");
        }
        if (resultDesc == null || resultDesc.trim().isEmpty()) throw new BusinessException("请填写处理结果");
        int from = o.getStatus();
        o.setResultDesc(resultDesc);
        o.setFinishTime(LocalDateTime.now());
        o.setStatus(FINISHED);
        orderMapper.updateById(o);
        writeLog(id, "完工", from, FINISHED, resultDesc);
    }

    /** 关闭工单 */
    @Transactional
    public void close(Long id, String remark) {
        RepairOrder o = get(id);
        if (o.getStatus() != null && o.getStatus() == CLOSED) throw new BusinessException("工单已关闭");
        int from = o.getStatus() == null ? PENDING : o.getStatus();
        o.setStatus(CLOSED);
        if (remark != null && !remark.trim().isEmpty()) o.setRemark(remark);
        if (o.getFinishTime() == null) o.setFinishTime(LocalDateTime.now());
        orderMapper.updateById(o);
        writeLog(id, "关闭", from, CLOSED, remark == null || remark.isEmpty() ? "关闭工单" : remark);
    }

    /** 满意度评价：仅报修人可评价已完成工单 */
    @Transactional
    public void rate(Long id, Integer rating) {
        RepairOrder o = get(id);
        if (o.getStatus() == null || o.getStatus() != FINISHED) throw new BusinessException("仅已完工的工单可评价");
        if (rating == null || rating < 1 || rating > 5) throw new BusinessException("评分需在 1-5 之间");
        if (!o.getApplicantId().equals(currentUser().getAccountId()) && !currentUser().isSuperAdmin()) {
            throw new BusinessException("仅报修人可评价");
        }
        o.setRating(rating);
        orderMapper.updateById(o);
        writeLog(id, "评价", FINISHED, FINISHED, "满意度 " + rating + " 星");
    }

    @Transactional
    public void remove(Long id) {
        RepairOrder o = orderMapper.selectById(id);
        if (o == null) throw new BusinessException("工单不存在");
        orderMapper.deleteById(id);
        logMapper.delete(Wrappers.<RepairLog>lambdaQuery().eq(RepairLog::getOrderId, id));
    }

    /* ---------------- 内部 ---------------- */

    private void writeLog(Long orderId, String action, Integer from, Integer to, String content) {
        RepairLog log = new RepairLog();
        log.setOrderId(orderId);
        log.setAction(action);
        log.setFromStatus(from);
        log.setToStatus(to);
        log.setOperator(currentUser().getUsername());
        log.setContent(content);
        log.setCreatedAt(LocalDateTime.now());
        logMapper.insert(log);
    }

    private String nextOrderNo() {
        String prefix = "BX" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        return prefix + String.format("%02d", ThreadLocalRandom.current().nextInt(100));
    }

    /** 只保留数字文件ID，去重后逗号拼接，最多 9 张 */
    private String normalizeImages(String images) {
        if (images == null || images.trim().isEmpty()) return null;
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        for (String s : images.split(",")) {
            String t = s.trim();
            if (t.matches("\\d{1,19}")) ids.add(t);
            if (ids.size() >= 9) break;
        }
        return ids.isEmpty() ? null : String.join(",", ids);
    }

    private LoginPrincipal currentUser() {
        Object p = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (p instanceof LoginPrincipal) return (LoginPrincipal) p;
        throw new BusinessException("未登录");
    }
}