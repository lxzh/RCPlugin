package com.geetest

import com.android.sdklib.IAndroidTarget
import org.gradle.api.Project;

/**
 * @author hyongbai
 */
class ScopeCompat {
    static def getAdbExecutable(def scope) {
        final MetaClass scopeClz = scope.metaClass
        if (scopeClz.hasProperty(scope, "androidBuilder")) {
            return scope.androidBuilder.sdkInfo.adb
        }
        if (scopeClz.hasProperty(scope, "sdkComponents")) {
            return scope.sdkComponents.adbExecutableProvider.get()
        }
    }

    // TODO: getBuilderTarget
//    static def getBuilderTarget(def scope, def target){
//        final MetaClass scopeClz = scope.metaClass
//
//        if (scopeClz.hasProperty(scope, "androidBuilder")) {
//            return scope.getAndroidBuilder().getTarget().getPath(target) //IAndroidTarget.ANDROID_JAR
//        }
//
//        return globalScope.getAndroidBuilder().getTarget().getPath(IAndroidTarget.ANDROID_JAR)
//    }

    static def getAndroidJar(def scope){
        final MetaClass scopeClz = scope.metaClass
        Log.d("getAndroidJar scopeClz=${scopeClz}")
        if (scopeClz.hasProperty(scope, "androidBuilder")) {
            return scope.getAndroidBuilder().getTarget().getPath(IAndroidTarget.ANDROID_JAR)
        }
        if (scopeClz.hasProperty(scope, "sdkComponents")) {
            return scope.sdkComponents.androidJarProvider.get().getAbsolutePath()
        }
    }

    static def getAndroidJar(Project project , def api) {
        def rootDir = project.rootDir
        def localProperties = File(rootDir, "local.properties")
        def sdkDir
        if (localProperties.exists()) {
            def properties = Properties()
            localProperties.inputStream().use { properties.load(it) }
            sdkDir = properties.getProperty("sdk.dir")
        } else {
            sdkDir = System.getenv("ANDROID_HOME")
        }
        return File("$sdkDir/platforms/android-$api/android.jar")
    }
}