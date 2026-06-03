<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="模型名称" prop="modelName">
        <el-input v-model="queryParams.modelName" placeholder="请输入模型名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="模型类型" prop="modelType">
        <el-select v-model="queryParams.modelType" placeholder="模型类型" clearable>
          <el-option v-for="dict in dict.type.ai_model_type" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="模型状态" clearable>
          <el-option v-for="dict in dict.type.ai_model_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['ai:model:add']">新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate" v-hasPermi="['ai:model:edit']">修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['ai:model:remove']">删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-s-promotion" size="mini" :disabled="single" @click="handleDeploy" v-hasPermi="['ai:model:deploy']">部署</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-row :gutter="16">
      <!-- 左侧列表 -->
      <el-col :span="12">
        <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange" @row-click="handleRowClick" highlight-current-row>
          <el-table-column type="selection" width="55" align="center" />
          <el-table-column label="ID" align="center" prop="modelId" width="60" />
          <el-table-column label="模型名称" align="center" prop="modelName" :show-overflow-tooltip="true" />
          <el-table-column label="模型类型" align="center" prop="modelType" width="100">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.ai_model_type" :value="scope.row.modelType" />
            </template>
          </el-table-column>
          <el-table-column label="版本" align="center" prop="version" width="80" />
          <el-table-column label="框架" align="center" prop="framework" width="90">
            <template slot-scope="scope">
              <dict-tag :options="dict.type.ai_framework" :value="scope.row.framework" />
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.status === '0'" type="info">未部署</el-tag>
              <el-tag v-else-if="scope.row.status === '1'" type="success">已部署</el-tag>
              <el-tag v-else-if="scope.row.status === '2'" type="warning">运行中</el-tag>
              <el-tag v-else-if="scope.row.status === '3'" type="danger">异常</el-tag>
              <el-tag v-else-if="scope.row.status === '4'" type="info">已下线</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="创建时间" align="center" prop="createTime" width="160">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
      </el-col>

      <!-- 右侧详情面板 -->
      <el-col :span="12">
        <el-card v-if="currentModel" class="detail-card">
          <div slot="header" class="clearfix">
            <span style="font-weight:bold;font-size:16px;">{{ currentModel.modelName }}</span>
            <el-tag v-if="currentModel.status === '0'" type="info" style="float:right;">未部署</el-tag>
            <el-tag v-else-if="currentModel.status === '1'" type="success" style="float:right;">已部署</el-tag>
            <el-tag v-else-if="currentModel.status === '2'" type="warning" style="float:right;">运行中</el-tag>
            <el-tag v-else-if="currentModel.status === '3'" type="danger" style="float:right;">异常</el-tag>
            <el-tag v-else-if="currentModel.status === '4'" type="info" style="float:right;">已下线</el-tag>
          </div>
          <el-descriptions :column="2" size="small" border>
            <el-descriptions-item label="模型编码">{{ currentModel.modelCode }}</el-descriptions-item>
            <el-descriptions-item label="模型类型">
              <dict-tag :options="dict.type.ai_model_type" :value="currentModel.modelType" />
            </el-descriptions-item>
            <el-descriptions-item label="版本">{{ currentModel.version || '-' }}</el-descriptions-item>
            <el-descriptions-item label="AI框架">
              <dict-tag :options="dict.type.ai_framework" :value="currentModel.framework" />
            </el-descriptions-item>
            <el-descriptions-item label="提供方">{{ currentModel.provider || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ parseTime(currentModel.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{ currentModel.description || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">模型路径</el-divider>
          <div class="path-row">
            <el-input v-model="currentModel.modelPath" size="small" readonly />
            <el-button type="primary" size="mini" icon="el-icon-document-copy" @click="copyPath">复制</el-button>
          </div>

          <el-divider content-position="left">输入/输出格式</el-divider>
          <el-descriptions :column="1" size="small" border>
            <el-descriptions-item label="输入格式">{{ currentModel.inputFormat || '-' }}</el-descriptions-item>
            <el-descriptions-item label="输出格式">{{ currentModel.outputFormat || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-divider content-position="left">参数配置</el-divider>
          <el-table v-if="configTable.length > 0" :data="configTable" size="small" border>
            <el-table-column label="参数" prop="key" width="200" />
            <el-table-column label="值" prop="value" />
          </el-table>
          <span v-else style="color:#999;">{{ currentModel.configJson || '无' }}</span>

          <el-divider content-position="left">评估指标</el-divider>
          <el-table v-if="metricsTable.length > 0" :data="metricsTable" size="small" border>
            <el-table-column label="指标" prop="key" width="200" />
            <el-table-column label="值" prop="value" />
          </el-table>
          <span v-else style="color:#999;">{{ currentModel.metrics || '无' }}</span>
        </el-card>
        <el-empty v-else description="请点击左侧列表中的模型查看详情" />
      </el-col>
    </el-row>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型名称" prop="modelName">
              <el-input v-model="form.modelName" placeholder="请输入模型名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="模型编码" prop="modelCode">
              <el-input v-model="form.modelCode" placeholder="请输入模型编码" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="模型类型" prop="modelType">
              <el-select v-model="form.modelType" placeholder="请选择模型类型" style="width:100%;">
                <el-option v-for="dict in dict.type.ai_model_type" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本" prop="version">
              <el-input v-model="form.version" placeholder="如 1.0.0" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="AI框架" prop="framework">
              <el-select v-model="form.framework" placeholder="请选择AI框架" clearable style="width:100%;">
                <el-option v-for="dict in dict.type.ai_framework" :key="dict.value" :label="dict.label" :value="dict.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="提供方" prop="provider">
              <el-input v-model="form.provider" placeholder="如 内部训练" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="模型路径" prop="modelPath">
          <el-input v-model="form.modelPath" placeholder="如 /models/xxx.pt" />
        </el-form-item>
        <el-form-item label="缩略图URL" prop="thumbnail">
          <el-input v-model="form.thumbnail" placeholder="缩略图URL（可选）" />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="输入格式">
              <el-input v-model="form.inputFormat" type="textarea" :rows="2" placeholder="如 图片base64, 640x640 RGB" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="输出格式">
              <el-input v-model="form.outputFormat" type="textarea" :rows="2" placeholder="如 [{\"class\":\"person\",...}]" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="参数配置(JSON)">
          <el-input v-model="form.configJson" type="textarea" :rows="4" placeholder='{"confidence_threshold": 0.5, "input_size": "640x640"}' />
        </el-form-item>
        <el-form-item label="评估指标(JSON)">
          <el-input v-model="form.metrics" type="textarea" :rows="4" placeholder='{"mAP50": 0.892, "precision": 0.87, "fps": 45}' />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio v-for="dict in dict.type.ai_model_status" :key="dict.value" :label="dict.value">{{ dict.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="模型功能描述" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" />
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
import { listModel, getModel, addModel, updateModel, delModel, deployModel } from "@/api/ai/model"

export default {
  name: "AiModel",
  dicts: ['ai_model_type', 'ai_model_status', 'ai_framework'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      list: [],
      title: "",
      open: false,
      currentModel: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        modelName: undefined,
        modelType: undefined,
        status: undefined
      },
      form: {
        modelId: null,
        modelName: '',
        modelType: '',
        modelCode: '',
        version: '',
        framework: '',
        modelPath: '',
        configJson: '',
        metrics: '',
        inputFormat: '',
        outputFormat: '',
        provider: '',
        thumbnail: '',
        status: '0',
        description: '',
        remark: ''
      },
      rules: {
        modelName: [{ required: true, message: "模型名称不能为空", trigger: "blur" }],
        modelType: [{ required: true, message: "模型类型不能为空", trigger: "change" }],
        modelCode: [{ required: true, message: "模型编码不能为空", trigger: "blur" }]
      }
    }
  },
  computed: {
    configTable() {
      return this.parseJsonSafe(this.currentModel?.configJson)
    },
    metricsTable() {
      return this.parseJsonSafe(this.currentModel?.metrics)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listModel(this.queryParams).then(response => {
        this.list = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        modelId: null,
        modelName: '',
        modelType: '',
        modelCode: '',
        version: '',
        framework: '',
        modelPath: '',
        configJson: '',
        metrics: '',
        inputFormat: '',
        outputFormat: '',
        provider: '',
        thumbnail: '',
        status: '0',
        description: '',
        remark: ''
      }
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
      this.ids = selection.map(item => item.modelId)
      this.single = selection.length != 1
      this.multiple = !selection.length
    },
    handleRowClick(row) {
      getModel(row.modelId).then(response => {
        this.currentModel = response.data
      })
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增AI模型"
    },
    handleUpdate() {
      this.reset()
      const modelId = this.ids[0]
      getModel(modelId).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改AI模型"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.modelId != null) {
            updateModel(this.form).then(() => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addModel(this.form).then(() => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete() {
      const modelIds = this.ids.join(",")
      this.$modal.confirm('是否确认删除选中的AI模型？').then(function() {
        return delModel(modelIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    handleDeploy() {
      const modelId = this.ids[0]
      this.$modal.confirm('确认部署该模型？部署后状态将变为"已部署"。').then(function() {
        return deployModel(modelId)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("部署成功")
      }).catch(() => {})
    },
    parseJsonSafe(str) {
      if (!str) return []
      try {
        const obj = JSON.parse(str)
        return Object.entries(obj).map(([k, v]) => ({ key: k, value: typeof v === 'object' ? JSON.stringify(v) : String(v) }))
      } catch (e) {
        return []
      }
    },
    copyPath() {
      if (!this.currentModel || !this.currentModel.modelPath) return
      navigator.clipboard.writeText(this.currentModel.modelPath).then(() => {
        this.$modal.msgSuccess("路径已复制到剪贴板")
      }).catch(() => {
        this.$modal.msgError("复制失败，请手动复制")
      })
    }
  }
}
</script>

<style scoped>
.detail-card {
  min-height: 400px;
}
.path-row {
  display: flex;
  gap: 8px;
}
.path-row .el-input {
  flex: 1;
}
</style>
