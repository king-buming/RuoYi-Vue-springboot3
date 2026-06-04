<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="角色编码" prop="roleCode">
        <el-input
          v-model="queryParams.roleCode"
          placeholder="请输入角色编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="角色名称" prop="roleName">
        <el-input
          v-model="queryParams.roleName"
          placeholder="请输入角色名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择" clearable>
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
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
          v-hasPermi="['worker:role:add']"
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
          v-hasPermi="['worker:role:edit']"
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
          v-hasPermi="['worker:role:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['worker:role:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="roleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="规则ID" align="center" prop="id" />
      <el-table-column label="角色编码" align="center" prop="roleCode" />
      <el-table-column label="角色名称" align="center" prop="roleName" />
      <el-table-column label="单位类型" align="center" prop="unitType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.worker_unit_type" :value="scope.row.unitType"/>
        </template>
      </el-table-column>
      <el-table-column label="签到" align="center" prop="needSignIn">
        <template slot-scope="scope">
          <span>{{ scope.row.needSignIn === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="点到" align="center" prop="needHourlyCheck">
        <template slot-scope="scope">
          <span>{{ scope.row.needHourlyCheck === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="自主打卡" align="center" prop="isSelfCheckin">
        <template slot-scope="scope">
          <span>{{ scope.row.isSelfCheckin === '0' ? '否' : '是' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="需要资质" align="center" prop="needCert">
        <template slot-scope="scope">
          <span>{{ scope.row.needCert === '1' ? '是' : '否' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.status"/>
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
            v-hasPermi="['worker:role:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['worker:role:remove']"
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

    <!-- 添加或修改角色规则对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="角色编码" prop="roleCode">
          <el-input v-model="form.roleCode" placeholder="请输入角色编码" />
        </el-form-item>
        <el-form-item label="角色名称" prop="roleName">
          <el-input v-model="form.roleName" placeholder="请输入角色名称" />
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
        <el-form-item label="固定工点" prop="fixedSiteFlag">
          <el-radio-group v-model="form.fixedSiteFlag">
            <el-radio label="0">否</el-radio>
            <el-radio label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="需要签到" prop="needSignIn">
          <el-radio-group v-model="form.needSignIn">
            <el-radio label="0">否</el-radio>
            <el-radio label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="需要签退" prop="needSignOut">
          <el-radio-group v-model="form.needSignOut">
            <el-radio label="0">否</el-radio>
            <el-radio label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="需要点到" prop="needHourlyCheck">
          <el-radio-group v-model="form.needHourlyCheck">
            <el-radio label="0">否</el-radio>
            <el-radio label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="点到间隔(分钟)" prop="hourlyInterval" v-if="form.needHourlyCheck === '1'">
          <el-input-number v-model="form.hourlyInterval" controls-position="right" :min="1" />
        </el-form-item>
        <el-form-item label="自主打卡" prop="isSelfCheckin">
          <el-radio-group v-model="form.isSelfCheckin">
            <el-radio label="1">是</el-radio>
            <el-radio label="0">否（跟随班前喊话）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="需要资质" prop="needCert">
          <el-radio-group v-model="form.needCert">
            <el-radio label="0">否</el-radio>
            <el-radio label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="所需资质类型" prop="certType" v-if="form.needCert === '1'">
          <el-select v-model="form.certType" placeholder="请选择资质类型">
            <el-option
              v-for="dict in dict.type.worker_cert_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.value"
            >{{dict.label}}</el-radio>
          </el-radio-group>
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
import { listRole, getRole, delRole, addRole, updateRole } from "@/api/worker/role"

export default {
  name: "WorkerRole",
  dicts: ['worker_unit_type', 'worker_cert_type', 'sys_normal_disable'],
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
      // 角色规则表格数据
      roleList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        roleCode: undefined,
        roleName: undefined,
        unitType: undefined,
        status: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        roleCode: [
          { required: true, message: "角色编码不能为空", trigger: "blur" }
        ],
        roleName: [
          { required: true, message: "角色名称不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created() { this.getList() },
  activated() { this.getList() },
  methods: {
    /** 查询角色规则列表 */
    getList() {
      this.loading = true
      listRole(this.queryParams).then(response => {
        this.roleList = response.rows
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
        roleCode: undefined,
        roleName: undefined,
        unitType: undefined,
        fixedSiteFlag: "0",
        needSignIn: "0",
        needSignOut: "0",
        needHourlyCheck: "0",
        hourlyInterval: undefined,
        isSelfCheckin: "1",
        needCert: "0",
        certType: undefined,
        status: "0",
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
      this.title = "添加角色规则"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getRole(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改角色规则"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != undefined) {
            updateRole(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addRole(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除角色规则编号为"' + ids + '"的数据项？').then(function() {
        return delRole(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('worker/role/export', {
        ...this.queryParams
      }, `role_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
