import axios, { type AxiosError, type AxiosRequestConfig } from "axios";
import type { ApiResponse, PageResponse } from "@/types/api";
import { message } from "@/utils/message";
import { useUserStore } from "@/stores/user";
import { isCurrentSessionRequest } from "@/utils/session";

// 创建 Axios 实例
const instance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL?.trim() || '/api',
    timeout: 15000, // 请求超时时间
});



// 添加请求拦截器
instance.interceptors.request.use(function (config) {
    // 从 localStorage 获取 token
    const userStore = useUserStore();
    const token = userStore.token;
    
    // 如果存在 token，则添加到请求头
    if (token) {
        // 添加请求头, key 为 Authorization，value 值的前缀为 'Bearer '
        config.headers['Authorization'] = `Bearer ${token}`;
    }

    return config;
}, function (error) {
    // 对请求错误做些什么
    return Promise.reject(error);
});

// 添加响应拦截器
instance.interceptors.response.use(function (response) {
    // 2xx 范围内的状态码都会触发该函数。
    // 对响应数据做点什么
    // 个人资料接口允许游客访问；请求自身资料时，失效登录态以业务错误返回。
    if (response.data?.success === false && response.data?.errorCode === 'USER-200012') {
        const userStore = useUserStore()
        const requestAuthorization = response.config.headers.get('Authorization')
        if (userStore.token && isCurrentSessionRequest(requestAuthorization, userStore.token)) {
            userStore.logout()
            message.show('登录状态已失效，请重新登录')
            window.dispatchEvent(new Event('shrimp:login-required'))
        }
    }
    return response.data;
}, function (error: AxiosError<{ message?: string }>) {
    const status = error.response?.status
    if (status === 401) {
        const userStore = useUserStore()
        const requestAuthorization = error.config?.headers.get('Authorization')
        // 迟到的旧会话响应不能清除已经更新的登录状态。
        if (!isCurrentSessionRequest(requestAuthorization, userStore.token)) {
            return Promise.reject(error)
        }
        userStore.logout()
        message.show('登录状态已失效，请重新登录')
        window.dispatchEvent(new Event('shrimp:login-required'))
    } else {
        message.show(error.response?.data?.message || '请求失败')
    }
    return Promise.reject(error)

});

// 暴露出去
export default instance;

// 响应拦截器返回 response.data，因此这里显式表达实际返回值。
export function post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
    return instance.post<T, ApiResponse<T>>(url, data, config)
}

export function postPage<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<PageResponse<T>> {
    return instance.post<T, PageResponse<T>>(url, data, config)
}
