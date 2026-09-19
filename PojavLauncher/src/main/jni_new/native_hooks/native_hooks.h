//
// Created by maks on 23.01.2025.
//

#ifndef POJAVLAUNCHER_NATIVE_HOOKS_H
#define POJAVLAUNCHER_NATIVE_HOOKS_H

#include <bytehook.h>
#include <jni.h>

typedef bytehook_stub_t (*bytehook_hook_all_t)(const char *callee_path_name, const char *sym_name, void *new_func,
                                               bytehook_hooked_t hooked, void *hooked_arg);

void create_chmod_hooks(bytehook_hook_all_t bytehook_hook_all_p);
void create_sdl_hooks(bytehook_hook_all_t bytehook_hook_all_p);
void create_sdl_dlopen_hooks(bytehook_hook_all_t bytehook_hook_all_p);
void *sdlDlsymProxy(const char *symbol, void *real);
struct SDL_Window *sdlHookGetPrimaryWindow(void);

/** 1.1.3（移植自 FCL）：把 LWJGL 的 ndlopen 重定向到 libpojavexec，绕过 linker namespace 限制。
 *  必须在**游戏 JVM** 里调用。 */
void installLwjglDlopenHook(JNIEnv *env);

#endif //POJAVLAUNCHER_NATIVE_HOOKS_H
