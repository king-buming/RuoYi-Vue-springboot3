package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.TbWorkerRole;

/**
 * 角色规则Service接口
 *
 * @author ruoyi
 * @date 2026-06-01
 */
public interface ITbWorkerRoleService
{
    /**
     * 查询角色规则
     *
     * @param id 角色规则主键
     * @return 角色规则
     */
    public TbWorkerRole selectTbWorkerRoleById(Long id);

    /**
     * 查询角色规则列表
     *
     * @param tbWorkerRole 角色规则
     * @return 角色规则集合
     */
    public List<TbWorkerRole> selectTbWorkerRoleList(TbWorkerRole tbWorkerRole);

    /**
     * 新增角色规则
     *
     * @param tbWorkerRole 角色规则
     * @return 结果
     */
    public int insertTbWorkerRole(TbWorkerRole tbWorkerRole);

    /**
     * 修改角色规则
     *
     * @param tbWorkerRole 角色规则
     * @return 结果
     */
    public int updateTbWorkerRole(TbWorkerRole tbWorkerRole);

    /**
     * 批量删除角色规则
     *
     * @param ids 需要删除的角色规则主键集合
     * @return 结果
     */
    public int deleteTbWorkerRoleByIds(Long[] ids);

    /**
     * 删除角色规则信息
     *
     * @param id 角色规则主键
     * @return 结果
     */
    public int deleteTbWorkerRoleById(Long id);
}
