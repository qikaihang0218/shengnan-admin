<template>
  <div class="login-page">
    <div class="login-box">
      <div class="brand">
        <div class="logo"><LeafLogo /></div>
        <h1>胜楠管理平台</h1>
        <p>SHENGNAN ADMIN PLATFORM</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="请输入账号" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large"
                    prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="form.captchaCode" placeholder="验证码" size="large" prefix-icon="Key" />
            <img :src="captcha.image" class="captcha-img" @click="loadCaptcha" title="点击刷新" />
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登 录
        </el-button>
      </el-form>
      <div class="hint">默认账号 admin / admin123</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../store/user'
import LeafLogo from '../../components/LeafLogo.vue'

const router = useRouter()
const store = useUserStore()
const formRef = ref()
const loading = ref(false)
const captcha = reactive({ captchaId: '', image: '' })
const form = reactive({ username: '', password: '', captchaCode: '' })
const rules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function loadCaptcha() {
  const res = await request.get('/auth/captcha')
  captcha.captchaId = res.data.captchaId
  captcha.image = res.data.image
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await store.login(form.username, form.password, captcha.captchaId, form.captchaCode)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (e) {
    loadCaptcha()
    form.captchaCode = ''
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--sn-brand) 0%, var(--sn-brand-2) 100%);
}
.login-box {
  width: 400px;
  background: var(--el-bg-color);
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 20px 60px rgba(0,0,0,.3);
}
.brand { text-align: center; margin-bottom: 30px; }
.logo {
  width: 56px; height: 56px; padding: 10px;
  margin: 0 auto 12px;
  border-radius: 14px;
  background: linear-gradient(135deg, var(--sn-brand), var(--sn-brand-2));
  color: #fff;
}
.brand h1 { font-size: 22px; color: var(--sn-logo-color); }
.brand p { font-size: 10px; color: var(--el-text-color-secondary); letter-spacing: 2px; margin-top: 4px; }
.captcha-row { display: flex; gap: 10px; width: 100%; }
.captcha-img {
  height: 40px; width: 120px; cursor: pointer;
  border: 1px solid var(--el-border-color); border-radius: 4px;
}
.login-btn { width: 100%; margin-top: 8px; }
.hint { text-align: center; color: var(--el-text-color-placeholder); font-size: 12px; margin-top: 20px; }
</style>
