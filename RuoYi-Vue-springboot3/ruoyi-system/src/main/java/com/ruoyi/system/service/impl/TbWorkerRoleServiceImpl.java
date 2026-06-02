package com.ruoyi.system.service.impl;

import java.util.List;
import com.ruoyi.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.system.mapper.TbWorkerRoleMapper;
import com.ruoyi.system.domain.TbWorkerRole;
import com.ruoyi.system.service.ITbWorkerRoleService;

/**
 * 角色规则Service业务层处理
 *
 * @author ruoyi
 * @date 2026-06-01
 */
@Service
public class TbWorkerRoleServiceImpl implements ITbWorkerRoleService
{
    @Autowired
    private TbWorkerRoleMapper tbWorkerRoleMapper;

    /**
     * 查询角色规则
     *
     * @param id 角色规则主键
     * @return 角色规则
     */
    @Override
    public TbWorkerRole selectTbWorkerRoleById(Long id)
    {
        return tbWorkerRoleMapper.selectTbWorkerRoleById(id);
    }

    /**
     * 查询角色规则列表
     *
     * @param tbWorkerRole 角色规则
     * @return 角色规则
     */
    @Override
    public List<TbWorkerRole> selectTbWorkerRoleList(TbWorkerRole tbWorkerRole)
    {
        return tbWorkerRoleMapper.selectTbWorkerRoleList(tbWorkerRole);
    }

    /**
     * 新增角色规则
     *
     * @param tbWorkerRole 角色规则
     * @return 结果
     */
    @Override
    public int insertTbWorkerRole(TbWorkerRole tbWorkerRole)
    {
        tbWorkerRole.setCreateTime(DateUtils.getNowDate());
        return tbWorkerRoleMapper.insertTbWorkerRole(tbWorkerRole);
    }

    /**
     * 修改角色规则
     *
     * @param tbWorkerRole 角色规则
     * @return 结果
     */
    @Override
    public int updateTbWorkerRole(TbWorkerRole tbWorkerRole)
    {
        tbWorkerRole.setUpdateTime(DateUtils.getNowDate());
        return tbWorkerRoleMapper.updateTbWorkerRole(tbWorkerRole);
    }

    /**
     * 批量删除角色规则
     *
     * @param ids 需要删除的角色规则主键
     * @return 结果
     */
    @Override
    public int deleteTbWorkerRoleByIds(Long[] ids)
    {
        return tbWorkerRoleMapper.deleteTbWorkerRoleByIds(ids);
    }

    /**
     * 删除角色规则信息
     *
     * @param id 角色规则主键
     * @return 结果
     */
    @Override
    public int deleteTbWorkerRoleById(Long id)
    {
        return tbWorkerRoleMapper.deleteTbWorkerRoleById(id);
    }
}
