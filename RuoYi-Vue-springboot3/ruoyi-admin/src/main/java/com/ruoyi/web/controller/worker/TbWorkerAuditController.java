package com.ruoyi.web.controller.worker;

import java.util.List;
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
import com.ruoyi.system.domain.TbWorkerAudit;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.service.ITbWorkerAuditService;
import com.ruoyi.system.service.ITbWorkerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 审核记录Controller（审核驱动业务状态逻辑在 ServiceImpl 中）
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@RestController
@RequestMapping("/worker/audit")
public class TbWorkerAuditController extends BaseController
{
    @Autowired
    private ITbWorkerAuditService tbWorkerAuditService;
    @Autowired
    private ITbWorkerService tbWorkerService;
    @Autowired
    private TbWorkerRoleRelMapper roleRelMapper;

    @PreAuthorize("@ss.hasPermi('worker:audit:list')")
    @GetMapping("/list")
    public TableDataInfo list(TbWorkerAudit tbWorkerAudit) {
        startPage();
        return getDataTable(tbWorkerAuditService.selectTbWorkerAuditList(tbWorkerAudit));
    }

    @PreAuthorize("@ss.hasPermi('worker:audit:export')")
    @Log(title = "审核记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TbWorkerAudit tbWorkerAudit) {
        List<TbWorkerAudit> list = tbWorkerAuditService.selectTbWorkerAuditList(tbWorkerAudit);
        ExcelUtil<TbWorkerAudit> util = new ExcelUtil<>(TbWorkerAudit.class);
        util.exportExcel(response, list, "审核记录数据");
    }

    @PreAuthorize("@ss.hasPermi('worker:audit:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(tbWorkerAuditService.selectTbWorkerAuditById(id));
    }

    /** 新增审核记录 → Service 层自动回写业务主表状态 */
    @PreAuthorize("@ss.hasPermi('worker:audit:add')")
    @Log(title = "审核记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TbWorkerAudit tbWorkerAudit) {
        if (!tbWorkerService.isWorkerActive(tbWorkerAudit.getWorkerId())) {
            return error("该人员已被归档，无法新增审核");
        }
        tbWorkerAuditService.insertTbWorkerAudit(tbWorkerAudit);
        return buildResult(tbWorkerAudit);
    }

    /** 修改审核记录 → Service 层自动回写业务主表状态 */
    @PreAuthorize("@ss.hasPermi('worker:audit:edit')")
    @Log(title = "审核记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TbWorkerAudit tbWorkerAudit) {
        if (!tbWorkerService.isWorkerActive(tbWorkerAudit.getWorkerId())) {
            return error("该人员已被归档，无法修改审核");
        }
        tbWorkerAuditService.updateTbWorkerAudit(tbWorkerAudit);
        return buildResult(tbWorkerAudit);
    }

    private AjaxResult buildResult(TbWorkerAudit a) {
        AjaxResult r = success();
        if ("worker".equals(a.getBizType()) && a.getBizId() != null) {
            java.util.List<Long> roleIds = roleRelMapper.selectRoleIdsByWorkerId(a.getBizId());
            java.util.List<String> missing = tbWorkerService.validateRoleRequirements(a.getBizId(), roleIds);
            if (!missing.isEmpty()) r.put("warnings", missing);
        }
        return r;
    }

    @PreAuthorize("@ss.hasPermi('worker:audit:remove')")
    @Log(title = "审核记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tbWorkerAuditService.deleteTbWorkerAuditByIds(ids));
    }
}
