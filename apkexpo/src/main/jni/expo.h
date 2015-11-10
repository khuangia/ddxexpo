// khuangia@gmail.com

#ifndef APKEXPO_EXPO_H
#define APKEXPO_EXPO_H

#include <jni.h>
#include <android/log.h>
#include <sys/types.h>

#define MAX_PATH 260

#ifndef NULL
#define NULL 0
#endif

#ifndef nullptr
#define nullptr NULL
#endif

#define DEBUG

#ifdef DEBUG
#define expo_d(...) __android_log_print(ANDROID_LOG_DEBUG, "apkexpo", __VA_ARGS__)
#else
#define expo_d(...)
#endif

jstring expo_new_jstr(JNIEnv* env, const char* pat);
const char *expo_const_ptr(JNIEnv *env, jstring str);
char *expo_jstr2ptr(JNIEnv *env, jstring jstr);

int expo_get_pid_by_name(const char *process_name);
int expo_inject_so2pid(pid_t target_pid, const char *library_path, const char *function_name,
                       const char *param, size_t param_size);

uint32_t expo_hook(const char *module_path, uint32_t hook_func, const char *symbol_name);

jstring expo_get_module_path(JNIEnv *env);
jstring expo_get_process_name(JNIEnv *env, bool intact) ;
jstring expo_get_package_name(JNIEnv *env, jobject contextObj);

jmethodID expo_find_method(JNIEnv *env, const char *className, const char *funcName,
                           const char *funcSignature);

jmethodID expo_find_static_method(JNIEnv *env, const char *className, const char *funcName,
                                  const char *funcSignature);

JNIEnv *expo_attach_env();
void expo_detach_env();

#endif //APKEXPO_EXPO_H
