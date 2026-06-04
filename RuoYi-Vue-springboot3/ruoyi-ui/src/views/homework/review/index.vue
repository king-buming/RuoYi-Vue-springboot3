<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目名称" prop="planName">
        <el-input v-model="queryParams.planName" placeholder="请输入项目名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="审核状态" prop="reviewStatus">
        <el-select v-model="queryParams.reviewStatus" placeholder="请选择审核状态" clearable>
          <el-option v-for="d in reviewStatusOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="申请人" prop="applicant">
        <el-input v-model="queryParams.applicant" placeholder="请输入申请人" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="申请时间">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" value-format="yyyy-MM-dd" style="width:240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['homework:review:reject']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="reviewList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="审核ID" align="center" prop="reviewId" width="80" />
      <el-table-column label="项目名称" align="center" prop="planName" show-overflow-tooltip />
      <el-table-column label="作业类型" align="center" prop="workType" width="100" />
      <el-table-column label="施工单位" align="center" prop="constructionUnit" show-overflow-tooltip />
      <el-table-column label="申请人" align="center" prop="applicant" width="100" />
      <el-table-column label="申请时间" align="center" prop="applyTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.applyTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" align="center" prop="reviewStatus" width="100">
        <template slot-scope="scope">
          <el-tag :type="['info','success','danger'][Number(scope.row.reviewStatus)]">
            {{ ['待审核','已通过','已驳回'][Number(scope.row.reviewStatus)] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审核人" align="center" prop="reviewer" width="100" />
      <el-table-column label="审核时间" align="center" prop="reviewTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.reviewTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="reviewOpinion" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button v-if="scope.row.reviewStatus === '0'" size="mini" type="text" icon="el-icon-circle-check"
            @click="handleApprove(scope.row)" v-hasPermi="['homework:review:approve']">通过</el-button>
          <el-button v-if="scope.row.reviewStatus === '0'" size="mini" type="text" icon="el-icon-circle-close"
            @click="handleReject(scope.row)" v-hasPermi="['homework:review:reject']">驳回</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete"
            @click="handleDelete(scope.row)" v-hasPermi="['homework:review:reject']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 审核弹窗 -->
    <el-dialog :title="reviewTitle" :visible.sync="reviewOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border size="small" style="margin-bottom:15px">
        <el-descriptions-item label="项目名称">{{ currentPlan.projectName }}</el-descriptions-item>
        <el-descriptions-item label="作业类型">{{ currentPlan.workType }}</el-descriptions-item>
        <el-descriptions-item label="施工单位">{{ currentPlan.constructionUnit }}</el-descriptions-item>
        <el-descriptions-item label="施工点">{{ currentPlan.constructionSite }}</el-descriptions-item>
        <el-descriptions-item label="计划作业时间">{{ parseTime(currentPlan.planWorkTime) }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ currentReview.applicant }}</el-descriptions-item>
        <el-descriptions-item label="作业内容" :span="2">{{ currentPlan.workContent }}</el-descriptions-item>
        <el-descriptions-item label="参与人员" :span="2">
          <el-tag v-for="w in currentPlanWorkers" :key="w.id || w.workerId" size="small" style="margin:2px">
            {{ w.workerName }}{{ w.roleType ? ' (' + w.roleType + ')' : '' }}
          </el-tag>
          <span v-if="currentPlanWorkers.length === 0" style="color:#999">—</span>
        </el-descriptions-item>
      </el-descriptions>
      <el-form ref="reviewForm" :model="reviewForm" label-width="80px">
        <el-form-item label="审核意见" prop="reviewOpinion">
          <el-input v-model="reviewForm.reviewOpinion" type="textarea" :rows="3" placeholder="请输入审核意见" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="success" @click="submitApprove" v-hasPermi="['homework:review:approve']">审核通过</el-button>
        <el-button type="danger" @click="submitReject" v-hasPermi="['homework:review:reject']">审核驳回</el-button>
        <el-button @click="reviewOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listReview, approveReview, rejectReview, delReview } from "@/api/homework/review"
import { getPlan, getPlanWorkers } from "@/api/homework/plan"

export default {
  name: "HwReview",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      reviewList: [],
      dateRange: [],
      reviewOpen: false,
      reviewTitle: "",
      reviewAction: "",
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        planName: undefined,
        reviewStatus: undefined,
        applicant: undefined
      },
      reviewForm: {
        reviewOpinion: ""
      },
      currentReview: {},
      currentPlan: {},
      currentPlanWorkers: [],
      reviewStatusOptions: [
        { label: '待审核', value: '0' },
        { label: '已通过', value: '1' },
        { label: '已驳回', value: '2' }
      ]
    }
  },
  created() {
    if (this.$route.query.planId) {
      this.queryParams.planId = Number(this.$route.query.planId)
    }
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      this.queryParams.params = {}
      if (this.dateRange && this.dateRange.length === 2) {
        this.queryParams.params.beginTime = this.dateRange[0]
        this.queryParams.params.endTime = this.dateRange[1]
      }
      listReview(this.queryParams).then(response => {
        this.reviewList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.dateRange = []
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.reviewId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleApprove(row) {
      this.currentReview = row
      this.reviewTitle = "审核通过 - " + row.planName
      this.reviewAction = "approve"
      this.reviewForm = { reviewOpinion: "" }
      this.currentPlanWorkers = []
      this.currentPlan = {}
      Promise.all([
        getPlan(row.planId),
        getPlanWorkers(row.planId)
      ]).then(([planRes, workerRes]) => {
        this.currentPlan = planRes.data || {}
        this.currentPlanWorkers = workerRes.data || []
        this.reviewOpen = true
      }).catch(() => {
        this.$modal.msgError("加载计划详情失败")
      })
    },
    handleReject(row) {
      this.currentReview = row
      this.reviewTitle = "审核驳回 - " + row.planName
      this.reviewAction = "reject"
      this.reviewForm = { reviewOpinion: "" }
      this.currentPlanWorkers = []
      this.currentPlan = {}
      Promise.all([
        getPlan(row.planId),
        getPlanWorkers(row.planId)
      ]).then(([planRes, workerRes]) => {
        this.currentPlan = planRes.data || {}
        this.currentPlanWorkers = workerRes.data || []
        this.reviewOpen = true
      }).catch(() => {
        this.$modal.msgError("加载计划详情失败")
      })
    },
    submitApprove() {
      approveReview({ reviewId: this.currentReview.reviewId, reviewOpinion: this.reviewForm.reviewOpinion }).then(r => {
        if (r.code === 200) {
          this.$modal.msgSuccess("审核通过")
          this.reviewOpen = false
          this.getList()
        } else {
          this.$modal.msgError(r.msg)
        }
      })
    },
    submitReject() {
      rejectReview({ reviewId: this.currentReview.reviewId, reviewOpinion: this.reviewForm.reviewOpinion }).then(r => {
        if (r.code === 200) {
          this.$modal.msgSuccess("已驳回")
          this.reviewOpen = false
          this.getList()
        } else {
          this.$modal.msgError(r.msg)
        }
      })
    },
    handleDelete(row) {
      const reviewIds = row.reviewId || this.ids
      this.$modal.confirm('是否确认删除审核记录编号为"' + reviewIds + '"的数据项？').then(function() {
        return delReview(reviewIds)
      }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>
