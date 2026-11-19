#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <pthread.h>
#include <unistd.h>
#include <string>
#include <cstring>
#include <sys/mman.h>
#include "dobby.h"

#define TAG "SHUVOxMODS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

uintptr_t il2cppBase = 0;

uintptr_t getLibBase(const char* name) {
    char line[512];
    FILE* f = fopen("/proc/self/maps", "r");
    if (!f) return 0;
    uintptr_t addr = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, name) && strstr(line, "r-xp")) {
            addr = (uintptr_t)strtoull(line, nullptr, 16);
            break;
        }
    }
    fclose(f);
    return addr;
}

struct ModFeatures {
    bool godMode       = false;
    bool freezeGranny  = false;
    bool highSpeed     = false;
    bool noGravity     = false;
    bool noBloodScreen = false;
    bool grannyStun    = false;
    bool esp           = false;
    bool silentMove    = false;
} g_mods;

typedef void (*EnemyAIGranny_Update_t)(void*);
EnemyAIGranny_Update_t orig_EnemyAIGranny_Update = nullptr;

void hook_EnemyAIGranny_Update(void* __this) {
    orig_EnemyAIGranny_Update(__this);
    if (!__this) return;
    uintptr_t obj = (uintptr_t)__this;
    if (g_mods.freezeGranny) {
        *(float*)(obj + 0x70) = 0.0f;
        *(float*)(obj + 0x78) = 0.0f;
        *(float*)(obj + 0xD4) = 0.0f;
        *(bool* )(obj + 0x190) = false;
    }
    if (g_mods.grannyStun) {
        *(bool*)(obj + 0x239) = true;
        *(bool*)(obj + 0x23A) = true;
        *(bool*)(obj + 0x23B) = true;
    }
    if (g_mods.godMode) {
        *(bool*)(obj + 0x1B0) = false;
        *(bool*)(obj + 0x1B3) = true;
        *(bool*)(obj + 0x191) = false;
    }
}

typedef void (*FPSController_FixedUpdate_t)(void*);
FPSController_FixedUpdate_t orig_FPS_FixedUpdate = nullptr;

void hook_FPS_FixedUpdate(void* __this) {
    orig_FPS_FixedUpdate(__this);
    if (!__this) return;
    uintptr_t obj = (uintptr_t)__this;
    if (g_mods.highSpeed) {
        *(float*)(obj + 0x40) = 15.0f;
        *(float*)(obj + 0x44) = 12.0f;
        *(float*)(obj + 0x48) = 12.0f;
    }
    if (g_mods.noGravity) {
        *(float*)(obj + 0x4C) = 0.0f;
        *(bool* )(obj + 0x158) = true;
    }
    if (g_mods.godMode) {
        *(bool*)(obj + 0xB8) = false;
    }
}

typedef void (*bloodOnScreen_Update_t)(void*);
bloodOnScreen_Update_t orig_blood_Update = nullptr;

void hook_blood_Update(void* __this) {
    orig_blood_Update(__this);
    if (!__this || !g_mods.noBloodScreen) return;
    *(bool*)((uintptr_t)__this + 0x28) = false;
}

extern "C" {
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setGodMode(JNIEnv*, jclass, jboolean v)       { g_mods.godMode = v;       LOGI("godMode=%d", v); }
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setFreezeGranny(JNIEnv*, jclass, jboolean v)  { g_mods.freezeGranny = v;  LOGI("freezeGranny=%d", v); }
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setHighSpeed(JNIEnv*, jclass, jboolean v)     { g_mods.highSpeed = v;     LOGI("highSpeed=%d", v); }
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setNoGravity(JNIEnv*, jclass, jboolean v)     { g_mods.noGravity = v;     LOGI("noGravity=%d", v); }
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setNoBloodScreen(JNIEnv*, jclass, jboolean v) { g_mods.noBloodScreen = v; LOGI("noBloodScreen=%d", v); }
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setGrannyStun(JNIEnv*, jclass, jboolean v)    { g_mods.grannyStun = v;    LOGI("grannyStun=%d", v); }
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setEsp(JNIEnv*, jclass, jboolean v)           { g_mods.esp = v;           LOGI("esp=%d", v); }
JNIEXPORT void JNICALL Java_com_shuvo_mods_ModService_setSilentMove(JNIEnv*, jclass, jboolean v)    { g_mods.silentMove = v;    LOGI("silentMove=%d", v); }
}

typedef void*     (*il2cpp_domain_get_t)();
typedef void**    (*il2cpp_domain_get_assemblies_t)(void*, size_t*);
typedef void*     (*il2cpp_assembly_get_image_t)(void*);
typedef void*     (*il2cpp_class_from_name_t)(void*, const char*, const char*);
typedef void*     (*il2cpp_class_get_method_from_name_t)(void*, const char*, int);
typedef uintptr_t (*il2cpp_method_get_pointer_t)(void*);

static il2cpp_domain_get_t                 f_domain_get;
static il2cpp_domain_get_assemblies_t      f_domain_assemblies;
static il2cpp_assembly_get_image_t         f_asm_image;
static il2cpp_class_from_name_t            f_class_from_name;
static il2cpp_class_get_method_from_name_t f_method_from_name;
static il2cpp_method_get_pointer_t         f_method_ptr;

static void* findClass(const char* ns, const char* name) {
    size_t cnt = 0;
    void* domain = f_domain_get();
    void** assemblies = f_domain_assemblies(domain, &cnt);
    for (size_t i = 0; i < cnt; i++) {
        void* img = f_asm_image(assemblies[i]);
        if (!img) continue;
        void* klass = f_class_from_name(img, ns, name);
        if (klass) return klass;
    }
    return nullptr;
}

static uintptr_t getMethodPtr(const char* ns, const char* cls, const char* method, int argc) {
    void* klass = findClass(ns, cls);
    if (!klass) { LOGE("class not found: %s", cls); return 0; }
    void* m = f_method_from_name(klass, method, argc);
    if (!m) { LOGE("method not found: %s::%s", cls, method); return 0; }
    return f_method_ptr(m);
}

static void installHooks() {
    void* lib = dlopen("libil2cpp.so", RTLD_NOW);
    if (!lib) { LOGE("dlopen failed: %s", dlerror()); return; }

#define LOAD(var, sym) *(void**)&var = dlsym(lib, sym); if (!var) LOGE("missing: %s", sym);
    LOAD(f_domain_get,       "il2cpp_domain_get")
    LOAD(f_domain_assemblies,"il2cpp_domain_get_assemblies")
    LOAD(f_asm_image,        "il2cpp_assembly_get_image")
    LOAD(f_class_from_name,  "il2cpp_class_from_name")
    LOAD(f_method_from_name, "il2cpp_class_get_method_from_name")
    LOAD(f_method_ptr,       "il2cpp_method_get_pointer")
#undef LOAD

    uintptr_t ptr;
    ptr = getMethodPtr("", "EnemyAIGranny", "Update", 0);
    if (ptr) DobbyHook((void*)ptr, (void*)hook_EnemyAIGranny_Update, (void**)&orig_EnemyAIGranny_Update);

    ptr = getMethodPtr("", "FPSControllerNEW", "FixedUpdate", 0);
    if (ptr) DobbyHook((void*)ptr, (void*)hook_FPS_FixedUpdate, (void**)&orig_FPS_FixedUpdate);

    ptr = getMethodPtr("", "bloodOnScreen", "Update", 0);
    if (ptr) DobbyHook((void*)ptr, (void*)hook_blood_Update, (void**)&orig_blood_Update);

    LOGI("hooks installed");
}

static void* init_thread(void*) {
    sleep(3);
    il2cppBase = getLibBase("libil2cpp.so");
    LOGI("il2cpp base = 0x%lx", il2cppBase);
    if (il2cppBase) installHooks();
    return nullptr;
}

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    pthread_t t;
    pthread_create(&t, nullptr, init_thread, nullptr);
    pthread_detach(t);
    LOGI("SHUVOxMODS loaded");
    return JNI_VERSION_1_6;
}
