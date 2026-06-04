package com.ruoyi.web.controller.app;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerRole;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbWorkerRoleMapper;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.service.ITbWorkerService;

@RestController
@RequestMapping("/app/auth")
public class AppAuthController
{
    @Autowired private TbWorkerMapper tbWorkerMapper;
    @Autowired private TbWorkerRoleRelMapper tbWorkerRoleRelMapper;
    @Autowired private TbWorkerRoleMapper tbWorkerRoleMapper;
    @Autowired private ITbWorkerService workerService;

    /** 手机号+密码登录 → 返回 HMAC Token */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String idCardLast6 = body.get("idCardLast6");
        if (phone == null || idCardLast6 == null) return AjaxResult.error("手机号和密码不能为空");
        TbWorker q = new TbWorker(); q.setPhone(phone);
        List<TbWorker> list = tbWorkerMapper.selectTbWorkerList(q);
        TbWorker w = list.stream()
            .filter(t -> t.getIdCard() != null && t.getIdCard().endsWith(idCardLast6)
                     && "0".equals(t.getDelFlag()) && !"2".equals(t.getStatus()))
            .findFirst().orElse(null);
        if (w == null) return AjaxResult.error("手机号或密码不正确");
        // 检查是否有自主打卡权限
        List<Long> roleIds = tbWorkerRoleRelMapper.selectRoleIdsByWorkerId(w.getId());
        boolean canSelfCheckin = false;
        if (roleIds != null && !roleIds.isEmpty()) {
            for (Long rid : roleIds) {
                TbWorkerRole r = tbWorkerRoleMapper.selectTbWorkerRoleById(rid);
                if (r != null && !"0".equals(r.getIsSelfCheckin())) { canSelfCheckin = true; break; }
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", AppTokenUtil.create(w.getId()));
        data.put("workerId", w.getId());
        data.put("workerName", w.getWorkerName());
        data.put("auditStatus", w.getAuditStatus());
        data.put("faceStatus", w.getFaceStatus());
        data.put("status", w.getStatus());
        data.put("canCheckin", canSelfCheckin);
        return AjaxResult.success(data);
    }

    /** 获取当前人员信息（Token 鉴权） */
    @GetMapping("/me")
    public AjaxResult me(HttpServletRequest req) {
        Long id = AppTokenUtil.getWorkerId(req);
        if (id == null) return AjaxResult.error(401, "未登录或登录已过期");
        if (!workerService.isWorkerActive(id)) return AjaxResult.error("该人员已归档或已禁用，无法操作");
        TbWorker w = tbWorkerMapper.selectTbWorkerById(id);
        Map<String, Object> data = new HashMap<>();
        data.put("workerId", w.getId()); data.put("workerName", w.getWorkerName());
        data.put("phone", w.getPhone()); data.put("idCard", w.getIdCard());
        data.put("gender", w.getGender()); data.put("unitType", w.getUnitType());
        data.put("auditStatus", w.getAuditStatus()); data.put("faceStatus", w.getFaceStatus());
        data.put("status", w.getStatus());
        data.put("roleIds", tbWorkerRoleRelMapper.selectRoleIdsByWorkerId(id));
        return AjaxResult.success(data);
    }
}
