<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="姓名" prop="workerName">
        <el-input v-model="queryParams.workerName" placeholder="请输入姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="人员角色" prop="roleType">
        <el-select v-model="queryParams.roleType" placeholder="请选择角色" clearable>
          <el-option v-for="d in roleTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="单位类型" prop="unitType">
        <el-select v-model="queryParams.unitType" placeholder="请选择单位" clearable>
          <el-option v-for="d in unitTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="资质状态" prop="qualStatus">
        <el-select v-model="queryParams.qualStatus" placeholder="请选择" clearable>
          <el-option v-for="d in qualStatusOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['homework:worker:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['homework:worker:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['homework:worker:remove']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="人员ID" align="center" prop="workerId" width="80" />
      <el-table-column label="姓名" align="center" prop="workerName" width="100" />
      <el-table-column label="角色" align="center" prop="roleType" width="130">
        <template slot-scope="scope">
          <el-tag>{{ getRoleTypeLabel(scope.row.roleType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="单位类型" align="center" prop="unitType" width="100">
        <template slot-scope="scope">
          <span>{{ getUnitTypeLabel(scope.row.unitType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="手机号" align="center" prop="phone" width="130" />
      <el-table-column label="资质状态" align="center" prop="qualStatus" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.qualStatus === '1' ? 'success' : scope.row.qualStatus === '2' ? 'danger' : 'warning'">
            {{ getQualStatusLabel(scope.row.qualStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="人脸状态" align="center" prop="faceStatus" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.faceStatus === '1' ? 'success' : 'info'">
            {{ scope.row.faceStatus === '1' ? '已注册' : '未注册' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="70">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.status" active-value="0" inactive-value="1" disabled />
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['homework:worker:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['homework:worker:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="姓名" prop="workerName">
              <el-input v-model="form.workerName" placeholder="请输入姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="请输入身份证号" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="人员角色" prop="roleType">
              <el-select v-model="form.roleType" placeholder="请选择" style="width:100%" @change="onRoleChange">
                <el-option v-for="d in roleTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单位类型" prop="unitType">
              <el-select v-model="form.unitType" placeholder="请选择" style="width:100%">
                <el-option v-for="d in unitTypeOptions" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="固定工点" prop="isFixedSite">
              <el-switch v-model="form.isFixedSite" active-value="1" inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="打卡规则" prop="checkRule">
          <el-input v-model="form.checkRule" placeholder="根据人员角色自动显示" disabled />
        </el-form-item>
        <el-form-item label="资质证件名称" prop="qualification">
          <el-input v-model="form.qualification" placeholder="请输入资质证件名称" />
        </el-form-item>
        <el-form-item label="资质证件" prop="qualFileUrl">
          <el-upload class="upload-demo" action="#" :http-request="function(){}" :file-list="qualFileList" list-type="text">
            <el-button size="small" type="primary">点击上传</el-button>
          </el-upload>
        </el-form-item>
        <el-form-item label="人脸底图" prop="faceImage">
          <el-upload class="avatar-uploader" action="#" :http-request="function(){}" :show-file-list="false">
            <img v-if="form.faceImage" :src="form.faceImage" class="avatar" style="width:100px;height:100px" />
            <i v-else class="el-icon-plus avatar-uploader-icon"></i>
          </el-upload>
        </el-form-item>
        <el-form-item label="微信OpenID" prop="openId">
          <el-input v-model="form.openId" placeholder="请输入微信公众号openid" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
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
import { listWorker, getWorker, delWorker, addWorker, updateWorker } from "@/api/homework/worker"

export default {
  name: "HwWorker",
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      workerList: [],
      title: "",
      open: false,
      qualFileList: [],
      roleTypeOptions: [
        { label: '作业申请人', value: '1' }, { label: '作业批准人', value: '2' },
        { label: '作业监护人', value: '3' }, { label: '监理人员', value: '4' },
        { label: '施工方项目经理', value: '5' }, { label: '施工方安全员', value: '6' },
        { label: '施工方现场负责人', value: '7' }, { label: '作业单位监护人', value: '8' },
        { label: '施工人员', value: '9' }
      ],
      unitTypeOptions: [
        { label: '管网', value: '1' }, { label: '第三方', value: '2' }, { label: '施工方', value: '3' }
      ],
      qualStatusOptions: [
        { label: '待审核', value: '0' }, { label: '已通过', value: '1' }, { label: '已驳回', value: '2' }
      ],
      roleCheckRuleMap: {
        '1': 'point', '2': 'point', '3': 'checkInOut,hourly', '4': 'checkInOut,hourly',
        '5': 'point', '6': 'point', '7': 'briefing,checkOut,hourly,safety', '8': 'checkInOut,hourly,safety',
        '9': 'briefing'
      },
      queryParams: { pageNum: 1, pageSize: 10, workerName: undefined, roleType: undefined, unitType: undefined, qualStatus: undefined },
      form: {},
      rules: {
        workerName: [{ required: true, message: "姓名不能为空", trigger: "blur" }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listWorker(this.queryParams).then(response => {
        this.workerList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    getRoleTypeLabel(v) {
      const f = this.roleTypeOptions.find(d => d.value === v)
      return f ? f.label : v
    },
    getUnitTypeLabel(v) {
      const f = this.unitTypeOptions.find(d => d.value === v)
      return f ? f.label : v
    },
    getQualStatusLabel(v) {
      const f = this.qualStatusOptions.find(d => d.value === v)
      return f ? f.label : v
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.qualFileList = []
      this.form = { workerId: undefined, workerName: undefined, idCard: undefined, phone: undefined, roleType: undefined, unitType: undefined, isFixedSite: '0', checkRule: undefined, qualification: undefined, qualFileUrl: undefined, qualStatus: '0', faceImage: undefined, faceStatus: '0', openId: undefined, status: "0", remark: undefined }
      this.resetForm("form")
    },
    onRoleChange(val) {
      if (val && this.roleCheckRuleMap[val]) {
        this.form.checkRule = this.roleCheckRuleMap[val]
      } else {
        this.form.checkRule = ''
      }
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
      this.ids = selection.map(item => item.workerId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加人员"
    },
    handleUpdate(row) {
      this.reset()
      const workerId = row.workerId || this.ids
      getWorker(workerId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改人员"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.workerId != undefined) {
            updateWorker(this.form).then(() => { this.$modal.msgSuccess("修改成功"); this.open = false; this.getList() })
          } else {
            addWorker(this.form).then(() => { this.$modal.msgSuccess("新增成功"); this.open = false; this.getList() })
          }
        }
      })
    },
    handleDelete(row) {
      const workerIds = row.workerId || this.ids
      this.$modal.confirm('是否确认删除人员编号为"' + workerIds + '"的数据项？').then(function() {
        return delWorker(workerIds)
      }).then(() => { this.getList(); this.$modal.msgSuccess("删除成功") }).catch(() => {})
    }
  }
}
</script>
