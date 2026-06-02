package com.ruoyi.web.controller.app;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.TbWorkerCheckin;
import com.ruoyi.system.mapper.TbWorkerCheckinMapper;
import com.ruoyi.system.service.ITbWorkerService;

@RestController
@RequestMapping("/app/checkin")
public class AppCheckinController
{
    @Autowired private TbWorkerCheckinMapper checkinMapper;
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
        data.put("hasSignIn", signIn != null); data.put("hasSignOut", signOut != null);
        data.put("signInTime", signIn != null ? signIn.getCheckTime() : null);
        data.put("signOutTime", signOut != null ? signOut.getCheckTime() : null);
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
        }

        TbWorkerCheckin c = new TbWorkerCheckin();
        c.setWorkerId(wid);
        c.setCheckType(checkType);
        c.setCheckTime(new Date());
        c.setCheckMethod(body.get("checkMethod") != null ? body.get("checkMethod").toString() : "H5");
        if (body.get("photoUrl") != null) c.setPhotoUrl(body.get("photoUrl").toString());
        if (body.get("latitude") != null) c.setLatitude(new java.math.BigDecimal(body.get("latitude").toString()));
        if (body.get("longitude") != null) c.setLongitude(new java.math.BigDecimal(body.get("longitude").toString()));
        c.setCreateBy("worker:" + wid);
        checkinMapper.insertTbWorkerCheckin(c);
        return AjaxResult.success(Collections.singletonMap("id", c.getId()));
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
}
