package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.TbWorker;

/**
 * 人员档案Mapper接口
 * 
 * @author ruoyi
 * @date 2026-06-01
 */
public interface TbWorkerMapper 
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
     * 删除人员档案
     * 
     * @param id 人员档案主键
     * @return 结果
     */
    public int deleteTbWorkerById(Long id);

    /**
     * 批量删除人员档案
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteTbWorkerByIds(Long[] ids);

    /**
     * 查询所有人员 {id, workerName} 供下拉选择
     */
    public List<TbWorker> selectWorkerOptions();
    public List<TbWorker> selectActiveWorkerOptions();

    /** 身份证号唯一检查，excludeId 非空时排除该 ID（修改场景） */
    public int checkIdCardUnique(@Param("idCard") String idCard, @Param("excludeId") Long excludeId);
}
