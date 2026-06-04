package com.ruoyi.web.controller.homework;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
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
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.domain.HwPlan;
import com.ruoyi.system.domain.HwPlanVideo;
import com.ruoyi.system.domain.HwPlanWorker;
import com.ruoyi.system.domain.TbWorker;
import com.ruoyi.system.mapper.TbWorkerMapper;
import com.ruoyi.system.mapper.TbWorkerRoleRelMapper;
import com.ruoyi.system.service.IHwPlanService;
import com.ruoyi.system.service.IHwPlanVideoService;
import com.ruoyi.system.service.IHwPlanWorkerService;

/**
 * 作业计划
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/homework/plan")
public class HwPlanController extends BaseController
{
    @Autowired
    private IHwPlanService hwPlanService;

    @Autowired
    private IHwPlanWorkerService planWorkerService;

    @Autowired
    private IHwPlanVideoService planVideoService;

    @Autowired
    private TbWorkerMapper tbWorkerMapper;

    @Autowired
    private TbWorkerRoleRelMapper tbWorkerRoleRelMapper;

    @PreAuthorize("@ss.hasPermi('homework:plan:list')")
    @GetMapping("/list")
    public TableDataInfo list(HwPlan plan)
    {
        startPage();
        List<HwPlan> list = hwPlanService.selectHwPlanList(plan);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:query')")
    @GetMapping(value = "/{planId}")
    public AjaxResult getInfo(@PathVariable Long planId)
    {
        return success(hwPlanService.selectHwPlanById(planId));
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:add')")
    @Log(title = "作业计划", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody HwPlan plan)
    {
        try
        {
            validatePlanCreator();
            plan.setCreateBy(getUsername());
            hwPlanService.insertHwPlan(plan);
            return success(plan.getPlanId());
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:edit')")
    @Log(title = "作业计划", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody HwPlan plan)
    {
        plan.setUpdateBy(getUsername());
        return toAjax(hwPlanService.updateHwPlan(plan));
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:remove')")
    @Log(title = "作业计划", businessType = BusinessType.DELETE)
    @DeleteMapping("/{planIds}")
    public AjaxResult remove(@PathVariable Long[] planIds)
    {
        return toAjax(hwPlanService.deleteHwPlanByIds(planIds));
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:edit')")
    @Log(title = "作业计划", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody HwPlan hwPlan)
    {
        try
        {
            return toAjax(hwPlanService.changeStatus(hwPlan.getPlanId(), hwPlan.getStatus()));
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:query')")
    @GetMapping("/{planId}/workers")
    public AjaxResult getWorkers(@PathVariable Long planId)
    {
        return success(planWorkerService.selectByPlanId(planId));
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:edit')")
    @PostMapping("/{planId}/workers")
    public AjaxResult saveWorkers(@PathVariable Long planId, @RequestBody List<HwPlanWorker> workers)
    {
        planWorkerService.deleteByPlanId(planId);
        if (!CollectionUtils.isEmpty(workers))
        {
            workers.forEach(w -> {
                w.setPlanId(planId);
                w.setCreateBy(getUsername());
            });
            planWorkerService.batchInsert(workers);
        }
        return success();
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:query')")
    @GetMapping("/{planId}/videos")
    public AjaxResult getVideos(@PathVariable Long planId)
    {
        return success(planVideoService.selectByPlanId(planId));
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:edit')")
    @PostMapping("/{planId}/videos")
    public AjaxResult bindVideo(@PathVariable Long planId, @RequestBody HwPlanVideo pv)
    {
        pv.setPlanId(planId);
        pv.setCreateBy(getUsername());
        return toAjax(planVideoService.insertHwPlanVideo(pv));
    }

    @PreAuthorize("@ss.hasPermi('homework:plan:edit')")
    @DeleteMapping("/{planId}/videos/{id}")
    public AjaxResult unbindVideo(@PathVariable Long id)
    {
        return toAjax(planVideoService.deleteById(id));
    }

    /**
     * 校验作业计划创建者权限：系统管理员 或 施工方人员（unit_type='3' 且角色不是施工人员 role_code!='worker'）
     */
    private void validatePlanCreator()
    {
        if (SecurityUtils.isAdmin())
        {
            return;
        }
        String userName = SecurityUtils.getUsername();
        // 按用户名查找人员
        TbWorker worker = null;
        TbWorker query = new TbWorker();
        query.setWorkerName(userName);
        List<TbWorker> workers = tbWorkerMapper.selectTbWorkerList(query);
        if (!workers.isEmpty())
        {
            worker = workers.get(0);
        }
        // 按手机号查找
        if (worker == null)
        {
            TbWorker phoneQuery = new TbWorker();
            phoneQuery.setPhone(userName);
            List<TbWorker> phoneWorkers = tbWorkerMapper.selectTbWorkerList(phoneQuery);
            if (!phoneWorkers.isEmpty())
            {
                worker = phoneWorkers.get(0);
            }
        }
        if (worker == null)
        {
            throw new ServiceException("未找到您的人员信息，无法创建作业计划");
        }
        if (!"3".equals(worker.getUnitType()))
        {
            throw new ServiceException("仅施工方人员可创建作业计划");
        }
        List<String> roleCodes = tbWorkerRoleRelMapper.selectRoleCodesByWorkerId(worker.getId());
        if (roleCodes.isEmpty())
        {
            throw new ServiceException("您未分配任何人员角色，无法创建作业计划");
        }
        if (roleCodes.contains("worker") && roleCodes.size() == 1)
        {
            throw new ServiceException("普通施工人员无权创建作业计划，请联系施工方管理人员");
        }
    }
}
