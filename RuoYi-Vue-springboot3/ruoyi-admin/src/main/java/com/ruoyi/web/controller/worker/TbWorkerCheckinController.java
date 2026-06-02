package com.ruoyi.web.controller.worker;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.TbWorkerCheckin;
import com.ruoyi.system.service.ITbWorkerCheckinService;
import com.ruoyi.system.service.ITbWorkerService;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.common.core.page.TableDataInfo;

@RestController
@RequestMapping("/worker/checkin")
public class TbWorkerCheckinController extends BaseController
{
    @Autowired
    private ITbWorkerCheckinService tbWorkerCheckinService;
    @Autowired
    private ITbWorkerService tbWorkerService;

    @PreAuthorize("@ss.hasPermi('worker:checkin:list')")
    @GetMapping("/list")
    public TableDataInfo list(TbWorkerCheckin checkin) {
        startPage();
        return getDataTable(tbWorkerCheckinService.selectTbWorkerCheckinList(checkin));
    }

    @PreAuthorize("@ss.hasPermi('worker:checkin:export')")
    @Log(title = "打卡记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TbWorkerCheckin checkin) {
        List<TbWorkerCheckin> list = tbWorkerCheckinService.selectTbWorkerCheckinList(checkin);
        ExcelUtil<TbWorkerCheckin> util = new ExcelUtil<>(TbWorkerCheckin.class);
        util.exportExcel(response, list, "打卡记录");
    }

    @PreAuthorize("@ss.hasPermi('worker:checkin:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(tbWorkerCheckinService.selectTbWorkerCheckinById(id));
    }

    @PreAuthorize("@ss.hasPermi('worker:checkin:add')")
    @Log(title = "打卡记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TbWorkerCheckin checkin) {
        if (!tbWorkerService.isWorkerActive(checkin.getWorkerId())) {
            return error("该人员已被归档，无法新增打卡");
        }
        return toAjax(tbWorkerCheckinService.insertTbWorkerCheckin(checkin));
    }

    @PreAuthorize("@ss.hasPermi('worker:checkin:edit')")
    @Log(title = "打卡记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TbWorkerCheckin checkin) {
        if (!tbWorkerService.isWorkerActive(checkin.getWorkerId())) {
            return error("该人员已被归档，无法修改打卡");
        }
        return toAjax(tbWorkerCheckinService.updateTbWorkerCheckin(checkin));
    }

    @PreAuthorize("@ss.hasPermi('worker:checkin:remove')")
    @Log(title = "打卡记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(tbWorkerCheckinService.deleteTbWorkerCheckinByIds(ids));
    }
}
