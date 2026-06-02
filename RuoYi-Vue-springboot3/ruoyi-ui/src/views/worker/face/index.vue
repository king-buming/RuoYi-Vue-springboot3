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
          v-hasPermi="['worker:face:add']"
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
          v-hasPermi="['worker:face:edit']"
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
          v-hasPermi="['worker:face:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['worker:face:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="faceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="人员" align="center" prop="workerId">
        <template slot-scope="scope">
          {{ workerName(scope.row.workerId) }} (ID:{{ scope.row.workerId }})
        </template>
      </el-table-column>
      <el-table-column label="人脸照片" align="center" prop="faceImgUrl" width="100">
        <template slot-scope="scope">
          <image-preview :src="scope.row.faceImgUrl" :width="50" :height="50"/>
        </template>
      </el-table-column>
      <el-table-column label="采集时间" align="center" prop="collectTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.collectTime) }}</span>
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
            v-hasPermi="['worker:face:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['worker:face:remove']"
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

    <!-- 添加或修改人脸信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="人员" prop="workerId">
          <el-select v-model="form.workerId" placeholder="请选择人员" filterable>
            <el-option
              v-for="w in activeOptions"
              :key="w.id"
              :label="w.workerName + ' (ID:' + w.id + ')'"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="人脸照片" prop="faceImgUrl">
          <image-upload v-model="form.faceImgUrl" :limit="1"/>
        </el-form-item>
        <el-form-item label="采集时间" prop="collectTime">
          <el-date-picker clearable
            v-model="form.collectTime"
            type="datetime"
            value-format="yyyy-MM-dd HH:mm:ss"
            placeholder="请选择采集时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="人脸特征值" prop="faceFeature">
          <el-input v-model="form.faceFeature" type="textarea" placeholder="当前阶段可留空，后续 AI 生成" />
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
import { listFace, getFace, delFace, addFace, updateFace } from "@/api/worker/face"
import { listWorkerOptions, listActiveWorkerOptions } from "@/api/worker/worker"

export default {
  name: "WorkerFace",
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
      // 人脸信息表格数据
      faceList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 人员下拉选项
      workerOptions: [],
      activeOptions: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workerId: undefined
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        workerId: [
          { required: true, message: "人员ID不能为空", trigger: "blur" }
        ]
      }
    }
  },
  created() {
    this.getList()
    listWorkerOptions().then(r => { this.workerOptions = r.data })
    listActiveWorkerOptions().then(r => { this.activeOptions = r.data })
  },
  activated() { this.getList(); listWorkerOptions().then(response => { this.workerOptions = response.data }) },
  methods: {
    /** 查询人脸信息列表 */
    getList() {
      this.loading = true
      listFace(this.queryParams).then(response => {
        this.faceList = response.rows
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
        workerId: undefined,
        faceImgUrl: undefined,
        faceFeature: undefined,
        collectTime: undefined,
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
      this.title = "添加人脸信息"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getFace(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改人脸信息"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != undefined) {
            updateFace(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addFace(this.form).then(() => {
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
      this.$modal.confirm('是否确认删除人脸信息编号为"' + ids + '"的数据项？').then(function() {
        return delFace(ids)
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
    handleExport() {
      this.download('worker/face/export', {
        ...this.queryParams
      }, `face_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
