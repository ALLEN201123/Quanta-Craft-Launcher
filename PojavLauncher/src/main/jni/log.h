#ifdef __ANDROID__
#include <android/log.h>

#define TAG "jrelog"
#endif

#ifdef __cplusplus
extern "C" {
#endif

#define LOGE(...) __android_log_print(ANDROID_LOG_INFO,    TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_SILENT,    TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_SILENT,    TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_INFO,    TAG, __VA_ARGS__)

// ===== 1.1.0 渲染桥移植（来自 FCL 的 log.h）=====
// FCL 渲染桥代码统一使用 FCL_LOG / FCL_INTERNAL_LOG 两个宏。
// 这里映射到 __android_log_print（TAG=jrelog）——正好被 QCL 启动日志窗抓取，
// 便于真机诊断渲染初始化过程。原 FCL 实现是 printf（走 stdout 重定向）。
#define FCL_INTERNAL_LOG(x...) do { \
    __android_log_print(ANDROID_LOG_INFO, TAG, "[FCL Internal] %s:%d", __FILE__, __LINE__); \
    __android_log_print(ANDROID_LOG_INFO, TAG, x); \
    } while (0)

#define FCL_LOG(x...) __android_log_print(ANDROID_LOG_INFO, TAG, x)

#ifdef __cplusplus
}
#endif

