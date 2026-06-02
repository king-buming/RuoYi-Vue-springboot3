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
import com.ruoyi.system.domain.TbWorkerFace;
import com.ruoyi.system.service.ITbWorkerFaceService;
import com.ruoyi.system.service.ITbWorkerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

/**
 * 人脸信息Controller
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@RestController
@RequestMapping("/worker/face")
public class TbWorkerFaceController extends BaseController
{
    @Autowired
    private ITbWorkerFaceService tbWorkerFaceService;
    @Autowired
    private ITbWorkerService tbWorkerService;

    /**
     * 查询人脸信息列表
     */
    @PreAuthorize("@ss.hasPermi('worker:face:list')")
    @GetMapping("/list")
    public TableDataInfo list(TbWorkerFace tbWorkerFace)
    {
        startPage();
        List<TbWorkerFace> list = tbWorkerFaceService.selectTbWorkerFaceList(tbWorkerFace);
        return getDataTable(list);
    }

    /**
     * 导出人脸信息列表
     */
    @PreAuthorize("@ss.hasPermi('worker:face:export')")
    @Log(title = "人脸信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TbWorkerFace tbWorkerFace)
    {
        List<TbWorkerFace> list = tbWorkerFaceService.selectTbWorkerFaceList(tbWorkerFace);
        ExcelUtil<TbWorkerFace> util = new ExcelUtil<TbWorkerFace>(TbWorkerFace.class);
        util.exportExcel(response, list, "人脸信息数据");
    }

    /**
     * 获取人脸信息详细信息
     */
    @PreAuthorize("@ss.hasPermi('worker:face:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(tbWorkerFaceService.selectTbWorkerFaceById(id));
    }

    /**
     * 新增人脸信息
     */
    @PreAuthorize("@ss.hasPermi('worker:face:add')")
    @Log(title = "人脸信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TbWorkerFace tbWorkerFace)
    {
        if (!tbWorkerService.isWorkerActive(tbWorkerFace.getWorkerId())) {
            return error("该人员已被归档，无法新增人脸信息");
        }
        return toAjax(tbWorkerFaceService.insertTbWorkerFace(tbWorkerFace));
    }

    /**
     * 修改人脸信息
     */
    @PreAuthorize("@ss.hasPermi('worker:face:edit')")
    @Log(title = "人脸信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TbWorkerFace tbWorkerFace)
    {
        if (!tbWorkerService.isWorkerActive(tbWorkerFace.getWorkerId())) {
            return error("该人员已被归档，无法修改人脸信息");
        }
        return toAjax(tbWorkerFaceService.updateTbWorkerFace(tbWorkerFace));
    }

    /**
     * 删除人脸信息
     */
    @PreAuthorize("@ss.hasPermi('worker:face:remove')")
    @Log(title = "人脸信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(tbWorkerFaceService.deleteTbWorkerFaceByIds(ids));
    }
}
