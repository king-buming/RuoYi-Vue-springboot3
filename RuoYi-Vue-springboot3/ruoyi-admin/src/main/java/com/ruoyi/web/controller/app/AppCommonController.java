package com.ruoyi.web.controller.app;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.system.mapper.SysDictDataMapper;

/** 手机端通用数据接口 */
@RestController
@RequestMapping("/app/common")
public class AppCommonController
{
    @Autowired private SysDictDataMapper dictDataMapper;

    /** 获取字典数据（证件类型、打卡类型等） */
    @GetMapping("/dicts")
    public AjaxResult dicts(@RequestParam String types) {
        Map<String, List<Map<String, String>>> result = new HashMap<>();
        for (String type : types.split(",")) {
            SysDictData q = new SysDictData(); q.setDictType(type.trim());
            List<Map<String, String>> items = new ArrayList<>();
            dictDataMapper.selectDictDataList(q).forEach(d -> {
                Map<String, String> item = new HashMap<>();
                item.put("value", d.getDictValue());
                item.put("label", d.getDictLabel());
                items.add(item);
            });
            result.put(type.trim(), items);
        }
        return AjaxResult.success(result);
    }
}
