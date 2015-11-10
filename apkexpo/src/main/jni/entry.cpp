// khuangia@gmail.com

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <unistd.h>
#include <libgen.h>
#include "expo.h"

#include "ddx_hooks_impl.h"

#define STR_FORCE_PACKAGE_NAME  "com.kr.apkexpo"
#define STR_CLASS_NAME_MAIN     "com/kr/apkexpo/Activity_Main"
#define STR_INJECT_TMP_DIR      "/data/local/tmp"

#ifndef NATIVE_METHODS_INVOICE

    static bool check_loading_process(JNIEnv *env);
    static int register_native_methods(JNIEnv *, const char *, JNINativeMethod *, int);

    static void JNICALL startup_inject(JNIEnv *env, jobject claz, jstring stringTarget);
    static void JNICALL init_context(JNIEnv *env, jobject claz, jobject context);

    extern "C" JNIEXPORT jint dl_entry(const char *myPackageName);
    extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* jvm, void* reserved);
#endif


static JNINativeMethod m_NativeMethods[] = {
    {"startupNativeCode", "(Ljava/lang/String;)V", (void *)startup_inject},
    {"initNativeCode", "(Ljava/lang/Object;)V", (void *)init_context}
};

static char m_package_name[BUFSIZ] = "default";
static jobject m_app_context = nullptr;
static jobject m_main_activity = nullptr;


static int register_native_methods(JNIEnv *env, const char *className, JNINativeMethod * methods,
                                 int numMethods) {

    jclass claz = env->FindClass(className);
    if (claz == NULL) return JNI_FALSE;

    if (0 > env->RegisterNatives(claz, methods, numMethods)) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static bool check_loading_process(JNIEnv *env) {

    jmethodID versionFunc = expo_find_static_method(env, STR_CLASS_NAME_MAIN,
                                             "get_5b2b77c68c95474198f70b7a9e471c9f", "()I");

    if (versionFunc != NULL) {
        return (13968 == env->CallStaticIntMethod(
                env->FindClass(STR_CLASS_NAME_MAIN), versionFunc));
    }

    return false;
}

extern "C" JNIEXPORT jint JNI_OnLoad(JavaVM* jvm, void* reserved) {

    expo_d("\n\n");
    JNIEnv *env = NULL;

    if (jvm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK) {
        expo_d("JNI_OnLoad: something error in jni\n");
    }

    if (check_loading_process(env)) {
        expo_d("JNI_OnLoad: so has been loaded\n");

        if (register_native_methods(env, STR_CLASS_NAME_MAIN, m_NativeMethods,
                sizeof(m_NativeMethods) / sizeof(JNINativeMethod))) {
            expo_d("JNI_OnLoad: class has been set\n");
        }
        else {
            expo_d("JNI_OnLoad: class not been set\n");
        }
    }
    else {
        expo_d("JNI_OnLoad: so loaded by other lib\n");
    }

    return JNI_VERSION_1_4;
}

static void su_command(const char *cmd, const char *prefix, const char *dir = NULL) {

    if (!cmd) return;
    size_t cmd_len = strlen(cmd);

    char *p_command = (char *)malloc(cmd_len + MAX_PATH);
    memset(p_command, 0, cmd_len + MAX_PATH);

    strcpy(p_command, "su -c \"");

    if (dir) {
        strcat(p_command, "cd ");
        strcat(p_command, dir);
        strcat(p_command, ";");
    }

    if (prefix) {
        strcat(p_command, prefix);
        strcat(p_command, " ");
    }

    strcat(p_command, cmd);
    strcat(p_command, "\"");

    expo_d("su_command: %s\n", p_command);
    system(p_command);

    free(p_command);
}

static void get_inject_tmp_dir(char *buf) {
    strcpy(buf, STR_INJECT_TMP_DIR);
}

static void replace_file(char *tar_path, size_t max_tar_path_len, const char *path_src_file) {

    if (!path_src_file || (0 != access(path_src_file, F_OK))) {
        memset(tar_path, 0, max_tar_path_len);
        return;
    }

    char *p_path_src_file = strdup(path_src_file);
    char *p_src_name = basename(p_path_src_file);

    char tar_path_dir[50] = {0};
    get_inject_tmp_dir(tar_path_dir);

    memset(tar_path, 0, max_tar_path_len);
    sprintf(tar_path, "%s/%s", tar_path_dir, p_src_name);
    if (access(tar_path, F_OK) == 0) {
        su_command(tar_path, "rm -f");
    }

    size_t max_copy_cmd_len = max_tar_path_len * 2;

    char *copy_cmd = (char *)malloc(max_copy_cmd_len);
    memset(copy_cmd, 0, max_copy_cmd_len);

    sprintf(copy_cmd, "cp -f %s %s", p_path_src_file, tar_path_dir);
    free(p_path_src_file);

    su_command(copy_cmd, NULL);
    su_command(tar_path, "chmod 777");
}

static char *prepare_for_inject(JNIEnv *env, const char *func_name) {

    char *buffer = (char *)malloc(MAX_PATH);
    memset(buffer, 0, MAX_PATH);

    jclass cls = env->GetObjectClass(m_main_activity);
    jmethodID jid = env->GetMethodID(cls, func_name, "()Ljava/lang/String;");
    if (NULL != jid) {

        jstring strRet = (jstring)env->CallObjectMethod(m_main_activity, jid);
        if (NULL != strRet) {
            replace_file(buffer, MAX_PATH - 2, expo_jstr2ptr(env, strRet));
        }
    }

    if (strlen(buffer) > 0) {
        expo_d("Prepare: %s -> %s\n", func_name, buffer);
    }
    else {
        expo_d("Prepare: can not get file path for: %s\n", func_name);
    }

    return buffer;
}

static void JNICALL init_context(JNIEnv *env, jobject obj, jobject context2) {

    m_main_activity = obj;
    m_app_context = context2;

    strcpy(m_package_name, "unknown");

    jclass clazz = env->GetObjectClass(context2);
    jmethodID jid = env->GetMethodID(clazz, "getPackageName", "()Ljava/lang/String;");
    if (NULL != jid) {

        jstring strRet = (jstring)env->CallObjectMethod(context2, jid);
        if (NULL != strRet) {
            strncpy(m_package_name, expo_jstr2ptr(env, strRet), BUFSIZ - 10);
        }
    }

    expo_d("init_context: pkg-> %s", m_package_name);
    if (strncmp(STR_FORCE_PACKAGE_NAME, m_package_name, strlen(STR_FORCE_PACKAGE_NAME)) != 0) {
        exit(0);
    }

    // su_command(mount -o remount,rw rootfs /");
}

static void JNICALL startup_inject(JNIEnv *env, jobject claz, jstring stringTarget) {

    expo_d("startup_inject: startup startup_inject\n");

    if ((NULL == m_app_context) || (stringTarget == NULL)) {
        expo_d("startup_inject: didnot init context\n");
        return;
    }
    else {
        char sz_inj_tmp_dir[100] = {0};
        get_inject_tmp_dir(sz_inj_tmp_dir);

        if (access(sz_inj_tmp_dir, F_OK) != 0) {
            su_command(sz_inj_tmp_dir, "mkdir");
        }
    }

    char *sys_command = (char *)malloc(BUFSIZ * 2);
    {
        char *tar_process_name = (char *) malloc(100);
        strcpy(tar_process_name, expo_const_ptr(env, stringTarget));

        char *so_library_path = prepare_for_inject(env, "getSoFilePath");
        char *lib_inject_path = prepare_for_inject(env, "getLibFilePath");

        sprintf(sys_command, "%s %s %s dl_entry com.efun.android.ddx",
                lib_inject_path, tar_process_name, so_library_path);

        free(so_library_path);
        free(tar_process_name);
        free(lib_inject_path);
    }

    su_command(sys_command, NULL, STR_INJECT_TMP_DIR);
    free(sys_command);

    expo_d("startup_inject: end of startup_inject\n");
}

extern "C" JNIEXPORT jint dl_entry(const char *myPackageName) {

    expo_d("dl_entry: hello from injected process: %s, pid: %d\n", myPackageName, getpid());

    JNIEnv *env = expo_attach_env();
    if (env == nullptr) {
        expo_d("dl_entry: attach env in new process failed\n");
    }
    else {
        expo_d("dl_entry: attach env in new process succeed\n");
    }

    expo_d(init_ddx_hooks(env) ? "dl_entry: init hooks\n" : "dl_entry: hook failed\n");

    expo_detach_env();
    return 0;
}












