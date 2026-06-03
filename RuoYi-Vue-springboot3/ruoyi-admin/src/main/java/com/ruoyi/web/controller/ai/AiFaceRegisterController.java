package com.ruoyi.web.controller.ai;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.IAiFaceRegisterService;

/**
 * AI人脸注册管理
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/ai/face")
public class AiFaceRegisterController extends BaseController
{
    @Autowired
    private IAiFaceRegisterService faceRegisterService;

    @PreAuthorize("@ss.hasPermi('ai:face:list')")
    @GetMapping("/list")
    public TableDataInfo list(@RequestParam(required = false) String workerName,
                               @RequestParam(required = false) String registerStatus)
    {
        startPage();
        List<?> list = faceRegisterService.selectFaceRegisterList(workerName, registerStatus);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('ai:face:query')")
    @GetMapping(value = "/{registerId}")
    public AjaxResult getInfo(@PathVariable Long registerId)
    {
        return success(faceRegisterService.selectFaceRegisterById(registerId));
    }

    @PreAuthorize("@ss.hasPermi('ai:face:register')")
    @Log(title = "人脸注册", businessType = BusinessType.INSERT)
    @PostMapping("/register/{workerId}")
    public AjaxResult register(@PathVariable Long workerId,
                                @RequestParam(defaultValue = "arcface_r100_001") String modelCode)
    {
        try
        {
            faceRegisterService.registerFace(workerId, modelCode);
            return success("人脸录入成功");
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('ai:face:batch')")
    @Log(title = "人脸注册", businessType = BusinessType.INSERT)
    @PostMapping("/batchRegister")
    public AjaxResult batchRegister(@RequestBody Map<String, Object> params)
    {
        try
        {
            @SuppressWarnings("unchecked")
            List<Integer> ids = (List<Integer>) params.get("workerIds");
            Long[] workerIds = ids.stream().map(Long::valueOf).toArray(Long[]::new);
            String modelCode = (String) params.getOrDefault("modelCode", "arcface_r100_001");
            int successCount = faceRegisterService.batchRegister(workerIds, modelCode);
            return success("批量录入完成：成功" + successCount + "人");
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('ai:face:cancel')")
    @Log(title = "人脸注册", businessType = BusinessType.UPDATE)
    @PutMapping("/cancel/{registerId}")
    public AjaxResult cancel(@PathVariable Long registerId)
    {
        try
        {
            return toAjax(faceRegisterService.cancelRegister(registerId));
        }
        catch (ServiceException e)
        {
            return error(e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('ai:face:cancel')")
    @Log(title = "人脸注册", businessType = BusinessType.DELETE)
    @DeleteMapping("/{registerIds}")
    public AjaxResult remove(@PathVariable Long[] registerIds)
    {
        return toAjax(faceRegisterService.deleteAiFaceRegisterByIds(registerIds));
    }
}
