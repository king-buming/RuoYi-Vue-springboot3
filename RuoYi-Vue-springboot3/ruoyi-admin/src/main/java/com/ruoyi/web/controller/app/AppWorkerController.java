package com.ruoyi.web.controller.app;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.domain.TbWorkerFace;
import com.ruoyi.system.mapper.TbWorkerCertMapper;
import com.ruoyi.system.mapper.TbWorkerFaceMapper;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.service.ITbWorkerService;

@RestController
@RequestMapping("/app/worker")
public class AppWorkerController
{
    @Autowired private TbWorkerMapper workerMapper;
    @Autowired private TbWorkerCertMapper certMapper;
    @Autowired private TbWorkerRoleRelMapper roleRelMapper;
    @Autowired private TbWorkerFaceMapper faceMapper;
    @Autowired private ITbWorkerService workerService;

    private AjaxResult checkActive(HttpServletRequest req) {
        Long id = AppTokenUtil.getWorkerId(req);
        if (id == null) return AjaxResult.error(401, "未登录");
        if (!workerService.isWorkerActive(id)) return AjaxResult.error("该人员已归档或已禁用，无法操作");
        return null;
    }

    @GetMapping("/profile")
    public AjaxResult profile(HttpServletRequest req) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long id = AppTokenUtil.getWorkerId(req);
        TbWorker w = workerMapper.selectTbWorkerById(id);
        Map<String, Object> d = new HashMap<>();
        d.put("workerId", w.getId()); d.put("workerName", w.getWorkerName());
        d.put("phone", w.getPhone()); d.put("idCard", w.getIdCard());
        d.put("gender", w.getGender()); d.put("unitType", w.getUnitType());
        d.put("auditStatus", w.getAuditStatus()); d.put("faceStatus", w.getFaceStatus());
        d.put("status", w.getStatus());
        d.put("roleIds", roleRelMapper.selectRoleIdsByWorkerId(id));
        return AjaxResult.success(d);
    }

    @GetMapping("/certs")
    public AjaxResult certs(HttpServletRequest req) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long id = AppTokenUtil.getWorkerId(req);
        TbWorkerCert q = new TbWorkerCert(); q.setWorkerId(id);
        return AjaxResult.success(certMapper.selectTbWorkerCertList(q));
    }

    @PostMapping("/certs")
    public AjaxResult addCert(HttpServletRequest req, @RequestBody TbWorkerCert cert) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long id = AppTokenUtil.getWorkerId(req);
        cert.setWorkerId(id); // 强制用 token 中的身份
        cert.setAuditStatus("0");
        certMapper.insertTbWorkerCert(cert);
        return AjaxResult.success(Collections.singletonMap("id", cert.getId()));
    }

    @PostMapping("/face")
    public AjaxResult addFace(HttpServletRequest req, @RequestBody TbWorkerFace face) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long id = AppTokenUtil.getWorkerId(req);
        face.setWorkerId(id);
        face.setCollectTime(new Date());
        faceMapper.insertTbWorkerFace(face);
        TbWorker w = workerMapper.selectTbWorkerById(id);
        if (w != null) { w.setFaceStatus("1"); workerMapper.updateTbWorker(w); }
        return AjaxResult.success(Collections.singletonMap("id", face.getId()));
    }

    @GetMapping("/face")
    public AjaxResult getFace(HttpServletRequest req) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long id = AppTokenUtil.getWorkerId(req);
        TbWorkerFace q = new TbWorkerFace(); q.setWorkerId(id);
        List<TbWorkerFace> list = faceMapper.selectTbWorkerFaceList(q);
        // 始终返回 data 对象，前端判断 faceImgUrl 字段
        TbWorkerFace f = list.isEmpty() ? new TbWorkerFace() : list.get(0);
        return AjaxResult.success(f);
    }
}
