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
import com.ruoyi.system.domain.TbWorkerCert;
import com.ruoyi.system.service.ITbWorkerCertService;
import com.ruoyi.system.service.ITbWorkerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 资质证件Controller
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@RestController
@RequestMapping("/worker/cert")
public class TbWorkerCertController extends BaseController
{
    @Autowired
    private ITbWorkerCertService tbWorkerCertService;
    @Autowired
    private ITbWorkerService tbWorkerService;

    /**
     * 查询资质证件列表
     */
    @PreAuthorize("@ss.hasPermi('worker:cert:list')")
    @GetMapping("/list")
    public TableDataInfo list(TbWorkerCert tbWorkerCert)
    {
        startPage();
        List<TbWorkerCert> list = tbWorkerCertService.selectTbWorkerCertList(tbWorkerCert);
        return getDataTable(list);
    }

    /**
     * 导出资质证件列表
     */
    @PreAuthorize("@ss.hasPermi('worker:cert:export')")
    @Log(title = "资质证件", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TbWorkerCert tbWorkerCert)
    {
        List<TbWorkerCert> list = tbWorkerCertService.selectTbWorkerCertList(tbWorkerCert);
        ExcelUtil<TbWorkerCert> util = new ExcelUtil<TbWorkerCert>(TbWorkerCert.class);
        util.exportExcel(response, list, "资质证件数据");
    }

    /**
     * 获取资质证件详细信息
     */
    @PreAuthorize("@ss.hasPermi('worker:cert:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(tbWorkerCertService.selectTbWorkerCertById(id));
    }

    /**
     * 新增资质证件
     */
    @PreAuthorize("@ss.hasPermi('worker:cert:add')")
    @Log(title = "资质证件", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TbWorkerCert tbWorkerCert)
    {
        if (!tbWorkerService.isWorkerActive(tbWorkerCert.getWorkerId())) {
            return error("该人员已被归档，无法新增证件");
        }
        return toAjax(tbWorkerCertService.insertTbWorkerCert(tbWorkerCert));
    }

    /**
     * 修改资质证件
     */
    @PreAuthorize("@ss.hasPermi('worker:cert:edit')")
    @Log(title = "资质证件", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TbWorkerCert tbWorkerCert)
    {
        if (!tbWorkerService.isWorkerActive(tbWorkerCert.getWorkerId())) {
            return error("该人员已被归档，无法修改证件");
        }
        return toAjax(tbWorkerCertService.updateTbWorkerCert(tbWorkerCert));
    }

    /**
     * 删除资质证件
     */
    @PreAuthorize("@ss.hasPermi('worker:cert:remove')")
    @Log(title = "资质证件", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(tbWorkerCertService.deleteTbWorkerCertByIds(ids));
    }
}
