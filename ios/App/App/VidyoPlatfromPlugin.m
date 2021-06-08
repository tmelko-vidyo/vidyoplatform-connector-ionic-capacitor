#import <Foundation/Foundation.h>
#import <Capacitor/Capacitor.h>

// Define the plugin using the CAP_PLUGIN Macro, and
// each method the plugin supports using the CAP_PLUGIN_METHOD macro.
CAP_PLUGIN(VidyoPlatformPlugin, "VidyoPlatform",
           CAP_PLUGIN_METHOD(openConference, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(closeConference, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(connect, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(disconnect, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(setPrivacy, CAPPluginReturnPromise);
           CAP_PLUGIN_METHOD(cycleCamera, CAPPluginReturnPromise);
)
