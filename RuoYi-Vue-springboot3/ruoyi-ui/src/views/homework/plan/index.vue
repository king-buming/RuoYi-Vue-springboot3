<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目名称" prop="projectName">
        <el-input v-model="queryParams.projectName" placeholder="请输入项目名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="作业类型" prop="workType">
        <el-select v-model="queryParams.workType" placeholder="请选择作业类型" clearable>
          <el-option v-for="d in workTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option v-for="d in statusOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['homework:plan:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['homework:plan:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['homework:plan:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="planList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="计划ID" align="center" prop="planId" width="80" />
      <el-table-column label="市/县" align="center" prop="cityCounty" width="100" />
      <el-table-column label="施工点" align="center" prop="constructionSite" show-overflow-tooltip />
      <el-table-column label="计划作业时间" align="center" prop="planWorkTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.planWorkTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="项目名称" align="center" prop="projectName" show-overflow-tooltip />
      <el-table-column label="作业类型" align="center" prop="workType" width="100">
        <template slot-scope="scope">
          <span>{{ getWorkTypeLabel(scope.row.workType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="施工单位" align="center" prop="constructionUnit" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="['info','primary','success','danger'][Number(scope.row.status)]">
            {{ ['待执行','进行中','已完成','已取消'][Number(scope.row.status)] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleViewAttendance(scope.row)">打卡</el-button>
          <el-dropdown v-if="scope.row.status !== '2' && scope.row.status !== '3'"
            @command="(cmd) => handleStatusChange(scope.row, cmd)" style="margin-left:5px">
            <el-button size="mini" type="text">
              状态<i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item v-if="scope.row.status === '0'" command="1">开始作业</el-dropdown-item>
              <el-dropdown-item v-if="scope.row.status === '1'" command="2">标记完成</el-dropdown-item>
              <el-dropdown-item command="3">取消计划</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['homework:plan:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['homework:plan:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="市/县" prop="cityCounty">
              <el-input v-model="form.cityCounty" placeholder="请输入市/县" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="施工点" prop="constructionSite">
              <el-input v-model="form.constructionSite" placeholder="请输入施工点" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="施工点纬度" prop="siteLatitude">
              <el-input-number v-model="form.siteLatitude" controls-position="right" :precision="7" :step="0.0000001" style="width:100%" placeholder="纬度" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="施工点经度" prop="siteLongitude">
              <el-input-number v-model="form.siteLongitude" controls-position="right" :precision="7" :step="0.0000001" style="width:100%" placeholder="经度" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="计划作业时间" prop="planWorkTime">
              <el-date-picker clearable v-model="form.planWorkTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择时间" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="作业类型" prop="workType">
              <el-select v-model="form.workType" placeholder="请选择作业类型" style="width:100%">
                <el-option v-for="d in workTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="项目名称" prop="projectName">
          <el-input v-model="form.projectName" placeholder="请输入项目名称" />
        </el-form-item>
        <el-form-item label="施工单位" prop="constructionUnit">
          <el-input v-model="form.constructionUnit" placeholder="请输入施工单位" />
        </el-form-item>
        <el-form-item label="参与人员">
          <el-select v-model="selectedWorkers" multiple filterable placeholder="请选择参与人员"
            value-key="workerId" style="width:100%">
            <el-option v-for="w in workerOptions" :key="w.workerId"
              :label="w.workerName + (roleLabel(w.workerId) ? ' (' + roleLabel(w.workerId) + ')' : '')"
              :value="{ workerId: w.workerId, workerName: w.workerName, roleType: roleLabel(w.workerId) }" />
          </el-select>
          <el-tag v-for="w in selectedWorkers" :key="w.workerId" closable style="margin:2px"
            @close="removeWorker(w.workerId)">
            {{ w.workerName }}{{ w.roleType ? ' (' + w.roleType + ')' : '' }}
          </el-tag>
        </el-form-item>
        <el-form-item label="作业内容" prop="workContent">
          <el-input v-model="form.workContent" type="textarea" placeholder="请输入作业内容" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="d in statusOptions" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPlan, getPlan, delPlan, addPlan, updatePlan, changePlanStatus, getPlanWorkers, savePlanWorkers } from "@/api/homework/plan"
import { listWorker, getAllRoleNames } from "@/api/worker/worker"

export default {
  name: "HwPlan",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      planList: [],
      title: "",
      open: false,
      selectedWorkers: [],
      workerOptions: [],
      roleNameMap: {},
      workTypeOptions: [
        { label: '动土', value: '动土' }, { label: '防腐', value: '防腐' },
        { label: '检测', value: '检测' }, { label: '临时用电', value: '临时用电' },
        { label: '受限空间', value: '受限空间' }, { label: '机械作业', value: '机械作业' },
        { label: '修复', value: '修复' }, { label: '点火', value: '点火' }
      ],
      statusOptions: [
        { label: '待执行', value: '0' }, { label: '进行中', value: '1' },
        { label: '已完成', value: '2' }, { label: '已取消', value: '3' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, projectName: undefined, workType: undefined, status: undefined },
      form: {},
      rules: {
        projectName: [{ required: true, message: "项目名称不能为空", trigger: "blur" }],
        workType: [{ required: true, message: "作业类型不能为空", trigger: "change" }],
        planWorkTime: [{ required: true, message: "计划作业时间不能为空", trigger: "change" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listPlan(this.queryParams).then(response => {
        this.planList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getWorkTypeLabel(v) {
      const f = this.workTypeOptions.find(d => d.value === v)
      return f ? f.label : v
    },
    roleLabel(workerId) {
      const names = this.roleNameMap[workerId]
      return names ? names.join(',') : ''
    },
    removeWorker(id) {
      this.selectedWorkers = this.selectedWorkers.filter(w => w.workerId !== id)
    },
    loadWorkerOptions() {
      Promise.all([
        listWorker({ pageNum: 1, pageSize: 999, status: '0' }),
        getAllRoleNames()
      ]).then(([workerRes, roleRes]) => {
        const roleMap = {}
        if (roleRes.data) {
          roleRes.data.forEach(r => { roleMap[r.workerId] = r.roleName })
        }
        this.roleNameMap = roleMap
        this.workerOptions = (workerRes.rows || []).map(w => ({
          ...w,
          workerId: w.id
        }))
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = { planId: undefined, cityCounty: undefined, constructionSite: undefined, siteLatitude: undefined, siteLongitude: undefined, planWorkTime: undefined, projectName: undefined, workType: undefined, constructionUnit: undefined, workers: undefined, workContent: undefined, status: "0", remark: undefined }
      this.selectedWorkers = []
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.planId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.loadWorkerOptions()
      this.open = true
      this.title = "添加作业计划"
    },
    handleUpdate(row) {
      this.reset()
      this.loadWorkerOptions()
      const planId = row.planId || this.ids
      getPlanWorkers(planId).then(r => { this.selectedWorkers = r.data || [] })
      getPlan(planId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改作业计划"
      })
    },
    handleStatusChange(row, newStatus) {
      const labels = { '0': '待执行', '1': '进行中', '2': '已完成', '3': '已取消' }
      this.$modal.confirm('确认将【' + row.projectName + '】状态变更为"' + labels[newStatus] + '"？')
        .then(() => changePlanStatus({ planId: row.planId, status: newStatus }))
        .then(r => { this.getList(); this.$modal.msgSuccess('状态变更成功') })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const save = this.form.planId != undefined ? updatePlan(this.form) : addPlan(this.form)
          save.then(r => {
            if (r.code === 200) {
              const pid = this.form.planId || r.data
              if (pid) {
                return savePlanWorkers(pid, this.selectedWorkers).then(() => {
                  this.$modal.msgSuccess("保存成功"); this.open = false; this.getList()
                })
              }
              this.$modal.msgSuccess("保存成功"); this.open = false; this.getList()
            }
          })
        }
      })
    },
    handleDelete(row) {
      const planIds = row.planId || this.ids
      this.$modal.confirm('是否确认删除计划编号为"' + planIds + '"的数据项？').then(function() {
        return delPlan(planIds)
      }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    },
    handleViewAttendance(row) {
      this.$router.push({ path: '/homework/attendance', query: { planId: row.planId } })
    }
  }
}
</script>
