<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="人员" prop="workerId">
        <el-select v-model="queryParams.workerId" placeholder="请选择人员" clearable filterable @change="handleQuery">
          <el-option
            v-for="w in activeOptions"
            :key="w.id"
            :label="w.workerName + ' (ID:' + w.id + ')'"
            :value="w.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核结果" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" placeholder="请选择" clearable>
          <el-option
            v-for="dict in dict.type.worker_audit_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['worker:audit:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['worker:audit:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['worker:audit:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['worker:audit:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="auditList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="关联人员" align="center" prop="workerId">
        <template slot-scope="scope">
          {{ workerName(scope.row.workerId) }} (ID:{{ scope.row.workerId }})
        </template>
      </el-table-column>
      <el-table-column label="审核结果" align="center" prop="auditStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.worker_audit_status" :value="scope.row.auditStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="审核意见" align="center" prop="auditOpinion" :show-overflow-tooltip="true" />
      <el-table-column label="审核人" align="center" prop="auditor" />
      <el-table-column label="审核时间" align="center" prop="auditTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.auditTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['worker:audit:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['worker:audit:remove']"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改审核记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="关联人员" prop="workerId">
          <el-select v-model="form.workerId" placeholder="请选择人员" filterable>
            <el-option
              v-for="w in activeOptions"
              :key="w.id"
              :label="w.workerName + ' (ID:' + w.id + ')'"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <!-- 展示人脸 -->
        <el-form-item label="人脸照片" v-if="form.workerId">
          <image-preview v-if="faceImg" :src="faceImg" :width="120" :height="120"/>
          <span v-else style="color:#999;font-size:13px">该人员尚未录入人脸</span>
        </el-form-item>
        <!-- 展示身份证 -->
        <el-form-item label="身份证照片" v-if="form.workerId">
          <div style="display:flex;gap:8px;flex-wrap:wrap" v-if="idCardImgs.length">
            <div v-for="(img,i) in idCardImgs" :key="i" style="text-align:center">
              <image-preview :src="img.url" :width="120" :height="80"/>
              <div style="font-size:12px;color:#999">{{ img.remark }}</div>
            </div>
          </div>
          <span v-else style="color:#999;font-size:13px">该人员尚未上传身份证</span>
        </el-form-item>
        <!-- 资质证书列表 -->
        <el-form-item label="资质证书" v-if="form.workerId && workerCerts.length">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <div v-for="c in workerCerts" :key="c.id" style="text-align:center;border:1px solid #eee;border-radius:8px;padding:8px">
              <image-preview v-if="c.certImg" :src="c.certImg" :width="80" :height="55"/>
              <div style="font-size:12px;margin-top:4px">{{ certName(c.certType) }}</div>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="资质证书" v-if="form.workerId && !workerCerts.length">
          <span v-if="workerNeedCerts" style="color:#ff9500;font-size:13px">⚠ 该人员尚未上传资质证书</span>
          <span v-else style="color:#999;font-size:13px">该人员所属角色无需资质证书</span>
        </el-form-item>
        <el-form-item label="审核结果" prop="auditStatus">
          <el-select v-model="form.auditStatus" placeholder="请选择审核结果">
            <el-option
              v-for="dict in dict.type.worker_audit_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="审核意见" prop="auditOpinion">
          <el-input v-model="form.auditOpinion" type="textarea" placeholder="审核意见 / 驳回原因" />
        </el-form-item>
        <el-form-item label="审核人" prop="auditor">
          <el-input v-model="form.auditor" placeholder="请输入审核人" />
        </el-form-item>
        <el-form-item label="审核时间" prop="auditTime">
          <el-date-picker clearable
            v-model="form.auditTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="请选择审核时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
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
import { listAudit, getAudit, delAudit, addAudit, updateAudit } from "@/api/worker/audit"
import { listWorkerOptions, listActiveWorkerOptions, getWorkerCerts, getWorkerFaces, getWorkerRoles } from "@/api/worker/worker"
import { listRole } from "@/api/worker/role"

export default {
  name: "WorkerAudit",
  dicts: ['worker_audit_status'],
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 审核记录表格数据
      auditList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 人员下拉选项
      workerOptions: [],
      activeOptions: [],
      allRoles: [],
      certMap: {},
      certOptions: [],
      selectedCertImg: '',
      idCardImgs: [],
      workerCerts: [],
      workerNeedCerts: false,
      faceImg: '',
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workerId: undefined,
        auditStatus: undefined
      },
      // 表单参数
      form: { bizType: 'worker' },
      // 表单校验
      rules: {
        auditStatus: [
          { required: true, message: "审核结果不能为空", trigger: "change" }
        ]
      }
    }
  },
  created() {
    this.getList()
    listWorkerOptions().then(r => { this.workerOptions = r.data })
    listActiveWorkerOptions().then(r => { this.activeOptions = r.data })
    listRole({pageNum:1,pageSize:999}).then(r => { this.allRoles = r.rows || [] })
    // 加载证件类型字典
    fetch('http://localhost:8080/app/common/dicts?types=worker_cert_type').then(r=>r.json()).then(d=>{
      if(d.code===200){(d.data['worker_cert_type']||[]).forEach(o=>{this.certMap[o.value]=o.label})}
    })
  },
  watch: {
    'form.workerId': function(id) {
      this.selectedCertImg = ''; this.idCardImgs = []; this.faceImg = ''
      if (!id) return
      getWorkerFaces(id).then(r => { this.faceImg = r.data && r.data.length ? r.data[0].faceImgUrl : '' })
      getWorkerRoles(id).then(r => {
        const roleIds = r.data || []
        this.workerNeedCerts = this.allRoles.some(rl => roleIds.includes(rl.id) && rl.needCert === '1')
      })
      getWorkerCerts(id).then(r => {
        const all = r.data || []
        this.idCardImgs = all.filter(c => c.certType === 'id_card' && c.certImg)
          .map(c => ({url: c.certImg, remark: c.remark || ''}))
        this.workerCerts = all.filter(c => c.certType !== 'id_card' && c.certImg)
      })
    }
  },
  activated() { this.getList(); listWorkerOptions().then(response => { this.workerOptions = response.data }) },
  methods: {
    /** 查询审核记录列表 */
    getList() {
      this.loading = true
      listAudit(this.queryParams).then(response => {
        this.auditList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        bizType: 'worker',
        bizId: undefined,
        workerId: undefined,
        auditStatus: undefined,
        auditOpinion: undefined,
        auditor: undefined,
        auditTime: undefined,
        remark: undefined
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加审核记录"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getAudit(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改审核记录"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != undefined) {
            updateAudit(this.form).then(res => {
              this.$modal.msgSuccess("修改成功")
              if (res.warnings && res.warnings.length) this.$modal.msgWarning("缺少资质：" + res.warnings.join("；"))
              this.open = false; this.getList()
            })
          } else {
            addAudit(this.form).then(res => {
              this.$modal.msgSuccess("新增成功")
              if (res.warnings && res.warnings.length) this.$modal.msgWarning("缺少资质：" + res.warnings.join("；"))
              this.open = false; this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除审核记录编号为"' + ids + '"的数据项？').then(function() {
        return delAudit(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    workerName(id) {
      const w = this.workerOptions.find(o => o.id === id)
      if (!w) return 'ID:' + (id || '')
      return w.delFlag === '2' ? w.workerName + '(已归档)' : w.workerName
    },
    certName(type) { return this.certMap[type] || type },
    onCertSelect(id) {
      const c = this.certOptions.find(o => o.id === id)
      this.selectedCertImg = c ? c.certImg || '' : ''
    },
    handleExport() {
      this.download('worker/audit/export', {
        ...this.queryParams
      }, `audit_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
