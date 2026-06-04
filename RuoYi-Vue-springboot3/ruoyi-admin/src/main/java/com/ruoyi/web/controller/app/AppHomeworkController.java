package com.ruoyi.web.controller.app;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.HwPlan;
import com.ruoyi.system.domain.HwPlanWorker;
import com.ruoyi.system.domain.HwReview;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.mapper.HwPlanMapper;
import com.ruoyi.system.mapper.HwPlanWorkerMapper;
import com.ruoyi.system.mapper.HwReviewMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.service.IHwPlanService;
import com.ruoyi.system.service.IHwReviewService;

/**
 * 移动端作业管理接口（/app/homework/*）
 * 使用 AppToken 鉴权，为施工方管理人员和作业批准人提供计划管理与审核功能
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/app/homework")
public class AppHomeworkController extends BaseController
{
    @Autowired private IHwPlanService hwPlanService;
    @Autowired private IHwReviewService hwReviewService;
    @Autowired private HwPlanMapper hwPlanMapper;
    @Autowired private HwReviewMapper hwReviewMapper;
    @Autowired private HwPlanWorkerMapper hwPlanWorkerMapper;
    @Autowired private TbWorkerMapper tbWorkerMapper;
    @Autowired private TbWorkerRoleRelMapper tbWorkerRoleRelMapper;

    // ==================== 作业计划 ====================

    /** 作业计划列表（与网页端一致，显示全部计划） */
    @GetMapping("/plan/list")
    public TableDataInfo planList(HttpServletRequest req,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String status) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) {
            TableDataInfo rsp = new TableDataInfo();
            rsp.setCode(401); rsp.setMsg("未登录"); rsp.setRows(Collections.emptyList()); rsp.setTotal(0);
            return rsp;
        }
        startPage();
        HwPlan query = new HwPlan();
        if (StringUtils.isNotEmpty(status)) query.setStatus(status);
        List<HwPlan> list = hwPlanMapper.selectHwPlanList(query);
        return getDataTable(list);
    }

    /** 计划详情 */
    @GetMapping("/plan/{planId}")
    public AjaxResult planDetail(HttpServletRequest req, @PathVariable Long planId) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) return AjaxResult.error(401, "未登录");
        HwPlan plan = hwPlanMapper.selectHwPlanById(planId);
        if (plan == null) return AjaxResult.error("计划不存在");
        List<HwPlanWorker> workers = hwPlanWorkerMapper.selectByPlanId(planId);
        // 同时加载审核记录
        HwReview review = hwReviewMapper.selectHwReviewByPlanId(planId);
        Map<String, Object> result = new HashMap<>();
        result.put("plan", plan);
        result.put("workers", workers);
        result.put("review", review);
        return AjaxResult.success(result);
    }

    /** 创建作业计划 */
    @PostMapping("/plan")
    public AjaxResult createPlan(HttpServletRequest req, @RequestBody HwPlan plan) {
        try {
            Long workerId = AppTokenUtil.getWorkerId(req);
            if (workerId == null) return AjaxResult.error(401, "未登录");
            TbWorker worker = tbWorkerMapper.selectTbWorkerById(workerId);
            if (worker == null) return AjaxResult.error("未找到您的人员信息");
            // 校验创建者权限：施工方（unit_type='3'）且非普通施工人员
            if (!"3".equals(worker.getUnitType())) return AjaxResult.error("仅施工方人员可创建作业计划");
            List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
            if (roleCodes.isEmpty() || (roleCodes.size() == 1 && "worker".equals(roleCodes.get(0))))
                return AjaxResult.error("普通施工人员无权创建作业计划，请联系施工方管理人员");

            plan.setCreateBy(worker.getWorkerName());
            hwPlanService.insertHwPlan(plan);
            return AjaxResult.success(plan.getPlanId());
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 查询计划关联的参与人员 */
    @GetMapping("/plan/{planId}/workers")
    public AjaxResult planWorkers(HttpServletRequest req, @PathVariable Long planId) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) return AjaxResult.error(401, "未登录");
        List<HwPlanWorker> workers = hwPlanWorkerMapper.selectByPlanId(planId);
        return AjaxResult.success(workers);
    }

    /** 获取角色及其下的人员（供参与人员二级选择器使用） */
    @GetMapping("/rolesWithWorkers")
    public AjaxResult rolesWithWorkers(HttpServletRequest req) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) return AjaxResult.error(401, "未登录");
        return AjaxResult.success(tbWorkerRoleRelMapper.selectWorkersByRole());
    }

    /** 修改作业计划 */
    @PutMapping("/plan")
    public AjaxResult updatePlan(HttpServletRequest req, @RequestBody HwPlan plan) {
        try {
            Long workerId = AppTokenUtil.getWorkerId(req);
            if (workerId == null) return AjaxResult.error(401, "未登录");
            HwPlan exist = hwPlanMapper.selectHwPlanById(plan.getPlanId());
            if (exist == null) return AjaxResult.error("计划不存在");
            // 仅创建者或管理员可修改
            if (!isPlanOwner(workerId, exist)) return AjaxResult.error("仅计划创建者可修改");
            // 已完成或已取消不可修改
            if ("3".equals(exist.getStatus()) || "4".equals(exist.getStatus()))
                return AjaxResult.error("已完成或已取消的计划不可修改");
            plan.setUpdateBy(AppTokenUtil.getWorkerId(req).toString());
            hwPlanService.updateHwPlan(plan);
            return AjaxResult.success();
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 删除作业计划 */
    @DeleteMapping("/plan/{planId}")
    public AjaxResult deletePlan(HttpServletRequest req, @PathVariable Long planId) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) return AjaxResult.error(401, "未登录");
        HwPlan exist = hwPlanMapper.selectHwPlanById(planId);
        if (exist == null) return AjaxResult.error("计划不存在");
        if (!isPlanOwner(workerId, exist)) return AjaxResult.error("仅计划创建者可删除");
        if ("2".equals(exist.getStatus())) return AjaxResult.error("进行中的计划不可删除，请先取消");
        hwPlanMapper.deleteHwPlanById(planId);
        return AjaxResult.success();
    }

    /** 变更计划状态 */
    @PutMapping("/plan/changeStatus")
    public AjaxResult changePlanStatus(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        try {
            Long workerId = AppTokenUtil.getWorkerId(req);
            if (workerId == null) return AjaxResult.error(401, "未登录");
            Long planId = Long.valueOf(body.get("planId").toString());
            String status = body.get("status").toString();
            HwPlan exist = hwPlanMapper.selectHwPlanById(planId);
            if (exist == null) return AjaxResult.error("计划不存在");
            if (!isPlanOwner(workerId, exist)) return AjaxResult.error("仅计划创建者可操作");
            hwPlanService.changeStatus(planId, status);
            return AjaxResult.success();
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 判断当前工人是否为计划创建者 */
    private boolean isPlanOwner(Long workerId, HwPlan plan) {
        TbWorker worker = tbWorkerMapper.selectTbWorkerById(workerId);
        if (worker == null) return false;
        return worker.getWorkerName().equals(plan.getCreateBy());
    }

    // ==================== 作业审核 ====================

    /** 审核列表（仅作业批准人可见） */
    @GetMapping("/review/list")
    public TableDataInfo reviewList(HttpServletRequest req,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String reviewStatus) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) {
            TableDataInfo rsp = new TableDataInfo();
            rsp.setCode(401); rsp.setMsg("未登录"); rsp.setRows(Collections.emptyList()); rsp.setTotal(0);
            return rsp;
        }
        List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
        if (!roleCodes.contains("approver")) {
            TableDataInfo rsp = new TableDataInfo();
            rsp.setCode(200); rsp.setMsg("success"); rsp.setRows(Collections.emptyList()); rsp.setTotal(0);
            return rsp;
        }
        startPage();
        HwReview query = new HwReview();
        if (StringUtils.isNotEmpty(reviewStatus)) query.setReviewStatus(reviewStatus);
        List<HwReview> list = hwReviewMapper.selectHwReviewList(query);
        return getDataTable(list);
    }

    /** 审核详情（含计划信息 + 参与人员） */
    @GetMapping("/review/{reviewId}")
    public AjaxResult reviewDetail(HttpServletRequest req, @PathVariable Long reviewId) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) return AjaxResult.error(401, "未登录");
        HwReview review = hwReviewMapper.selectHwReviewById(reviewId);
        if (review == null) return AjaxResult.error("审核记录不存在");
        HwPlan plan = hwPlanMapper.selectHwPlanById(review.getPlanId());
        List<HwPlanWorker> workers = hwPlanWorkerMapper.selectByPlanId(review.getPlanId());
        Map<String, Object> result = new HashMap<>();
        result.put("review", review);
        result.put("plan", plan);
        result.put("workers", workers);
        return AjaxResult.success(result);
    }

    /** 审核通过 */
    @PutMapping("/review/approve")
    public AjaxResult approveReview(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        try {
            Long workerId = AppTokenUtil.getWorkerId(req);
            if (workerId == null) return AjaxResult.error(401, "未登录");
            List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
            if (!roleCodes.contains("approver")) return AjaxResult.error("无审核权限，仅作业批准人可审核");

            Long reviewId = Long.valueOf(body.get("reviewId").toString());
            String opinion = body.get("reviewOpinion") != null ? body.get("reviewOpinion").toString() : "";
            hwReviewService.approve(reviewId, opinion);
            return AjaxResult.success();
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /** 审核驳回 */
    @PutMapping("/review/reject")
    public AjaxResult rejectReview(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        try {
            Long workerId = AppTokenUtil.getWorkerId(req);
            if (workerId == null) return AjaxResult.error(401, "未登录");
            List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(workerId);
            if (!roleCodes.contains("approver")) return AjaxResult.error("无审核权限，仅作业批准人可审核");

            Long reviewId = Long.valueOf(body.get("reviewId").toString());
            String opinion = body.get("reviewOpinion") != null ? body.get("reviewOpinion").toString() : "";
            hwReviewService.reject(reviewId, opinion);
            return AjaxResult.success();
        } catch (ServiceException e) {
            return AjaxResult.error(e.getMessage());
        }
    }
}
