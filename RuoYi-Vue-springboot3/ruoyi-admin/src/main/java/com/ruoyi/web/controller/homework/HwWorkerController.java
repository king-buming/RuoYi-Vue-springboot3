package com.ruoyi.web.controller.homework;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.HwWorker;
import com.ruoyi.system.service.IHwWorkerService;

/**
 * 施工人员
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/homework/worker")
public class HwWorkerController extends BaseController
{
    @Autowired
    private IHwWorkerService hwWorkerService;

    @PreAuthorize("@ss.hasPermi('homework:worker:list')")
    @GetMapping("/list")
    public TableDataInfo list(HwWorker worker)
    {
        startPage();
        List<HwWorker> list = hwWorkerService.selectHwWorkerList(worker);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('homework:worker:query')")
    @GetMapping(value = "/{workerId}")
    public AjaxResult getInfo(@PathVariable Long workerId)
    {
        return success(hwWorkerService.selectHwWorkerById(workerId));
    }

    @PreAuthorize("@ss.hasPermi('homework:worker:add')")
    @Log(title = "人员管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody HwWorker worker)
    {
        worker.setCreateBy(getUsername());
        return toAjax(hwWorkerService.insertHwWorker(worker));
    }

    @PreAuthorize("@ss.hasPermi('homework:worker:edit')")
    @Log(title = "人员管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody HwWorker worker)
    {
        worker.setUpdateBy(getUsername());
        return toAjax(hwWorkerService.updateHwWorker(worker));
    }

    @PreAuthorize("@ss.hasPermi('homework:worker:remove')")
    @Log(title = "人员管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{workerIds}")
    public AjaxResult remove(@PathVariable Long[] workerIds)
    {
        return toAjax(hwWorkerService.deleteHwWorkerByIds(workerIds));
    }
}
