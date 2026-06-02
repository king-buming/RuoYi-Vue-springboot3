package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.TbWorker;

/**
 * 用户：弱约束——角色仍允许分配，但标记为「待审核」并返回缺失资质列表。
 */

/**
 * 人员档案Service接口
 * 
 * @author ruoyi
 * @date 2026-06-01
 */
public interface ITbWorkerService 
{
    /**
     * 查询人员档案
     * 
     * @param id 人员档案主键
     * @return 人员档案
     */
    public TbWorker selectTbWorkerById(Long id);

    /**
     * 查询人员档案列表
     * 
     * @param tbWorker 人员档案
     * @return 人员档案集合
     */
    public List<TbWorker> selectTbWorkerList(TbWorker tbWorker);

    /**
     * 新增人员档案
     * 
     * @param tbWorker 人员档案
     * @return 结果
     */
    public int insertTbWorker(TbWorker tbWorker);

    /**
     * 修改人员档案
     * 
     * @param tbWorker 人员档案
     * @return 结果
     */
    public int updateTbWorker(TbWorker tbWorker);

    /**
     * 批量删除人员档案
     * 
     * @param ids 需要删除的人员档案主键集合
     * @return 结果
     */
    public int deleteTbWorkerByIds(Long[] ids);

    /**
     * 删除人员档案信息
     *
     * @param id 人员档案主键
     * @return 结果
     */
    public int deleteTbWorkerById(Long id);

    /**
     * 查询所有人员 {id, workerName} 供下拉选择
     */
    public List<TbWorker> selectWorkerOptions();
    public List<TbWorker> selectActiveWorkerOptions();

    /** 检查身份证号唯一 */
    public boolean checkIdCardUnique(String idCard, Long excludeId);

    /**
     * 校验人员角色资质要求（统一规则入口）。
     * @return 缺失资质的中文描述列表；空列表=全部合规。
     *         弱约束：若缺资质，同时将该人员 audit_status 退回 '0'。
     */
    public List<String> validateRoleRequirements(Long workerId, List<Long> roleIds);

    /** 人员是否有效（存在且未删除），用于各接口前置校验 */
    public boolean isWorkerActive(Long workerId);
}
