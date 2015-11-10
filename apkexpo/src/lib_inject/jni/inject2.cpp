
#include "../../main/jni/expo.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <unistd.h>

int main(int argc, char** argv) {

    if (argc != 5) {
        expo_d("libinj: arguments count wrong!\n");
        return -1;
    }

    const char *tar_process_name = argv[1];
    const char *library_path = argv[2];
    const char *func_name = argv[3];
    const char *func_param = argv[4];

    if (!tar_process_name) {
        expo_d("libinj: null tar_process_name\n");
        return -1;
    }

    if (!func_param) {
        expo_d("libinj: null func_param\n");
        return -1;
    }

    if (!library_path) {
        expo_d("libinj: null library_path\n");
        return -1;
    }

    if (!func_name) {
        expo_d("libinj: null func_name\n");
        return -1;
    }

    if (access(library_path, 0) != 0) {
        expo_d("libinj: library path not exists\n");
        return -1;
    }

    pid_t target_pid = expo_get_pid_by_name(tar_process_name);
    if (-1 == target_pid) {
        expo_d("libinj: cannot find this process: %s\n", tar_process_name);
        return -1;
    }

    if (0 != expo_inject_so2pid(target_pid, library_path,
                       func_name, func_param, strlen(func_param))) {
        expo_d("libinj: error occured in injection\n");
        return -1;
    }

    return 0;
}
