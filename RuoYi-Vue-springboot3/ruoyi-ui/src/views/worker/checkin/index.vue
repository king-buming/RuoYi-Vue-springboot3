<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="人员" prop="workerId">
        <el-select v-model="queryParams.workerId" placeholder="请选择" clearable filterable @change="handleQuery">
          <el-option v-for="w in activeOptions" :key="w.id" :label="w.workerName + ' (ID:' + w.id + ')'" :value="w.id"/>
        </el-select>
      </el-form-item>
      <el-form-item label="打卡类型" prop="checkType">
        <el-select v-model="queryParams.checkType" placeholder="请选择" clearable @change="handleQuery">
          <el-option v-for="d in dict.type.worker_check_type" :key="d.value" :label="d.label" :value="d.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="打卡时间">
        <el-date-picker
          v-model="dateRange" type="daterange" range-separator="至"
          start-placeholder="开始" end-placeholder="结束" value-format="yyyy-MM-dd"
          @change="handleQuery"/>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['worker:checkin:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['worker:checkin:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['worker:checkin:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport" v-hasPermi="['worker:checkin:export']">导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>

    <el-table v-loading="loading" :data="checkinList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="人员" align="center" prop="workerId">
        <template slot-scope="scope">{{ workerName(scope.row.workerId) }} (ID:{{ scope.row.workerId }})</template>
      </el-table-column>
      <el-table-column label="打卡类型" align="center" prop="checkType">
        <template slot-scope="scope"><dict-tag :options="dict.type.worker_check_type" :value="scope.row.checkType"/></template>
      </el-table-column>
      <el-table-column label="打卡时间" align="center" prop="checkTime" width="160">
        <template slot-scope="scope">{{ parseTime(scope.row.checkTime) }}</template>
      </el-table-column>
      <el-table-column label="打卡方式" align="center" prop="checkMethod"/>
      <el-table-column label="定位" align="center" min-width="150">
        <template slot-scope="scope">
          <span v-if="scope.row.latitude && scope.row.longitude">{{ scope.row.latitude }}, {{ scope.row.longitude }}</span>
          <el-tag v-else type="warning" size="mini">缺GPS定位</el-tag>
        </template>
      </el-table-column>
<el-table-column label="照片" align="center" prop="photoUrl" width="80">
        <template slot-scope="scope"><image-preview :src="scope.row.photoUrl" :width="40" :height="40"/></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['worker:checkin:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['worker:checkin:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="550px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="人员" prop="workerId">
          <el-select v-model="form.workerId" placeholder="请选择人员" filterable>
            <el-option v-for="w in activeOptions" :key="w.id" :label="w.workerName + ' (ID:' + w.id + ')'" :value="w.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="打卡类型" prop="checkType">
          <el-radio-group v-model="form.checkType">
            <el-radio v-for="d in dict.type.worker_check_type" :key="d.value" :label="d.value">{{ d.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="打卡时间" prop="checkTime">
          <el-date-picker clearable v-model="form.checkTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="请选择"/>
        </el-form-item>
        <el-form-item label="打卡方式" prop="checkMethod">
          <el-input v-model="form.checkMethod" placeholder="手动/公众号/AI"/>
        </el-form-item>
        <el-form-item label="现场照片" prop="photoUrl">
          <image-upload v-model="form.photoUrl" :limit="1"/>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
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
import { listCheckin, getCheckin, delCheckin, addCheckin, updateCheckin } from "@/api/worker/checkin"
import { listWorkerOptions, listActiveWorkerOptions } from "@/api/worker/worker"

export default {
  name: "WorkerCheckin",
  dicts: ['worker_check_type'],
  data() {
    return {
      loading: true, ids: [], single: true, multiple: true, showSearch: true, total: 0,
      checkinList: [], title: "", open: false, dateRange: [],
      workerOptions: [],
      activeOptions: [],
      queryParams: { pageNum: 1, pageSize: 10, workerId: undefined, checkType: undefined },
      form: {},
      rules: {
        workerId: [{ required: true, message: "人员不能为空", trigger: "change" }],
        checkType: [{ required: true, message: "打卡类型不能为空", trigger: "change" }],
        checkTime: [{ required: true, message: "打卡时间不能为空", trigger: "change" }]
      }
    }
  },
  created() { this.getList(); listWorkerOptions().then(r => { this.workerOptions = r.data }); listActiveWorkerOptions().then(r => { this.activeOptions = r.data }) },
  activated() { this.getList(); listWorkerOptions().then(r => { this.workerOptions = r.data }); listActiveWorkerOptions().then(r => { this.activeOptions = r.data }) },
  methods: {
    getList() {
      this.loading = true
      const p = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        p.params = {
          beginCheckTime: this.dateRange[0] + ' 00:00:00',
          endCheckTime: this.dateRange[1] + ' 23:59:59'
        }
      }
      listCheckin(p).then(r => { this.checkinList = r.rows; this.total = r.total; this.loading = false })
    },
    cancel() { this.open = false; this.reset() },
    reset() {
      this.form = { id: undefined, workerId: undefined, checkType: '1', checkTime: undefined, checkMethod: '手动', photoUrl: undefined, remark: undefined }
      this.resetForm("form")
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.dateRange = []; this.resetForm("queryForm"); this.handleQuery() },
    handleSelectionChange(s) { this.ids = s.map(i => i.id); this.single = s.length != 1; this.multiple = !s.length },
    handleAdd() { this.reset(); this.open = true; this.title = "添加打卡记录" },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getCheckin(id).then(r => { this.form = r.data; this.open = true; this.title = "修改打卡记录" })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          (this.form.id != undefined ? updateCheckin(this.form) : addCheckin(this.form)).then(() => {
            this.$modal.msgSuccess("操作成功"); this.open = false; this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除打卡记录编号为"' + ids + '"的数据项？').then(() => delPlan(ids)).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
      function delPlan(id) { return delCheckin(id) }
    },
    handleExport() {
      const p = { ...this.queryParams }
      if (this.dateRange && this.dateRange.length === 2) {
        p.params = { beginCheckTime: this.dateRange[0] + ' 00:00:00', endCheckTime: this.dateRange[1] + ' 23:59:59' }
      }
      this.download('worker/checkin/export', p, `checkin_${new Date().getTime()}.xlsx`)
    },
    workerName(id) { const w = this.workerOptions.find(o => o.id === id); if (!w) return 'ID:' + (id || ''); return w.delFlag === '2' ? w.workerName + '(已归档)' : w.workerName }
  }
}
</script>
