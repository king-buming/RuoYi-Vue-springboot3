package com.ruoyi.web.controller.app;

import java.util.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.domain.TbNotification;
import com.ruoyi.system.mapper.TbNotificationMapper;

@RestController
@RequestMapping("/app/notification")
public class AppNotificationController
{
    @Autowired private TbNotificationMapper notifMapper;

    @GetMapping("/list")
    public AjaxResult list(HttpServletRequest req) {
        Long id = AppTokenUtil.getWorkerId(req);
        if (id == null) return AjaxResult.error(401, "未登录");
        return AjaxResult.success(notifMapper.selectByWorker(id));
    }

    @GetMapping("/unread")
    public AjaxResult unread(HttpServletRequest req) {
        Long id = AppTokenUtil.getWorkerId(req);
        if (id == null) return AjaxResult.error(401, "未登录");
        return AjaxResult.success(Collections.singletonMap("count", notifMapper.countUnread(id)));
    }

    @PutMapping("/{id}/read")
    public AjaxResult read(@PathVariable Long id, HttpServletRequest req) {
        Long workerId = AppTokenUtil.getWorkerId(req);
        if (workerId == null) return AjaxResult.error(401, "未登录");
        notifMapper.markRead(id, workerId);
        return AjaxResult.success();
    }
}
