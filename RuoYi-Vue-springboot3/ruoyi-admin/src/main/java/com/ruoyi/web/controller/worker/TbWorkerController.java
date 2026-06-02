package com.ruoyi.web.controller.worker;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.domain.TbWorkerAudit;
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.domain.TbWorkerFace;
import com.ruoyi.system.mapper.TbWorkerAuditMapper;
import com.ruoyi.system.mapper.TbWorkerCertMapper;
import com.ruoyi.system.mapper.TbWorkerFaceMapper;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.service.ITbWorkerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 人员档案Controller
 * 
 * @author ruoyi
 * @date 2026-06-01
 */
@RestController
@RequestMapping("/worker/worker")
public class TbWorkerController extends BaseController
{
    @Autowired
    private ITbWorkerService tbWorkerService;

    @Autowired
    private TbWorkerRoleRelMapper tbWorkerRoleRelMapper;

    @Autowired
    private TbWorkerCertMapper tbWorkerCertMapper;

    @Autowired
    private TbWorkerFaceMapper tbWorkerFaceMapper;

    @Autowired
    private TbWorkerAuditMapper tbWorkerAuditMapper;


    /**
     * 查询人员档案列表
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:list')")
    @GetMapping("/list")
    public TableDataInfo list(TbWorker tbWorker)
    {
        startPage();
        List<TbWorker> list = tbWorkerService.selectTbWorkerList(tbWorker);
        return getDataTable(list);
    }

    /**
     * 导出人员档案列表
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:export')")
    @Log(title = "人员档案", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TbWorker tbWorker)
    {
        List<TbWorker> list = tbWorkerService.selectTbWorkerList(tbWorker);
        ExcelUtil<TbWorker> util = new ExcelUtil<TbWorker>(TbWorker.class);
        util.exportExcel(response, list, "人员档案数据");
    }

    /**
     * 获取人员档案详细信息
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(tbWorkerService.selectTbWorkerById(id));
    }

    /**
     * 新增人员档案
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:add')")
    @Log(title = "人员档案", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TbWorker tbWorker)
    {
        if (!tbWorkerService.checkIdCardUnique(tbWorker.getIdCard(), null)) {
            return error("身份证号已存在：" + tbWorker.getIdCard());
        }
        tbWorkerService.insertTbWorker(tbWorker);
        return success(tbWorker.getId());
    }

    /**
     * 修改人员档案
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:edit')")
    @Log(title = "人员档案", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TbWorker tbWorker)
    {
        if (!tbWorkerService.checkIdCardUnique(tbWorker.getIdCard(), tbWorker.getId())) {
            return error("身份证号已存在：" + tbWorker.getIdCard());
        }
        return toAjax(tbWorkerService.updateTbWorker(tbWorker));
    }

    /**
     * 删除人员档案
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:remove')")
    @Log(title = "人员档案", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        for (Long id : ids) {
            TbWorker w = tbWorkerService.selectTbWorkerById(id);
            if (w != null) {
                w.setDelFlag("2");
                w.setStatus("2");  // 同时设为禁用
                tbWorkerService.updateTbWorker(w);
            }
        }
        return success();
    }

    /**
     * 获取人员已分配的角色 ID 列表
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:query')")
    @GetMapping("/{workerId}/roles")
    public AjaxResult getRoles(@PathVariable("workerId") Long workerId)
    {
        List<Long> roleIds = tbWorkerRoleRelMapper.selectRoleIdsByWorkerId(workerId);
        return success(roleIds);
    }

    /**
     * 保存人员角色关联（全量替换：先清旧再插新）
     */
    @PreAuthorize("@ss.hasPermi('worker:worker:edit')")
    @Log(title = "人员角色", businessType = BusinessType.UPDATE)
    @PutMapping("/{workerId}/roles")
    public AjaxResult saveRoles(@PathVariable("workerId") Long workerId, @RequestBody Map<String, List<Long>> body)
    {
        List<Long> roleIds = body.get("roleIds");
        tbWorkerRoleRelMapper.deleteByWorkerId(workerId);
        if (roleIds != null) {
            for (Long roleId : roleIds) {
                tbWorkerRoleRelMapper.insertWorkerRole(workerId, roleId);
            }
        }
        // 统一规则入口：校验角色资质（Service 层弱约束）
        if (!tbWorkerService.isWorkerActive(workerId)) {
            return error("该人员已被归档，无法分配角色");
        }
        List<String> missing = tbWorkerService.validateRoleRequirements(workerId, roleIds);
        AjaxResult r = success();
        if (!missing.isEmpty()) r.put("warnings", missing);
        return r;
    }

    /**
     * 人员下拉选项（{id, workerName}，供其他页面的选择器使用）
     */
    /** 全部人员下拉（含已归档，供历史展示用） */
    @GetMapping("/options")
    public AjaxResult options() {
        return success(tbWorkerService.selectWorkerOptions());
    }

    /** 有效人员下拉（仅 del_flag='0'，供新增业务数据用） */
    @GetMapping("/options/active")
    public AjaxResult activeOptions() {
        return success(tbWorkerService.selectActiveWorkerOptions());
    }

    /**
     * 查询某人员的资质证件
     */
    @GetMapping("/{workerId}/certs")
    public AjaxResult getCerts(@PathVariable("workerId") Long workerId)
    {
        TbWorkerCert q = new TbWorkerCert();
        q.setWorkerId(workerId);
        return success(tbWorkerCertMapper.selectTbWorkerCertList(q));
    }

    /**
     * 查询某人员的人脸信息
     */
    @GetMapping("/{workerId}/faces")
    public AjaxResult getFaces(@PathVariable("workerId") Long workerId)
    {
        TbWorkerFace q = new TbWorkerFace();
        q.setWorkerId(workerId);
        return success(tbWorkerFaceMapper.selectTbWorkerFaceList(q));
    }

    /**
     * 查询某人员的审核记录
     */
    @GetMapping("/{workerId}/audits")
    public AjaxResult getAudits(@PathVariable("workerId") Long workerId)
    {
        TbWorkerAudit q = new TbWorkerAudit();
        q.setWorkerId(workerId);
        return success(tbWorkerAuditMapper.selectTbWorkerAuditList(q));
    }

    /** 查询所有人员角色映射 [{worker_id, roleName}]，供列表展示 */
    @GetMapping("/allRoleNames")
    public AjaxResult allRoleNames()
    {
        return success(tbWorkerRoleRelMapper.selectAllWithRoleNames());
    }
}
