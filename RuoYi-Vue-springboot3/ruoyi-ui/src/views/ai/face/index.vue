<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="人员姓名" prop="workerName">
        <el-input v-model="queryParams.workerName" placeholder="请输入人员姓名" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="录入状态" prop="registerStatus">
        <el-select v-model="queryParams.registerStatus" placeholder="录入状态" clearable>
          <el-option v-for="dict in dict.type.ai_face_register_status" :key="dict.value" :label="dict.label" :value="dict.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['ai:face:cancel']">删除记录</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert type="info" :closable="false" show-icon style="margin-bottom:12px;">
      <template slot="title">
        数据来源：人员管理模块（tb_worker + tb_worker_face）。点击<strong>"录入"</strong>按钮将人员人脸注册到AI识别模型底库，注册后该人员可被AI人脸匹配识别。当前AI推理服务为桩实现，录入时提示"AI推理服务尚未配置"为桩行为，部署真实模型后即可正常使用。
      </template>
    </el-alert>

    <el-table v-loading="loading" :data="list" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="人员ID" align="center" prop="workerId" width="80" />
      <el-table-column label="姓名" align="center" prop="workerName" width="100" />
      <el-table-column label="人脸照片" align="center" prop="faceImgUrl" width="120">
        <template slot-scope="scope">
          <el-image v-if="scope.row.faceImgUrl" :src="scope.row.faceImgUrl" :preview-src-list="[scope.row.faceImgUrl]" style="width:60px;height:60px;border-radius:4px;" fit="cover" />
          <el-tag v-else type="info" size="small">无照片</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="人脸采集时间" align="center" prop="collectTime" width="160">
        <template slot-scope="scope">
          <span>{{ scope.row.collectTime ? parseTime(scope.row.collectTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="AI录入状态" align="center" prop="registerStatus" width="110">
        <template slot-scope="scope">
          <dict-tag v-if="scope.row.registerStatus" :options="dict.type.ai_face_register_status" :value="scope.row.registerStatus" />
          <el-tag v-else type="info">未录入</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关联模型" align="center" prop="modelName" :show-overflow-tooltip="true" width="180">
        <template slot-scope="scope">
          {{ scope.row.modelName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="录入时间" align="center" prop="registerTime" width="160">
        <template slot-scope="scope">
          <span>{{ scope.row.registerTime ? parseTime(scope.row.registerTime) : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="失败原因" align="center" prop="failReason" :show-overflow-tooltip="true" width="180">
        <template slot-scope="scope">
          <span style="color:#f56c6c;">{{ scope.row.failReason || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="200" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <!-- 未录入状态：显示"录入"按钮 -->
          <el-button
            v-if="scope.row.registerStatus !== '1'"
            size="mini" type="primary" icon="el-icon-upload2"
            @click="handleRegister(scope.row)"
            v-hasPermi="['ai:face:register']"
          >录入</el-button>

          <!-- 已录入状态：显示"取消注册"按钮 -->
          <el-button
            v-if="scope.row.registerStatus === '1'"
            size="mini" type="warning" icon="el-icon-refresh-left"
            @click="handleCancelRegister(scope.row)"
            v-hasPermi="['ai:face:cancel']"
          >取消注册</el-button>

          <!-- 录入失败状态：显示"重试"按钮 -->
          <el-button
            v-if="scope.row.registerStatus === '2'"
            size="mini" type="primary" icon="el-icon-refresh"
            @click="handleRegister(scope.row)"
            v-hasPermi="['ai:face:register']"
          >重试</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listFaceRegister, registerFace, batchRegister, cancelRegister, delFaceRegister } from "@/api/ai/face"

export default {
  name: "AiFace",
  dicts: ['ai_face_register_status'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      list: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        workerName: undefined,
        registerStatus: undefined
      },
      batchForm: {
        workerIds: [],
        modelCode: 'arcface_r100_001'
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listFaceRegister(this.queryParams).then(response => {
        this.list = response.rows
        this.total = response.total
        this.loading = false
      })
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
      this.ids = selection.map(item => item.registerId)
      this.single = selection.length != 1
      this.multiple = !selection.length
      this.batchForm.workerIds = selection
        .filter(item => item.registerStatus !== '1')
        .map(item => item.workerId)
    },
    /** 单个录入 */
    handleRegister(row) {
      // 前端的预检查：无人脸照片时直接提示，不发起请求
      if (!row.faceImgUrl) {
        this.$message.warning("该人员尚未上传人脸照片，请先在人员管理模块完成人脸采集")
        return
      }
      const modelCode = this.batchForm.modelCode
      this.$confirm('确认将 <strong>' + row.workerName + '</strong> 的人脸录入到AI识别模型？<br/>录入后该人员可被AI人脸匹配识别。', '确认录入', {
        dangerouslyUseHTMLString: true,
        type: 'info'
      }).then(() => {
        return registerFace(row.workerId, modelCode)
      }).then(response => {
        this.$message.success(response.msg || "人脸录入成功")
        this.getList()
      }).catch(err => {
        if (err !== 'cancel') {
          this.$message.error(err.msg || "录入失败")
          this.getList()
        }
      })
    },
    /** 批量录入（选中行中没有已录入的） */
    handleBatchRegister() {
      const selectedRows = this.ids.filter(item => item.registerStatus !== '1')
      if (selectedRows.length === 0) {
        this.$message.warning("请选择未录入状态的人员")
        return
      }
      this.$confirm('确认批量录入选中的人员人脸到AI识别模型？', '批量录入', { type: 'info' }).then(() => {
        return batchRegister({
          workerIds: selectedRows.map(item => item.workerId),
          modelCode: this.batchForm.modelCode
        })
      }).then(response => {
        this.$message.success(response.msg || "批量录入完成")
        this.getList()
      }).catch(err => {
        if (err !== 'cancel') {
          this.$message.error(err.msg || "批量录入失败")
          this.getList()
        }
      })
    },
    /** 取消注册 */
    handleCancelRegister(row) {
      this.$confirm('确认取消 <strong>' + row.workerName + '</strong> 的人脸注册？取消后该人员将无法被AI人脸匹配识别。', '确认取消', {
        dangerouslyUseHTMLString: true,
        type: 'warning'
      }).then(() => {
        return cancelRegister(row.registerId)
      }).then(() => {
        this.$message.success("取消注册成功")
        this.getList()
      }).catch(err => {
        if (err !== 'cancel') {
          this.$message.error(err.msg || "取消注册失败")
        }
      })
    },
    /** 删除记录 */
    handleDelete() {
      const registerIds = this.ids.join(",")
      this.$modal.confirm('是否确认删除选中的注册记录？').then(function() {
        return delFaceRegister(registerIds)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    }
  }
}
</script>
