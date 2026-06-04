package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.HwReview;

/**
 * 作业审核Mapper接口
 *
 * @author ruoyi
 */
public interface HwReviewMapper
{
    List<HwReview> selectHwReviewList(HwReview hwReview);

    HwReview selectHwReviewById(Long reviewId);

    HwReview selectHwReviewByPlanId(Long planId);

    int insertHwReview(HwReview hwReview);

    int updateHwReview(HwReview hwReview);

    int deleteHwReviewById(Long reviewId);

    int deleteHwReviewByIds(Long[] reviewIds);
}
