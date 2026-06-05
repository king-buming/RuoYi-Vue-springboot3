package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.TbNotification;

public interface TbNotificationMapper
{
    int insert(TbNotification n);
    List<TbNotification> selectByWorker(@Param("workerId") Long workerId);
    int countUnread(@Param("workerId") Long workerId);
    int markRead(@Param("id") Long id);
}
