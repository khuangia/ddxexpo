// khuangia@gmail.com

#include <stdint.h>
#include "ddx_hooks_impl.h"
#include "expo.h"

static uint32_t ofn_UnitySendMessage = 0;

void fn_UnitySendMessage(char *objName, char *function_code, const char *param) {

    expo_d("UnitySendMessage(%s, %s", objName, function_code);
    if (param) expo_d("%s", param);
    expo_d(")\n");

    typedef void (* pfn_UnitySendMessage)(char *, char *, const char *);

    if (ofn_UnitySendMessage != 0)
        ((pfn_UnitySendMessage)ofn_UnitySendMessage)(objName, function_code, param);
}

bool init_ddx_hooks(JNIEnv *env) {
// context->getPackageCodePath
    const char *unity_so_path = "/data/app/com.efun.android.ddx-1/lib/x86/";

    ofn_UnitySendMessage = expo_hook(unity_so_path, (uint32_t) fn_UnitySendMessage,
                                     "UnitySendMessage");

    return true;
}
