<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="作业计划" prop="planId">
        <el-input v-model="queryParams.planId" placeholder="请输入计划ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="姓名" prop="userName">
        <el-input v-model="queryParams.userName" placeholder="请输入打卡人员姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="打卡类型" prop="checkType">
        <el-select v-model="queryParams.checkType" placeholder="请选择" clearable>
          <el-option v-for="d in checkTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="打卡方式" prop="checkMethod">
        <el-select v-model="queryParams.checkMethod" placeholder="请选择" clearable>
          <el-option v-for="d in checkMethodOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="打卡状态" prop="checkStatus">
        <el-select v-model="queryParams.checkStatus" placeholder="请选择" clearable>
          <el-option v-for="d in checkStatusOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="时间范围">
        <el-date-picker v-model="dateRange" type="daterange" range-separator="-" start-placeholder="开始" end-placeholder="结束" value-format="yyyy-MM-dd" style="width:240px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-s-order" size="mini" @click="handleCheckIn" v-hasPermi="['homework:attendance:checkIn']">进场打卡</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-s-home" size="mini" @click="handleCheckOut" v-hasPermi="['homework:attendance:checkOut']">离场打卡</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['homework:attendance:remove']">删除记录</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="attendanceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="记录ID" align="center" prop="attendanceId" width="80" />
      <el-table-column label="作业计划ID" align="center" prop="planId" width="100" />
      <el-table-column label="打卡人员" align="center" prop="userName" width="100" />
      <el-table-column label="打卡类型" align="center" prop="checkType" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.checkType === '0' ? 'primary' : scope.row.checkType === '1' ? 'success' : scope.row.checkType === '2' ? 'info' : 'warning'">
            {{ getCheckTypeLabel(scope.row.checkType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="打卡方式" align="center" prop="checkMethod" width="100">
        <template slot-scope="scope">
          <span>{{ scope.row.checkMethod === '0' ? '人脸' : '公众号' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="打卡时间" align="center" prop="checkTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.checkTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="打卡位置" align="center" prop="location" show-overflow-tooltip />
      <el-table-column label="打卡状态" align="center" prop="checkStatus" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.checkStatus === '0' ? 'success' : scope.row.checkStatus === '1' ? 'danger' : 'warning'">
            {{ scope.row.checkStatus === '0' ? '成功' : scope.row.checkStatus === '1' ? '失败' : '异常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="失败原因" align="center" prop="failReason" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="80">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['homework:attendance:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 打卡弹窗 -->
    <el-dialog :title="checkDialogTitle" :visible.sync="checkOpen" width="500px" append-to-body>
      <el-alert type="warning" :closable="false" style="margin-bottom:15px">
        <template slot="title">
          <span>注意：仅审核通过的作业计划（状态为"待执行"或"进行中"）可进行打卡操作</span>
        </template>
      </el-alert>
      <el-alert type="info" :closable="false" style="margin-bottom:15px">
        <template slot="title">
          <span v-if="checkMode === 'in'">进场打卡：请确保已在施工点范围内，并完成人脸识别</span>
          <span v-else>离场打卡：已完成进场打卡的人员方可进行离场打卡</span>
        </template>
      </el-alert>
      <el-form ref="checkForm" :model="checkForm" label-width="80px">
        <el-form-item label="作业计划" prop="planId">
          <el-input-number v-model="checkForm.planId" :min="1" placeholder="请输入计划ID" style="width:100%" />
        </el-form-item>
        <el-form-item label="用户ID" prop="userId">
          <el-input-number v-model="checkForm.userId" :min="1" placeholder="请输入用户ID" style="width:100%" />
        </el-form-item>
        <el-form-item label="用户姓名" prop="userName">
          <el-input v-model="checkForm.userName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="打卡方式" prop="checkMethod">
          <el-radio-group v-model="checkForm.checkMethod">
            <el-radio label="0">人脸识别</el-radio>
            <el-radio label="1">微信公众号</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="打卡位置" prop="location">
          <el-input v-model="checkForm.location" placeholder="请输入打卡位置" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="checkForm.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitCheck">确 定</el-button>
        <el-button @click="checkOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAttendance, getAttendance, checkIn, checkOut, delAttendance } from "@/api/homework/attendance"

export default {
  name: "HwAttendance",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      attendanceList: [],
      dateRange: [],
      checkOpen: false,
      checkMode: 'in',
      checkDialogTitle: "",
      checkTypeOptions: [
        { label: '进场', value: '0' }, { label: '离场', value: '1' },
        { label: '点到', value: '2' }, { label: '每小时点到', value: '3' }
      ],
      checkMethodOptions: [{ label: '人脸', value: '0' }, { label: '公众号', value: '1' }],
      checkStatusOptions: [{ label: '成功', value: '0' }, { label: '失败', value: '1' }, { label: '异常', value: '2' }],
      queryParams: { pageNum: 1, pageSize: 10, planId: undefined, userName: undefined, checkType: undefined, checkMethod: undefined, checkStatus: undefined },
      checkForm: { planId: undefined, userId: undefined, userName: undefined, checkMethod: '0', location: undefined, remark: undefined }
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
      listAttendance(this.queryParams).then(response => {
        this.attendanceList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getCheckTypeLabel(v) {
      const f = this.checkTypeOptions.find(d => d.value === v)
      return f ? f.label : v
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
      this.ids = selection.map(item => item.attendanceId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleCheckIn() {
      this.checkMode = 'in'
      this.checkDialogTitle = "进场打卡"
      this.checkForm = { planId: undefined, userId: undefined, userName: undefined, checkMethod: '0', location: undefined, remark: undefined }
      this.checkOpen = true
    },
    handleCheckOut() {
      this.checkMode = 'out'
      this.checkDialogTitle = "离场打卡"
      this.checkForm = { planId: undefined, userId: undefined, userName: undefined, checkMethod: '0', location: undefined, remark: undefined }
      this.checkOpen = true
    },
    submitCheck() {
      const api = this.checkMode === 'in' ? checkIn : checkOut
      api(this.checkForm).then(response => {
        if (response.code === 200) {
          this.$modal.msgSuccess(this.checkMode === 'in' ? '进场打卡成功' : '离场打卡成功')
          this.checkOpen = false
          this.getList()
        } else {
          this.$modal.msgError(response.msg)
        }
      })
    },
    handleDelete(row) {
      const ids = row.attendanceId || this.ids
      this.$modal.confirm('是否确认删除打卡记录编号为"' + ids + '"的数据项？').then(function() {
        return delAttendance(ids)
      }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>
