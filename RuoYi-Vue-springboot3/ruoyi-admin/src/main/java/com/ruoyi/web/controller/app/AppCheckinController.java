package com.ruoyi.web.controller.app;

import java.util.*;
import java.math.BigDecimal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerCheckin;
import com.ruoyi.system.domain.TbWorkerRole;
import com.ruoyi.system.mapper.TbWorkerCheckinMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbWorkerRoleMapper;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.service.ITbWorkerService;

@RestController
@RequestMapping("/app/checkin")
public class AppCheckinController
{
    @Autowired private TbWorkerCheckinMapper checkinMapper;
    @Autowired private TbWorkerMapper workerMapper;
    @Autowired private TbWorkerRoleMapper workerRoleMapper;
    @Autowired private TbWorkerRoleRelMapper workerRoleRelMapper;
    @Autowired private ITbWorkerService workerService;

    /** 今日打卡状态 */
    @GetMapping("/today")
    public AjaxResult today(HttpServletRequest req) {
        Long id = AppTokenUtil.getWorkerId(req);
        if (id == null) return AjaxResult.error(401, "未登录");
        if (!workerService.isWorkerActive(id)) return AjaxResult.error("该人员已归档或已禁用");
        TbWorkerCheckin q = new TbWorkerCheckin(); q.setWorkerId(id);
        List<TbWorkerCheckin> list = checkinMapper.selectTbWorkerCheckinList(q);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
        TbWorkerCheckin signIn = null, signOut = null;
        for (TbWorkerCheckin c : list) {
            String d = new java.text.SimpleDateFormat("yyyy-MM-dd").format(c.getCheckTime());
            if (!today.equals(d)) continue;
            if ("1".equals(c.getCheckType())) signIn = c;
            if ("2".equals(c.getCheckType())) signOut = c;
        }
        Map<String, Object> data = new HashMap<>();
        RuleResult signInRule = checkRoleRule(id, "1");
        RuleResult signOutRule = checkRoleRule(id, "2");
        boolean hasSignIn = signIn != null;
        boolean hasSignOut = signOut != null;
        boolean canSignIn = signInRule.allowed && !hasSignIn;
        boolean canSignOut = signOutRule.allowed && hasSignIn && !hasSignOut;
        String signInReason = signInRule.reason;
        String signOutReason = signOutRule.reason;
        if (hasSignIn) signInReason = "今日已签到，请勿重复提交";
        if (!hasSignIn && signOutRule.allowed) signOutReason = "今日尚未签到，不能签退";
        if (hasSignOut) signOutReason = "今日已签退，请勿重复提交";

        data.put("hasSignIn", hasSignIn); data.put("hasSignOut", hasSignOut);
        data.put("signInTime", signIn != null ? signIn.getCheckTime() : null);
        data.put("signOutTime", signOut != null ? signOut.getCheckTime() : null);
        data.put("canSignIn", canSignIn);
        data.put("canSignOut", canSignOut);
        data.put("signInReason", canSignIn ? "" : signInReason);
        data.put("signOutReason", canSignOut ? "" : signOutReason);
        data.put("roleName", signInRule.roleName != null ? signInRule.roleName : signOutRule.roleName);
        return AjaxResult.success(data);
    }

    @PostMapping("/signIn")
    public AjaxResult signIn(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        return doCheck(req, body, "1");
    }

    @PostMapping("/signOut")
    public AjaxResult signOut(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        return doCheck(req, body, "2");
    }

    private AjaxResult doCheck(HttpServletRequest req, Map<String, Object> body, String checkType) {
        Long wid = AppTokenUtil.getWorkerId(req);
        if (wid == null) return AjaxResult.error(401, "未登录");
        if (!workerService.isWorkerActive(wid)) return AjaxResult.error("该人员已归档或已禁用，无法打卡");

        RuleResult roleRule = checkRoleRule(wid, checkType);
        if (!roleRule.allowed) return AjaxResult.error(roleRule.reason);

        String photoUrl = body.get("photoUrl") != null ? body.get("photoUrl").toString() : "";
        BigDecimal latitude = parseDecimal(body.get("latitude"));
        BigDecimal longitude = parseDecimal(body.get("longitude"));
        if (photoUrl.trim().isEmpty()) return AjaxResult.error("请先拍照上传现场照片");
        boolean missingGps = latitude == null || longitude == null;

        // 查今天已有记录
        TbWorkerCheckin q = new TbWorkerCheckin(); q.setWorkerId(wid);
        List<TbWorkerCheckin> list = checkinMapper.selectTbWorkerCheckinList(q);
        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
        TbWorkerCheckin todaySignIn = null, todaySignOut = null;
        for (TbWorkerCheckin ck : list) {
            if (!today.equals(new java.text.SimpleDateFormat("yyyy-MM-dd").format(ck.getCheckTime()))) continue;
            if ("1".equals(ck.getCheckType())) todaySignIn = ck;
            if ("2".equals(ck.getCheckType())) todaySignOut = ck;
        }

        // 规则校验
        if ("1".equals(checkType) && todaySignIn != null)
            return AjaxResult.error("今日已签到，请勿重复提交");
        if ("2".equals(checkType)) {
            if (todaySignIn == null) return AjaxResult.error("今日尚未签到，不能签退");
            if (todaySignOut != null) return AjaxResult.error("今日已签退，请勿重复提交");
            if (System.currentTimeMillis() <= todaySignIn.getCheckTime().getTime())
                return AjaxResult.error("签退时间必须晚于签到时间");
        }

        TbWorkerCheckin c = new TbWorkerCheckin();
        c.setWorkerId(wid);
        c.setCheckType(checkType);
        c.setCheckTime(new Date());
        c.setCheckMethod(body.get("checkMethod") != null ? body.get("checkMethod").toString() : "H5");
        c.setPhotoUrl(photoUrl);
        c.setLatitude(latitude);
        c.setLongitude(longitude);
        if (missingGps) c.setRemark("缺GPS定位");
        c.setCreateBy("worker:" + wid);
        checkinMapper.insertTbWorkerCheckin(c);
        return AjaxResult.success(Collections.singletonMap("id", c.getId()));
    }

    private BigDecimal parseDecimal(Object value) {
        if (value == null) return null;
        try {
            String text = value.toString();
            if (text.trim().isEmpty()) return null;
            return new BigDecimal(text);
        } catch (Exception e) {
            return null;
        }
    }

    private RuleResult checkRoleRule(Long workerId, String checkType) {
        TbWorkerRole role = selectCurrentRole(workerId);
        if (role == null) {
            return RuleResult.blocked("未分配可用人员角色，不能自主打卡", null);
        }
        String roleName = role.getRoleName();
        if (!"1".equals(role.getIsSelfCheckin())) {
            return RuleResult.blocked("当前角色【" + roleName + "】不允许自主打卡，请通过班前喊话统一签到/签退", roleName);
        }
        if ("1".equals(checkType) && !"1".equals(role.getNeedSignIn())) {
            return RuleResult.blocked("当前角色【" + roleName + "】不需要自主签到", roleName);
        }
        if ("2".equals(checkType) && !"1".equals(role.getNeedSignOut())) {
            return RuleResult.blocked("当前角色【" + roleName + "】不需要自主签退", roleName);
        }
        return RuleResult.allowed(roleName);
    }

    private TbWorkerRole selectCurrentRole(Long workerId) {
        List<Long> roleIds = workerRoleRelMapper.selectRoleIdsByWorkerId(workerId);
        if (roleIds == null || roleIds.isEmpty()) return null;
        for (Long roleId : roleIds) {
            TbWorkerRole role = workerRoleMapper.selectTbWorkerRoleById(roleId);
            if (role != null && (role.getStatus() == null || "0".equals(role.getStatus()))) {
                return role;
            }
        }
        return null;
    }

    private static class RuleResult {
        private final boolean allowed;
        private final String reason;
        private final String roleName;

        private RuleResult(boolean allowed, String reason, String roleName) {
            this.allowed = allowed;
            this.reason = reason;
            this.roleName = roleName;
        }

        private static RuleResult allowed(String roleName) {
            return new RuleResult(true, "", roleName);
        }

        private static RuleResult blocked(String reason, String roleName) {
            return new RuleResult(false, reason, roleName);
        }
    }

    @GetMapping("/list")
    public AjaxResult list(HttpServletRequest req) {
        Long id = AppTokenUtil.getWorkerId(req);
        if (id == null) return AjaxResult.error(401, "未登录");
        TbWorkerCheckin q = new TbWorkerCheckin(); q.setWorkerId(id);
        List<TbWorkerCheckin> list = checkinMapper.selectTbWorkerCheckinList(q);
        list.sort((a, b) -> b.getCheckTime().compareTo(a.getCheckTime()));
        return AjaxResult.success(list);
    }

    /** 同单位人员今日打卡状态（用于班前喊话/签退查看） */
    @GetMapping("/project-status")
    public AjaxResult projectStatus(HttpServletRequest req) {
        Long id = AppTokenUtil.getWorkerId(req);
        if (id == null) return AjaxResult.error(401, "未登录");
        TbWorker me = workerMapper.selectTbWorkerById(id);
        if (me == null) return AjaxResult.error("人员不存在");

        // 查同单位类型的所有人员
        TbWorker q = new TbWorker(); q.setUnitType(me.getUnitType());
        List<TbWorker> coworkers = workerMapper.selectTbWorkerList(q);

        String today = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());
        List<Map<String, Object>> result = new ArrayList<>();
        for (TbWorker cw : coworkers) {
            Map<String, Object> item = new HashMap<>();
            item.put("workerId", cw.getId());
            item.put("workerName", cw.getWorkerName());
            // 查今日打卡
            TbWorkerCheckin cq = new TbWorkerCheckin(); cq.setWorkerId(cw.getId());
            List<TbWorkerCheckin> checks = checkinMapper.selectTbWorkerCheckinList(cq);
            boolean signedIn = false, signedOut = false;
            for (TbWorkerCheckin ck : checks) {
                String d = new java.text.SimpleDateFormat("yyyy-MM-dd").format(ck.getCheckTime());
                if (!today.equals(d)) continue;
                if ("1".equals(ck.getCheckType())) signedIn = true;
                if ("2".equals(ck.getCheckType())) signedOut = true;
            }
            item.put("signedIn", signedIn);
            item.put("signedOut", signedOut);
            result.add(item);
        }
        return AjaxResult.success(result);
    }
}
