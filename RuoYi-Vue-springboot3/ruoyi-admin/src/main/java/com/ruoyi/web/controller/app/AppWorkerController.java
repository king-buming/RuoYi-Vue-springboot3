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

    @GetMapping("/certs/{certId}")
    public AjaxResult certDetail(HttpServletRequest req, @PathVariable Long certId) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long workerId = AppTokenUtil.getWorkerId(req);
        TbWorkerCert cert = certMapper.selectTbWorkerCertById(certId);
        if (cert == null || !workerId.equals(cert.getWorkerId())) {
            return AjaxResult.error("证件不存在或无权限查看");
        }
        return AjaxResult.success(cert);
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

    @PutMapping("/certs/{certId}")
    public AjaxResult updateCert(HttpServletRequest req, @PathVariable Long certId, @RequestBody TbWorkerCert cert) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long workerId = AppTokenUtil.getWorkerId(req);
        TbWorkerCert old = certMapper.selectTbWorkerCertById(certId);
        if (old == null || !workerId.equals(old.getWorkerId())) {
            return AjaxResult.error("证件不存在或无权限修改");
        }
        cert.setId(certId);
        cert.setWorkerId(workerId); // 强制用 token 中的身份，避免越权修改
        cert.setAuditStatus("0"); // 重新上传后必须重新审核
        cert.setUpdateTime(new Date());
        certMapper.updateTbWorkerCert(cert);
        return AjaxResult.success();
    }

    @DeleteMapping("/certs/{certId}")
    public AjaxResult deleteCert(HttpServletRequest req, @PathVariable Long certId) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long workerId = AppTokenUtil.getWorkerId(req);
        TbWorkerCert old = certMapper.selectTbWorkerCertById(certId);
        if (old == null || !workerId.equals(old.getWorkerId())) {
            return AjaxResult.error("证件不存在或无权限删除");
        }
        certMapper.deleteTbWorkerCertById(certId);
        return AjaxResult.success();
    }

    @PostMapping("/face")
    public AjaxResult addFace(HttpServletRequest req, @RequestBody TbWorkerFace face) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long id = AppTokenUtil.getWorkerId(req);
        if (face.getFaceImgUrl() == null || face.getFaceImgUrl().trim().isEmpty()) {
            return AjaxResult.error("请先上传人脸照片");
        }

        TbWorkerFace q = new TbWorkerFace();
        q.setWorkerId(id);
        List<TbWorkerFace> existingFaces = faceMapper.selectTbWorkerFaceList(q);
        existingFaces.sort((a, b) -> {
            Long aid = a.getId() == null ? 0L : a.getId();
            Long bid = b.getId() == null ? 0L : b.getId();
            return bid.compareTo(aid);
        });

        TbWorkerFace savedFace;
        Date now = new Date();
        if (existingFaces.isEmpty()) {
            face.setWorkerId(id);
            face.setCollectTime(now);
            faceMapper.insertTbWorkerFace(face);
            savedFace = face;
        } else {
            savedFace = existingFaces.get(0);
            savedFace.setFaceImgUrl(face.getFaceImgUrl());
            savedFace.setFaceFeature("");
            savedFace.setCollectTime(now);
            savedFace.setUpdateTime(now);
            faceMapper.updateTbWorkerFace(savedFace);

            for (int i = 1; i < existingFaces.size(); i++) {
                faceMapper.deleteTbWorkerFaceById(existingFaces.get(i).getId());
            }
        }

        TbWorker w = workerMapper.selectTbWorkerById(id);
        if (w != null) { w.setFaceStatus("1"); workerMapper.updateTbWorker(w); }
        return AjaxResult.success(Collections.singletonMap("id", savedFace.getId()));
    }

    @GetMapping("/face")
    public AjaxResult getFace(HttpServletRequest req) {
        AjaxResult r = checkActive(req); if (r != null) return r;
        Long id = AppTokenUtil.getWorkerId(req);
        TbWorkerFace q = new TbWorkerFace(); q.setWorkerId(id);
        List<TbWorkerFace> list = faceMapper.selectTbWorkerFaceList(q);
        list.sort((a, b) -> {
            Long aid = a.getId() == null ? 0L : a.getId();
            Long bid = b.getId() == null ? 0L : b.getId();
            return bid.compareTo(aid);
        });
        // 始终返回 data 对象，前端判断 faceImgUrl 字段
        TbWorkerFace f = list.isEmpty() ? new TbWorkerFace() : list.get(0);
        return AjaxResult.success(f);
    }
}
