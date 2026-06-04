package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.HwReview;

/**
 * 作业审核 服务层
 *
 * @author ruoyi
 */
public interface IHwReviewService
{
    List<HwReview> selectHwReviewList(HwReview hwReview);

    HwReview selectHwReviewById(Long reviewId);

    HwReview selectHwReviewByPlanId(Long planId);

    int insertHwReview(HwReview hwReview);

    int updateHwReview(HwReview hwReview);

    int deleteHwReviewById(Long reviewId);

    int deleteHwReviewByIds(Long[] reviewIds);

    int approve(Long reviewId, String opinion);

    int reject(Long reviewId, String opinion);
}
