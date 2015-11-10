// khuangia@gmail.com
// thanks to free code on internet

#include <malloc.h>
#include <stdio.h>
#include <stdlib.h>

#include <libgen.h>
#include <asm/ptrace.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <sys/types.h>

#include <elf.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <dlfcn.h>
#include <dirent.h>
#include <unistd.h>

#include "expo.h"

jmethodID expo_find_method(JNIEnv *env, const char *className, const char *funcName,
                           const char *funcSignature) {

    jmethodID methodid = 0;

    jclass jclazz = env->FindClass(className);
    if (jclazz == 0) {
        expo_d("class not find: %s\n", className);
        return nullptr;
    }

    methodid = env->GetMethodID(jclazz, funcName, funcSignature);
    if (methodid == 0) {
        expo_d("method not find: %s\n", funcName);
        return nullptr;
    }

    return methodid;
}

jmethodID expo_find_static_method(JNIEnv *env, const char *className, const char *funcName,
                                  const char *funcSignature) {

    jmethodID methodid = 0;

    jclass jclazz = env->FindClass(className);
    if (jclazz == 0) {
        expo_d("class not find: %s\n", className);
        return methodid;
    }

    methodid = env->GetStaticMethodID(jclazz, funcName, funcSignature);
    if (methodid == 0) {
        expo_d("method not find: %s\n", funcName);
        return methodid;
    }

    return methodid;
}

const char *expo_const_ptr(JNIEnv *env, jstring str) {
    return env->GetStringUTFChars(str, 0);
}

jstring expo_new_jstr(JNIEnv* env, const char* pat) {
    jclass strClass = env->FindClass("java/lang/String");
    jmethodID ctorID = env->GetMethodID(strClass, "", "([BLjava/lang/String;)V");
    jbyteArray bytes = env->NewByteArray(strlen(pat));
    env->SetByteArrayRegion(bytes, 0, strlen(pat), (jbyte*)pat);
    jstring  encoding = env->NewStringUTF("UTF-8");
    return (jstring)env->NewObject(strClass, ctorID, bytes, encoding);
}

char* expo_jstr2ptr(JNIEnv* env, jstring jstr) {
    char* pStr = NULL;
    jclass jstrObj = env->FindClass("java/lang/String");
    jstring encode = env->NewStringUTF("utf-8");
    jmethodID methodId = env->GetMethodID(jstrObj, "getBytes", "(Ljava/lang/String;)[B");
    jbyteArray byteArray = (jbyteArray)env->CallObjectMethod(jstr, methodId, encode);
    jsize strLen = env->GetArrayLength(byteArray);
    jbyte *jBuf = env->GetByteArrayElements(byteArray, JNI_FALSE);
    if (jBuf > 0) {
        pStr = (char*)malloc(strLen + 1);
        if (!pStr) {
            return NULL;
        }

        memcpy(pStr, jBuf, strLen);
        pStr[strLen] = 0;
    }

    env->ReleaseByteArrayElements(byteArray, jBuf, 0);
    return pStr;
}

static bool mJavaHolding = false;
static JavaVM *mJavaVM = NULL;

static JavaVM * expo_find_jvm() {

    void *mod = dlopen("libandroid_runtime.so", 0);
    if (mod) {
        JavaVM **ppVM = (JavaVM **)dlsym(mod, "_ZN7android14AndroidRuntime7mJavaVME");
        if (ppVM) return *ppVM;
    }

    return nullptr;
}

JNIEnv *expo_attach_env() {

    if (nullptr != expo_find_jvm()) {
        mJavaVM = expo_find_jvm();
    }
    else {
        expo_d("couldnot find jvm\n");
        return nullptr;
    }

    int status;
    JNIEnv *env = NULL;

    status = mJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);
    if (status < 0) {

        status = mJavaVM->AttachCurrentThread(&env, NULL);
        if (status < 0) {
            return NULL;
        }

        mJavaHolding = true;
    }

    return env;
}

void expo_detach_env() {

    if (mJavaHolding) {
        mJavaVM->DetachCurrentThread();
    }
}

#define CPSR_T_MASK     ( 1u << 5 )

#if defined(__i386__)
#define pt_regs         user_regs_struct
#endif

const char *libc_path = "/system/lib/libc.so";
const char *linker_path = "/system/bin/linker";

int ptrace_readdata(pid_t pid, uint8_t *src, uint8_t *buf, size_t size) {
    uint32_t i, j, remain;
    uint8_t *laddr;

    union u {
        long val;
        char chars[sizeof(long)];
    } d;

    j = size / 4;
    remain = size % 4;

    laddr = buf;

    for (i = 0; i < j; i++) {
        d.val = ptrace(PTRACE_PEEKTEXT, pid, src, 0);
        memcpy(laddr, d.chars, 4);
        src += 4;
        laddr += 4;
    }

    if (remain > 0) {
        d.val = ptrace(PTRACE_PEEKTEXT, pid, src, 0);
        memcpy(laddr, d.chars, remain);
    }

    return 0;
}

int ptrace_writedata(pid_t pid, uint8_t *dest, uint8_t *data, size_t size) {
    uint32_t i, j, remain;
    uint8_t *laddr;

    union u {
        long val;
        char chars[sizeof(long)];
    } d;

    j = size / 4;
    remain = size % 4;

    laddr = data;

    for (i = 0; i < j; i++) {
        memcpy(d.chars, laddr, 4);
        ptrace(PTRACE_POKETEXT, pid, dest, d.val);

        dest += 4;
        laddr += 4;
    }

    if (remain > 0) {
        d.val = ptrace(PTRACE_PEEKTEXT, pid, dest, 0);
        for (i = 0; i < remain; i++) {
            d.chars[i] = *laddr++;
        }

        ptrace(PTRACE_POKETEXT, pid, dest, d.val);
    }

    return 0;
}

int ptrace_getregs(pid_t pid, struct pt_regs *regs) {
    if (ptrace(PTRACE_GETREGS, pid, NULL, regs) < 0) {
        expo_d("ptrace_getregs: Can not get register values");
        return -1;
    }

    return 0;
}

int ptrace_setregs(pid_t pid, struct pt_regs *regs) {
    if (ptrace(PTRACE_SETREGS, pid, NULL, regs) < 0) {
        expo_d("ptrace_setregs: Can not set register values");
        return -1;
    }

    return 0;
}

int ptrace_continue(pid_t pid) {
    if (ptrace(PTRACE_CONT, pid, NULL, 0) < 0) {
        expo_d("ptrace_cont");
        return -1;
    }

    return 0;
}

int ptrace_attach(pid_t pid) {
    if (ptrace(PTRACE_ATTACH, pid, NULL, 0) < 0) {
        expo_d("ptrace_attach");
        return -1;
    }

    int status = 0;
    waitpid(pid, &status, WUNTRACED);

    return 0;
}

int ptrace_detach(pid_t pid) {
    if (ptrace(PTRACE_DETACH, pid, NULL, 0) < 0) {
        expo_d("ptrace_detach");
        return -1;
    }

    return 0;
}

#if defined(__arm__)    
int ptrace_call(pid_t pid, uint32_t addr, long *params, uint32_t num_params, struct pt_regs* regs)    
{    
    uint32_t i;    
    for (i = 0; i < num_params && i < 4; i ++) {    
        regs->uregs[i] = params[i];    
    }    
    
    //    
    // push remained params onto stack    
    //    
    if (i < num_params) {    
        regs->ARM_sp -= (num_params - i) * sizeof(long) ;    
        ptrace_writedata(pid, (void *)regs->ARM_sp, (uint8_t *)&params[i], (num_params - i) * sizeof(long));    
    }    
    
    regs->ARM_pc = addr;    
    if (regs->ARM_pc & 1) {    
        /* thumb */    
        regs->ARM_pc &= (~1u);    
        regs->ARM_cpsr |= CPSR_T_MASK;    
    } else {    
        /* arm */    
        regs->ARM_cpsr &= ~CPSR_T_MASK;    
    }    
    
    regs->ARM_lr = 0;        
    
    if (ptrace_setregs(pid, regs) == -1     
            || ptrace_continue(pid) == -1) {    
        expo_d("error\n");    
        return -1;    
    }    
    
    int stat = 0;  
    waitpid(pid, &stat, WUNTRACED);  
    while (stat != 0xb7f) {  
        if (ptrace_continue(pid) == -1) {  
            expo_d("error\n");  
            return -1;  
        }  
        waitpid(pid, &stat, WUNTRACED);  
    }  
    
    return 0;    
}    
    
#elif defined(__i386__)    
long ptrace_call(pid_t pid, uint32_t addr, long *params, uint32_t num_params, struct user_regs_struct * regs)    
{    
    regs->esp -= (num_params) * sizeof(long) ;    
    ptrace_writedata(pid, (uint8_t *)regs->esp, (uint8_t *)params, (num_params) * sizeof(long));
    
    long tmp_addr = 0x00;    
    regs->esp -= sizeof(long);    
    ptrace_writedata(pid, (uint8_t *)regs->esp, (uint8_t *)&tmp_addr, sizeof(tmp_addr));
    
    regs->eip = addr;    
    
    if (ptrace_setregs(pid, regs) == -1     
            || ptrace_continue( pid) == -1) {    
        expo_d("error\n");    
        return -1;    
    }    
    
    int stat = 0;  
    waitpid(pid, &stat, WUNTRACED);  
    while (stat != 0xb7f) {  
        if (ptrace_continue(pid) == -1) {  
            expo_d("error\n");  
            return -1;  
        }  
        waitpid(pid, &stat, WUNTRACED);  
    }  
    
    return 0;    
}    
#else     
#error "Not supported"    
#endif    

void *get_module_base(pid_t pid, const char *module_name) {
    FILE *fp;
    long addr = 0;
    char *pch;
    char filename[32];
    char line[1024];

    if (pid < 0) {
        /* self process */
        snprintf(filename, sizeof(filename), "/proc/self/maps");
    } else {
        snprintf(filename, sizeof(filename), "/proc/%d/maps", pid);
    }

    fp = fopen(filename, "r");

    if (fp != NULL) {
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, module_name)) {
                pch = strtok(line, "-");
                addr = strtoul(pch, NULL, 16);

                if (addr == 0x8000)
                    addr = 0;

                break;
            }
        }

        fclose(fp);
    }

    return (void *) addr;
}

void *get_remote_addr(pid_t target_pid, const char *module_name, void *local_addr) {
    void *local_handle, *remote_handle;

    local_handle = get_module_base(-1, module_name);
    remote_handle = get_module_base(target_pid, module_name);

    expo_d("get_remote_addr: local[%x], remote[%x]\n", (unsigned int)local_handle, (unsigned int)remote_handle);

    char *ret_addr = (char *) ((uint32_t) local_addr + (uint32_t) remote_handle -
                               (uint32_t) local_handle);

#if defined(__i386__)
    if (!strcmp(module_name, libc_path)) {
        ret_addr += 2;
    }
#endif
    return ret_addr;
}

int expo_get_pid_by_name(const char *process_name) {
    int id;
    pid_t pid = -1;
    DIR *dir;
    FILE *fp;
    char filename[32];
    char cmdline[256];

    struct dirent *entry;

    if (process_name == NULL)
        return -1;

    dir = opendir("/proc");
    if (dir == NULL)
        return -1;

    while ((entry = readdir(dir)) != NULL) {
        id = atoi(entry->d_name);
        if (id != 0) {
            sprintf(filename, "/proc/%d/cmdline", id);
            fp = fopen(filename, "r");
            if (fp) {
                fgets(cmdline, sizeof(cmdline), fp);
                fclose(fp);

                if (strcmp(process_name, cmdline) == 0) {
                    /* process found */
                    pid = id;
                    break;
                }
            }
        }
    }

    closedir(dir);
    return pid;
}

void ptrace_d_all_regs(struct pt_regs *regs) {
#if defined(__i386__)
    expo_d("%x %x %x %x", regs->eax, regs->eip, regs->esp, regs->ebp);
#endif 
}

long ptrace_retval(struct pt_regs *regs) {
#if defined(__arm__)
    return regs->ARM_r0;
#elif defined(__i386__)
    return regs->eax;
#else
#error "Not supported"
#endif
}

long ptrace_ip(struct pt_regs *regs) {
#if defined(__arm__)
    return regs->ARM_pc;
#elif defined(__i386__)
    return regs->eip;
#else
#error "Not supported"
#endif
}

int ptrace_call_wrapper(pid_t target_pid, const char *func_name, void *func_addr, long *param_stack,
                        int param_num, struct pt_regs *regs) {
    expo_d("Calling %s in target process.\n", func_name);
    if (ptrace_call(target_pid, (uint32_t) func_addr, param_stack, param_num, regs) == -1)
        return -1;

    if (ptrace_getregs(target_pid, regs) == -1)
        return -1;
    expo_d("Target process returned from %s, return value=%x, pc=%x \n",
         func_name, ptrace_retval(regs), ptrace_ip(regs));
    return 0;
}

void expo_inject_err(pid_t target_pid, void *dlerror_addr, struct pt_regs& regs) {
    
    void * hook_entry_addr = NULL;
    uint8_t buffera[100] = {0};
    
    if (ptrace_call_wrapper(target_pid, "dlerror", dlerror_addr, NULL, 0, &regs) == -1)
        return;
        
    hook_entry_addr = (void *)ptrace_retval(&regs);
    ptrace_readdata(target_pid, (uint8_t *)hook_entry_addr, buffera, 90);
    
    expo_d("err: %s\n", buffera);
}

#if defined(__i386__) // means below x86 code

int expo_inject_so2pid_stub(pid_t target_pid, const char *library_path, const char *function_name, const char *param, size_t param_size) {
    
    int ret = -1;
    void *mmap_addr, *dlopen_addr, *dlsym_addr, *dlclose_addr, *dlerror_addr;
    void *local_handle, *remote_handle, *dlhandle;
    uint8_t *map_base = 0;
    
    struct pt_regs regs, original_regs;
    uint32_t code_length;
    long parameters[10];
    
    void * hook_entry_addr = NULL;
    void * sohandle = NULL;
  
    expo_d("Injecting process: %d\n", target_pid);

    if (ptrace_attach(target_pid) == -1)
        goto exit;

    if (ptrace_getregs(target_pid, &regs) == -1)
        goto exit;

    /* save original registers */
    memcpy(&original_regs, &regs, sizeof(regs));

    mmap_addr = get_remote_addr(target_pid, libc_path, (void *)mmap);
    expo_d("Remote mmap address: %x\n", mmap_addr);

    /* call mmap */
    parameters[0] = 0;  // addr
    parameters[1] = 0x4000; // size
    parameters[2] = PROT_READ | PROT_WRITE | PROT_EXEC;  // prot
    parameters[3] = MAP_ANONYMOUS | MAP_PRIVATE; // flags
    parameters[4] = 0; //fd
    parameters[5] = 0; //offset

    if (ptrace_call_wrapper(target_pid, "mmap", mmap_addr, parameters, 6, &regs) == -1)
        goto exit;

    map_base = (uint8_t *)ptrace_retval(&regs);

    dlopen_addr = get_remote_addr( target_pid, linker_path, (void *)dlopen );
    dlsym_addr = get_remote_addr( target_pid, linker_path, (void *)dlsym );
    dlclose_addr = get_remote_addr( target_pid, linker_path, (void *)dlclose );
    dlerror_addr = get_remote_addr( target_pid, linker_path, (void *)dlerror );

    expo_d("Get imports: dlopen: %x, dlsym: %x, dlclose: %x, dlerror: %x\n",
            dlopen_addr, dlsym_addr, dlclose_addr, dlerror_addr);

    expo_d("library path = %s\n", library_path);
    ptrace_writedata(target_pid, map_base, (uint8_t *)library_path, strlen(library_path) + 1);

    parameters[0] = (long)map_base;
    parameters[1] = RTLD_NOW| RTLD_GLOBAL;

    if (ptrace_call_wrapper(target_pid, "dlopen", dlopen_addr, parameters, 2, &regs) == -1)
        goto exit;

    sohandle = (void *)ptrace_retval(&regs);

#define FUNCTION_NAME_ADDR_OFFSET       0x100
    ptrace_writedata(target_pid, map_base + FUNCTION_NAME_ADDR_OFFSET, (uint8_t *)function_name, strlen(function_name) + 1);
    parameters[0] = (long)sohandle;
    parameters[1] = (long)(map_base + FUNCTION_NAME_ADDR_OFFSET);

    if (ptrace_call_wrapper(target_pid, "dlsym", dlsym_addr, parameters, 2, &regs) == -1)
        goto exit;

    hook_entry_addr = (void *)ptrace_retval(&regs);
    expo_d("func name: %s func addr: %x\n", function_name, hook_entry_addr);
       
    if ((long)hook_entry_addr < 0x400000) {
        
        expo_inject_err(target_pid, dlerror_addr, regs);
        goto exit;
    }
    
#define FUNCTION_PARAM_ADDR_OFFSET      0x200
    ptrace_writedata(target_pid, map_base + FUNCTION_PARAM_ADDR_OFFSET, (uint8_t *)param, param_size + 1);
    parameters[0] = (long)(map_base + FUNCTION_PARAM_ADDR_OFFSET);

    if (ptrace_call_wrapper(target_pid, function_name, hook_entry_addr, parameters, 1, &regs) == -1)
        goto exit;

#if 1
    //
    // injected, didnot consider detach
    //
    parameters[0] = (long)sohandle;

    if (ptrace_call_wrapper(target_pid, "dlclose", (void *)dlclose, parameters, 1, &regs) == -1)
        goto exit;

    /* restore */
    ptrace_setregs(target_pid, &original_regs);
    ptrace_detach(target_pid);
#endif

    ret = 0;

exit:
    return ret;
}

#elif defined(__arm__) // means below is arm code

#define REMOTE_ADDR( addr, local_base, remote_base ) ( (uint32_t)(addr) + (uint32_t)(remote_base) - (uint32_t)(local_base) )

int expo_inject_so2pid_stub(pid_t target_pid, const char *library_path, const char *function_name,
                       const char *param, size_t param_size) {

    int ret = -1;
	void *mmap_addr, *dlopen_addr, *dlsym_addr, *dlclose_addr;
	void *local_handle, *remote_handle, *dlhandle;
	uint8_t *map_base;
	uint8_t *dlopen_param1_ptr, *dlsym_param2_ptr, *saved_r0_pc_ptr, *inject_param_ptr, *remote_code_ptr, *local_code_ptr;

	struct pt_regs regs, original_regs;
	extern uint32_t _dlopen_addr_s, _dlopen_param1_s, _dlopen_param2_s, _dlsym_addr_s, \
			_dlsym_param2_s, _dlclose_addr_s, _inject_start_s, _inject_end_s, _inject_function_param_s, \
			_saved_cpsr_s, _saved_r0_pc_s;

	uint32_t code_length;
	long parameters[10];

	expo_d( "Injecting process: %d\n", target_pid );

	if ( ptrace_attach( target_pid ) == -1 )
		return EXIT_SUCCESS;

	if ( ptrace_getregs( target_pid, &regs ) == -1 )
		goto exit;

	/* save original registers */
	memcpy( &original_regs, &regs, sizeof(regs) );

	mmap_addr = get_remote_addr( target_pid, "/system/lib/libc.so", (void *)mmap );

	expo_d( "Remote mmap address: %x\n", mmap_addr );

	/* call mmap */
	parameters[0] = 0;	// addr
	parameters[1] = 0x4000; // size
	parameters[2] = PROT_READ | PROT_WRITE | PROT_EXEC;  // prot
	parameters[3] =  MAP_ANONYMOUS | MAP_PRIVATE; // flags
	parameters[4] = 0; //fd
	parameters[5] = 0; //offset

	expo_d( "Calling mmap in target process.\n" );

	if ( ptrace_call( target_pid, (uint32_t)mmap_addr, parameters, 6, &regs ) == -1 )
		goto exit;

	if ( ptrace_getregs( target_pid, &regs ) == -1 )
		goto exit;

	expo_d( "Target process returned from mmap, return value=%x, pc=%x \n", regs.ARM_r0, regs.ARM_pc );

	map_base = (uint8_t *)regs.ARM_r0;

	dlopen_addr = get_remote_addr( target_pid, linker_path, (void *)dlopen );
	dlsym_addr = get_remote_addr( target_pid, linker_path, (void *)dlsym );
	dlclose_addr = get_remote_addr( target_pid, linker_path, (void *)dlclose );

	expo_d( "Get imports: dlopen: %x, dlsym: %x, dlclose: %x\n", dlopen_addr, dlsym_addr, dlclose_addr );

	remote_code_ptr = map_base + 0x3C00;
	local_code_ptr = (uint8_t *)&_inject_start_s;

	_dlopen_addr_s = (uint32_t)dlopen_addr;
	_dlsym_addr_s = (uint32_t)dlsym_addr;
	_dlclose_addr_s = (uint32_t)dlclose_addr;

	expo_d( "Inject code start: %x, end: %x\n", local_code_ptr, &_inject_end_s );

	code_length = (uint32_t)&_inject_end_s - (uint32_t)&_inject_start_s;
	dlopen_param1_ptr = local_code_ptr + code_length + 0x20;
	dlsym_param2_ptr = dlopen_param1_ptr + MAX_PATH;
	saved_r0_pc_ptr = dlsym_param2_ptr + MAX_PATH;
	inject_param_ptr = saved_r0_pc_ptr + MAX_PATH;

	/* dlopen parameter 1: library name */
	strcpy( dlopen_param1_ptr, library_path );
	_dlopen_param1_s = REMOTE_ADDR( dlopen_param1_ptr, local_code_ptr, remote_code_ptr );
	expo_d( "_dlopen_param1_s: %x\n", _dlopen_param1_s );

	/* dlsym parameter 2: function name */
	strcpy( dlsym_param2_ptr, function_name );
	_dlsym_param2_s = REMOTE_ADDR( dlsym_param2_ptr, local_code_ptr, remote_code_ptr );
	expo_d( "_dlsym_param2_s: %x\n", _dlsym_param2_s );

	/* saved cpsr */
	_saved_cpsr_s = original_regs.ARM_cpsr;

	/* saved r0-pc */
	memcpy( saved_r0_pc_ptr, &(original_regs.ARM_r0), 16 * 4 ); // r0 ~ r15
	_saved_r0_pc_s = REMOTE_ADDR( saved_r0_pc_ptr, local_code_ptr, remote_code_ptr );
	expo_d( "_saved_r0_pc_s: %x\n", _saved_r0_pc_s );

	/* Inject function parameter */
	memcpy( inject_param_ptr, param, param_size );
	_inject_function_param_s = REMOTE_ADDR( inject_param_ptr, local_code_ptr, remote_code_ptr );
	expo_d( "_inject_function_param_s: %x\n", _inject_function_param_s );

	expo_d( "Remote shellcode address: %x\n", remote_code_ptr );
	ptrace_writedata( target_pid, remote_code_ptr, local_code_ptr, 0x400 );

	memcpy( &regs, &original_regs, sizeof(regs) );
	regs.ARM_sp = (long)remote_code_ptr;
	regs.ARM_pc = (long)remote_code_ptr;
	ptrace_setregs( target_pid, &regs );
	ptrace_detach( target_pid );

	// inject succeeded
	ret = 0;

exit:
	return ret;
}

#else
#error "Not supported"
#endif

int expo_inject_so2pid(pid_t target_pid, const char *library_path, const char *function_name,
                       const char *param, size_t param_size) {


	return expo_inject_so2pid_stub(target_pid, library_path, function_name, param, param_size);
}

jstring getPackageName(JNIEnv *env, jobject contextObj) {

    jclass jctx = env->GetObjectClass(contextObj);
    if (jctx != NULL) {
        jmethodID jmidGetPackageName = env->GetMethodID(jctx, "getPackageName", "()Ljava/lang/String");
        if (jmidGetPackageName != NULL) {

            return (jstring)env->CallObjectMethod(contextObj, jmidGetPackageName);
        }
    }

    return NULL;
}

jstring expo_get_process_name(JNIEnv *env, bool intact) {

    char szPath[BUFSIZ] = {0};

    char sz_link_param[50] = {0};
    sprintf(sz_link_param, "/proc/%d/cmdline", getpid());

    if (readlink(sz_link_param, szPath, BUFSIZ - 10) > 0) {
        if (intact) {
            return env->NewStringUTF(szPath);
        }
        else {
            return env->NewStringUTF(basename(szPath));
        }
    }

    return NULL;
}

jstring expo_get_module_path(JNIEnv *env) {

    char szModulePath[BUFSIZ] = {0};
    strcpy(szModulePath, "/data/data/com.kr.apkexpo/lib/libapkexpo.lib");

    jstring js = NULL;

    if (szModulePath[0]) {
        js = env->NewStringUTF(szModulePath);
    }

    return js;
}

#define PAGE_START(addr, size) ~((size) - 1) & (addr)

uint32_t find_got_entry_address(const char *module_path, const char *symbol_name) {

    uint32_t module_base = (uint32_t)get_module_base(-1, module_path);
    if ( module_base == 0 ) {
        expo_d("[-] it seems that process %d does not dependent on %s", getpid(), module_path);
        return 0;
    }
    
    expo_d("base address of %s: 0x%x", module_path, module_base);

    int fd = open(module_path, O_RDONLY);
    if ( fd == -1 ) {
        expo_d("[-] open %s error!", module_path);
        return 0;
    }

    Elf32_Ehdr *elf_header = (Elf32_Ehdr *)malloc(sizeof(Elf32_Ehdr));
    if ( read(fd, elf_header, sizeof(Elf32_Ehdr)) != sizeof(Elf32_Ehdr) ) 
    {
        expo_d("[-] read %s error! in %s at line %d", module_path, __FILE__, __LINE__);
        return 0;
    }

    uint32_t sh_base = elf_header->e_shoff;
    uint32_t ndx = elf_header->e_shstrndx;
    uint32_t shstr_base = sh_base + ndx * sizeof(Elf32_Shdr);
    expo_d("start of section headers: 0x%x", sh_base);
    expo_d("section header string table index: %d", ndx);
    expo_d("section header string table offset: 0x%x", shstr_base);

    lseek(fd, shstr_base, SEEK_SET);
    Elf32_Shdr *shstr_shdr = (Elf32_Shdr *)malloc(sizeof(Elf32_Shdr));
    if ( read(fd, shstr_shdr, sizeof(Elf32_Shdr)) != sizeof(Elf32_Shdr) ) 
    {
        expo_d("[-] read %s error! in %s at line %d", module_path, __FILE__, __LINE__);
        return 0;
    }
    expo_d("section header string table offset: 0x%x", shstr_shdr->sh_offset);

    char *shstrtab = (char *)malloc(sizeof(char) * shstr_shdr->sh_size);
    lseek(fd, shstr_shdr->sh_offset, SEEK_SET);
    if ( read(fd, shstrtab, shstr_shdr->sh_size) != shstr_shdr->sh_size ) 
    {
        expo_d("[-] read %s error! in %s at line %d", module_path, __FILE__, __LINE__);
        return 0;
    }

    Elf32_Shdr *shdr = (Elf32_Shdr *)malloc(sizeof(Elf32_Shdr));
    Elf32_Shdr *relplt_shdr = (Elf32_Shdr *)malloc(sizeof(Elf32_Shdr));
    Elf32_Shdr *dynsym_shdr = (Elf32_Shdr *)malloc(sizeof(Elf32_Shdr));
    Elf32_Shdr *dynstr_shdr = (Elf32_Shdr *)malloc(sizeof(Elf32_Shdr));

    lseek(fd, sh_base, SEEK_SET);
    if ( read(fd, shdr, sizeof(Elf32_Shdr)) != sizeof(Elf32_Shdr) ) 
    {
        expo_d("[-] read %s error! in %s at line %d", module_path, __FILE__, __LINE__);
        perror("Error");
        return 0;
    }
    int i = 1;
    char *s = NULL;
    for ( ; i < elf_header->e_shnum; i++ ) 
    {
        s = shstrtab + shdr->sh_name;
        if ( strcmp(s, ".rel.plt") == 0 )
            memcpy(relplt_shdr, shdr, sizeof(Elf32_Shdr));
        else if ( strcmp(s, ".dynsym") == 0 ) 
            memcpy(dynsym_shdr, shdr, sizeof(Elf32_Shdr));
        else if ( strcmp(s, ".dynstr") == 0 ) 
            memcpy(dynstr_shdr, shdr, sizeof(Elf32_Shdr));

        if ( read(fd, shdr, sizeof(Elf32_Shdr)) != sizeof(Elf32_Shdr) ) 
        {
            expo_d("[-] read %s error! i = %d, in %s at line %d", module_path, i, __FILE__, __LINE__);
            return 0;
        }
    }

    expo_d("offset of .rel.plt section: 0x%x", relplt_shdr->sh_offset);
    expo_d("offset of .dynsym section: 0x%x", dynsym_shdr->sh_offset);
    expo_d("offset of .dynstr section: 0x%x", dynstr_shdr->sh_offset);

    char *dynstr = (char *)malloc(sizeof(char) * dynstr_shdr->sh_size);
    lseek(fd, dynstr_shdr->sh_offset, SEEK_SET);
    if ( read(fd, dynstr, dynstr_shdr->sh_size) != dynstr_shdr->sh_size ) 
    {
        expo_d("[-] read %s error!", module_path);
        return 0;
    }

    Elf32_Sym *dynsymtab = (Elf32_Sym *)malloc(dynsym_shdr->sh_size);
    lseek(fd, dynsym_shdr->sh_offset, SEEK_SET);
    if ( read(fd, dynsymtab, dynsym_shdr->sh_size) != dynsym_shdr->sh_size ) 
    {
        expo_d("[-] read %s error!", module_path);
        return 0;
    }

    Elf32_Rel *rel_ent = (Elf32_Rel *)malloc(sizeof(Elf32_Rel));
    lseek(fd, relplt_shdr->sh_offset, SEEK_SET);
    if ( read(fd, rel_ent, sizeof(Elf32_Rel)) != sizeof(Elf32_Rel) ) 
    {
        expo_d("[-] read %s error!", module_path);
        return 0;
    }
    for ( i = 0; i < relplt_shdr->sh_size / sizeof(Elf32_Rel); i++ ) 
    {
        ndx = ELF32_R_SYM(rel_ent->r_info);
        if ( strcmp(dynstr + dynsymtab[ndx].st_name, symbol_name) == 0 ) 
        {
            expo_d("got entry offset of %s: 0x%x", symbol_name, rel_ent->r_offset);
            break;
        }
        if ( read(fd, rel_ent, sizeof(Elf32_Rel)) != sizeof(Elf32_Rel) ) 
        {
            expo_d("[-] read %s error!", module_path);
            return 0;
        }
    }

    uint32_t offset = rel_ent->r_offset;
    Elf32_Half type = elf_header->e_type;

    free(elf_header);
    free(shstr_shdr);
    free(shstrtab);
    free(shdr);
    free(relplt_shdr);
    free(dynsym_shdr);
    free(dynstr_shdr);
    free(dynstr);
    free(dynsymtab);
    free(rel_ent);

    if ( type == ET_EXEC )
        return offset;
    else if ( type == ET_DYN )
        return offset + module_base;

    return 0;
}

uint32_t expo_hook(const char *module_path, uint32_t hook_func, const char *symbol_name) {

    uint32_t entry_addr = find_got_entry_address(module_path, symbol_name);
    if ( entry_addr == 0 )
        return 0;

    uint32_t original_addr = 0;
    memcpy(&original_addr, (uint32_t *)entry_addr, sizeof(uint32_t));

    expo_d("hook_fun addr: 0x%x", hook_func);
    expo_d("got entry addr: 0x%x", entry_addr);
    expo_d("original addr: 0x%x", original_addr);

    uint32_t page_size = getpagesize();
    uint32_t entry_page_start = PAGE_START(entry_addr, page_size);
    expo_d("page size: 0x%x", page_size);
    expo_d("entry page start: 0x%x", entry_page_start);

    mprotect((uint32_t *)entry_page_start, page_size, PROT_READ | PROT_WRITE);
    memcpy((uint32_t *)entry_addr, &hook_func, sizeof(uint32_t));

    return original_addr;
}


