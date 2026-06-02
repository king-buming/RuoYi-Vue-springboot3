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
import com.ruoyi.system.domain.TbWorkerRole;
import com.ruoyi.system.service.ITbWorkerRoleService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 角色规则Controller
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@RestController
@RequestMapping("/worker/role")
public class TbWorkerRoleController extends BaseController
{
    @Autowired
    private ITbWorkerRoleService tbWorkerRoleService;

    /**
     * 查询角色规则列表
     */
    @PreAuthorize("@ss.hasPermi('worker:role:list')")
    @GetMapping("/list")
    public TableDataInfo list(TbWorkerRole tbWorkerRole)
    {
        startPage();
        List<TbWorkerRole> list = tbWorkerRoleService.selectTbWorkerRoleList(tbWorkerRole);
        return getDataTable(list);
    }

    /**
     * 导出角色规则列表
     */
    @PreAuthorize("@ss.hasPermi('worker:role:export')")
    @Log(title = "角色规则", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TbWorkerRole tbWorkerRole)
    {
        List<TbWorkerRole> list = tbWorkerRoleService.selectTbWorkerRoleList(tbWorkerRole);
        ExcelUtil<TbWorkerRole> util = new ExcelUtil<TbWorkerRole>(TbWorkerRole.class);
        util.exportExcel(response, list, "角色规则数据");
    }

    /**
     * 获取角色规则详细信息
     */
    @PreAuthorize("@ss.hasPermi('worker:role:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(tbWorkerRoleService.selectTbWorkerRoleById(id));
    }

    /**
     * 新增角色规则
     */
    @PreAuthorize("@ss.hasPermi('worker:role:add')")
    @Log(title = "角色规则", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TbWorkerRole tbWorkerRole)
    {
        return toAjax(tbWorkerRoleService.insertTbWorkerRole(tbWorkerRole));
    }

    /**
     * 修改角色规则
     */
    @PreAuthorize("@ss.hasPermi('worker:role:edit')")
    @Log(title = "角色规则", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TbWorkerRole tbWorkerRole)
    {
        return toAjax(tbWorkerRoleService.updateTbWorkerRole(tbWorkerRole));
    }

    /**
     * 删除角色规则
     */
    @PreAuthorize("@ss.hasPermi('worker:role:remove')")
    @Log(title = "角色规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(tbWorkerRoleService.deleteTbWorkerRoleByIds(ids));
    }
}
