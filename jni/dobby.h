#ifndef DOBBY_H
#define DOBBY_H
#ifdef __cplusplus
extern "C" {
#endif
typedef void* DobbyHookEntry;
int DobbyHook(void* address, DobbyHookEntry replace, DobbyHookEntry* origin);
int DobbyDestroy(void* address);
void* DobbySymbolResolver(const char* image_name, const char* symbol_name);
#ifdef __cplusplus
}
#endif
#endif
