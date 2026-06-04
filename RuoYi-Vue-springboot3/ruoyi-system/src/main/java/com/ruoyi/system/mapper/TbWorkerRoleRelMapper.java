package com.ruoyi.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 人员角色关联Mapper接口（tb_worker_role_rel）
 *
 * @author ruoyi
 * @date 2026-06-02
 */
public interface TbWorkerRoleRelMapper
{
    /**
     * 查询某人员已分配的角色 ID 列表
     */
    public List<Long> selectRoleIdsByWorkerId(Long workerId);

    /**
     * 删除某人员的所有角色关联（全量替换前先清空）
     */
    public int deleteByWorkerId(Long workerId);

    /**
     * 插入一条人员↔角色关联
     */
    public int insertWorkerRole(@Param("workerId") Long workerId, @Param("roleId") Long roleId);

    /** 查询所有人-角色映射（worker_id + role_name） */
    public List<Map<String, Object>> selectAllWithRoleNames();

    /** 查询某人员已分配的角色编码列表 */
    public List<String> selectRoleCodesByWorkerId(Long workerId);

    /** 查询所有角色及其下的人员（role_code, role_name, worker_id, worker_name） */
    public List<Map<String, Object>> selectWorkersByRole();
}
