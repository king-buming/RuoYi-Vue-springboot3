<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="姓名" prop="workerName">
        <el-input
          v-model="queryParams.workerName"
          placeholder="请输入姓名"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input
          v-model="queryParams.phone"
          placeholder="请输入手机号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="人员状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option
            v-for="dict in dict.type.worker_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="审核状态" prop="auditStatus">
        <el-select v-model="queryParams.auditStatus" placeholder="请选择" clearable>
          <el-option
            v-for="dict in dict.type.worker_audit_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="单位类型" prop="unitType">
        <el-select v-model="queryParams.unitType" placeholder="请选择" clearable>
          <el-option
            v-for="dict in dict.type.worker_unit_type"
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
          v-hasPermi="['worker:worker:add']"
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
          v-hasPermi="['worker:worker:edit']"
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
          v-hasPermi="['worker:worker:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['worker:worker:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="workerList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="人员ID" align="center" prop="id" />
      <el-table-column label="姓名" align="center" prop="workerName" />
      <el-table-column label="手机号" align="center" prop="phone" />
      <el-table-column label="身份证号" align="center" prop="idCard" />
      <el-table-column label="性别" align="center" prop="gender">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_user_sex" :value="scope.row.gender"/>
        </template>
      </el-table-column>
      <el-table-column label="单位类型" align="center" prop="unitType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.worker_unit_type" :value="scope.row.unitType"/>
        </template>
      </el-table-column>
      <el-table-column label="角色" align="center" prop="id">
        <template slot-scope="scope">
          {{ (roleMap[scope.row.id] || []).join('、') || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="人员状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.worker_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="人脸录入" align="center" prop="faceStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.worker_face_status" :value="scope.row.faceStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="审核状态" align="center" prop="auditStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.worker_audit_status" :value="scope.row.auditStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['worker:worker:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['worker:worker:remove']"
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

    <!-- 添加或修改人员档案对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="750px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="姓名" prop="workerName">
          <el-input v-model="form.workerName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="身份证号" prop="idCard">
          <el-input v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" @blur="autoFillGender" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="form.gender" placeholder="请选择性别">
            <el-option
              v-for="dict in dict.type.sys_user_sex"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="单位类型" prop="unitType">
          <el-select v-model="form.unitType" placeholder="请选择单位类型">
            <el-option
              v-for="dict in dict.type.worker_unit_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="人员状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.worker_status"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="分配角色" prop="roleIds">
          <el-select v-model="roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="r in roleList"
              :key="r.id"
              :label="r.roleName"
              :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容" />
        </el-form-item>
      </el-form>

      <!-- 关联数据（仅编辑时有） -->
      <template v-if="form.id">
        <el-divider content-position="left">关联数据</el-divider>
        <el-tabs type="border-card" size="small">
          <el-tab-pane :label="'资质证件 (' + relatedCerts.length + ')'">
            <el-table :data="relatedCerts" size="small" max-height="180">
              <el-table-column label="类型" prop="certType">
                <template slot-scope="s"><dict-tag :options="dict.type.worker_cert_type" :value="s.row.certType"/></template>
              </el-table-column>
              <el-table-column label="编号" prop="certNo" />
              <el-table-column label="到期日" prop="expireDate" width="100" />
              <el-table-column label="审核" prop="auditStatus">
                <template slot-scope="s"><dict-tag :options="dict.type.worker_audit_status" :value="s.row.auditStatus"/></template>
              </el-table-column>
            </el-table>
            <div v-if="relatedCerts.length===0" style="color:#999;text-align:center;padding:10px">暂无</div>
          </el-tab-pane>
          <el-tab-pane :label="'人脸信息 (' + relatedFaces.length + ')'">
            <el-table :data="relatedFaces" size="small" max-height="180">
              <el-table-column label="照片" prop="faceImgUrl" width="80">
                <template slot-scope="s"><image-preview :src="s.row.faceImgUrl" :width="40" :height="40"/></template>
              </el-table-column>
              <el-table-column label="采集时间" prop="collectTime" width="160">
                <template slot-scope="s">{{ parseTime(s.row.collectTime) }}</template>
              </el-table-column>
            </el-table>
            <div v-if="relatedFaces.length===0" style="color:#999;text-align:center;padding:10px">暂无</div>
          </el-tab-pane>
          <el-tab-pane :label="'审核记录 (' + relatedAudits.length + ')'">
            <el-table :data="relatedAudits" size="small" max-height="180">
              <el-table-column label="业务" prop="bizType" width="60">
                <template slot-scope="s">{{ s.row.bizType==='worker'?'人员':'资质' }}</template>
              </el-table-column>
              <el-table-column label="结果" prop="auditStatus">
                <template slot-scope="s"><dict-tag :options="dict.type.worker_audit_status" :value="s.row.auditStatus"/></template>
              </el-table-column>
              <el-table-column label="意见" prop="auditOpinion" :show-overflow-tooltip="true" />
              <el-table-column label="审核时间" prop="auditTime" width="160">
                <template slot-scope="s">{{ parseTime(s.row.auditTime) }}</template>
              </el-table-column>
            </el-table>
            <div v-if="relatedAudits.length===0" style="color:#999;text-align:center;padding:10px">暂无</div>
          </el-tab-pane>
        </el-tabs>
      </template>

      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listWorker, getWorker, delWorker, addWorker, updateWorker, getWorkerRoles, saveWorkerRoles, getWorkerCerts, getWorkerFaces, getWorkerAudits, getAllRoleNames } from "@/api/worker/worker"
import { listRole } from "@/api/worker/role"

export default {
  name: "Worker",
  dicts: ['sys_user_sex', 'worker_status', 'worker_face_status', 'worker_audit_status', 'worker_cert_type', 'worker_unit_type'],
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
      // 人员档案表格数据
      workerList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workerName: undefined,
        phone: undefined,
        status: undefined,
        auditStatus: undefined,
        unitType: undefined
      },
      // 表单参数
      form: {},
      // 角色列表（所有可选角色）
      roleList: [],
      // 当前选中角色 ID
      roleIds: [],
      // 角色映射 { workerId: [roleName, ...] }
      roleMap: {},
      // 关联数据（编辑时加载）
      relatedCerts: [],
      relatedFaces: [],
      relatedAudits: [],
      // 表单校验
      rules: {
        workerName: [
          { required: true, message: "姓名不能为空", trigger: "blur" }
        ],
        phone: [
          { required: true, message: "手机号不能为空", trigger: "blur" },
          { pattern: /^1[3|4|5|6|7|8|9][0-9]\d{8}$/, message: "请输入正确的手机号", trigger: "blur" }
        ],
        idCard: [
          { required: true, message: "身份证号不能为空", trigger: "blur" },
          { pattern: /^[1-9]\d{5}(18|19|20)\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\d|3[01])\d{3}[0-9Xx]$/, message: "请输入正确的身份证号", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.initPage()
  },
  activated() {
    this.initPage()
  },
  methods: {
    initPage() {
      this.getList()
      this.refreshRoleMap()
      if (this.roleList.length === 0) {
        listRole({ pageNum: 1, pageSize: 999 }).then(response => {
          this.roleList = response.rows
        })
      }
    },
    refreshRoleMap() {
      getAllRoleNames().then(response => {
        const map = {}
        ;(response.data || []).forEach(r => {
          if (!map[r.worker_id]) map[r.worker_id] = []
          map[r.worker_id].push(r.roleName)
        })
        this.roleMap = map
      })
    },
    /** 查询人员档案列表 */
    getList() {
      this.loading = true
      listWorker(this.queryParams).then(response => {
        this.workerList = response.rows
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
        workerName: undefined,
        phone: undefined,
        idCard: undefined,
        gender: "0",
        unitType: undefined,
        status: "0",
        remark: undefined
      }
      this.roleIds = []
      this.relatedCerts = []
      this.relatedFaces = []
      this.relatedAudits = []
      this.resetForm("form")
    },
    /** 输完身份证号自动推断性别 */
    autoFillGender() {
      const id = this.form.idCard
      if (id && id.length === 18) {
        const genderCode = parseInt(id.charAt(16))
        this.form.gender = genderCode % 2 === 1 ? '0' : '1'
      }
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
      this.title = "添加人员档案"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getWorker(id).then(response => {
        this.form = response.data
        getWorkerRoles(id).then(res => {
          this.roleIds = res.data
        })
        // 加载关联数据
        getWorkerCerts(id).then(res => { this.relatedCerts = res.data })
        getWorkerFaces(id).then(res => { this.relatedFaces = res.data })
        getWorkerAudits(id).then(res => { this.relatedAudits = res.data })
        this.open = true
        this.title = "修改人员档案"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != undefined) {
            updateWorker(this.form).then(() => {
              return saveWorkerRoles(this.form.id, this.roleIds)
            }).then(res => {
              this.$modal.msgSuccess("修改成功")
              if (res.warnings && res.warnings.length) this.$modal.msgWarning("缺少资质：" + res.warnings.join("；"))
              this.open = false
              this.getList()
              this.refreshRoleMap()
            })
          } else {
            addWorker(this.form).then(response => {
              const workerId = response.data
              return saveWorkerRoles(workerId, this.roleIds)
            }).then(res => {
              this.$modal.msgSuccess("新增成功")
              if (res.warnings && res.warnings.length) this.$modal.msgWarning("缺少资质：" + res.warnings.join("；"))
              this.open = false
              this.getList()
              this.refreshRoleMap()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除人员档案编号为"' + ids + '"的数据项？').then(function() {
        return delWorker(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('worker/worker/export', {
        ...this.queryParams
      }, `worker_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
