/*
 * Copyright (C) 2005-2017 Qihoo 360 Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed To in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

package com.geetest

import com.geetest.manifest.ManifestAPI
import javassist.CannotCompileException
import javassist.ClassPool
import javassist.CtClass
import javassist.expr.ExprEditor
import javassist.expr.MethodCall

/**
 * LOADER_ACTIVITY_CHECK_INJECTOR
 *
 * 修改普通的 Activity 为 PluginActivity
 *
 * @author RePlugin Team
 */
public class LoaderActivityInjector extends BaseInjector {

    def private static LOADER_PROP_FILE = 'loader_activities.properties'

    /* LoaderActivity 替换规则 */
    def private static loaderActivityRules = [
            'com.geetest.onelogin.activity.OneLoginActivity': 'androidx.fragment.app.FragmentActivity'
    ]

    @Override
    def injectClass(ClassPool pool, String dir) {
        init()
        Log.d("injectClass init end")
        /* 遍历程序中声明的所有 Activity */
        //每次都new一下，否则多个variant一起构建时只会获取到首个manifest
        new ManifestAPI().getActivities(project, variantDir).each {
            Log.d("injectClass activity:${it}")
            // 处理没有被忽略的 Activity
            if (it in loaderActivityRules.keySet()) {
                handleActivity(pool, it, dir)
            }
        }
        Log.d("injectClass end")
    }

    /**
     * 处理 Activity
     *
     * @param pool
     * @param activity Activity 名称
     * @param classesDir class 文件目录
     */
    private def handleActivity(ClassPool pool, String activity, String classesDir) {
        def activityPath = activity.replace(".", File.separator)
        def clsFilePath = classesDir + File.separatorChar + activityPath + '.class'
        Log.d("handleActivity clsFilePath:${clsFilePath}")
        if (!new File(clsFilePath).exists()) {
            Log.d("handleActivity clsFilePath:${clsFilePath} is not exists")
            return
        }

        Log.d("handleActivity activity=$activity")

        def stream, ctCls
        try {
            stream = new FileInputStream(clsFilePath)
            ctCls = pool.makeClass(stream);
            Log.d("handleActivity ctCls=$ctCls.name")
             // 打印当前 Activity 的所有父类
            CtClass tmpSuper = ctCls.superclass
            def index = 1
            while (tmpSuper != null) {
                Log.d("handleActivity ${index++} 级父类: ${tmpSuper.name}")
                tmpSuper = tmpSuper.superclass
            }

            // ctCls 之前的父类
            def originSuperCls = ctCls.superclass

            /* 从当前 Activity 往上回溯，直到找到需要替换的 Activity */
            def superCls = originSuperCls
            def tmpCls = ctCls
            while (superCls != null) {
                Log.d("向上查找 $superCls.name")
                tmpCls = superCls
                superCls = tmpCls.superclass
                if (ctCls.name in loaderActivityRules.values()) {
                    Log.d("handleActivity 跳过 ${tmpCls.name()}")
                    return
                }
            }
            superCls = originSuperCls
            // 如果 ctCls 已经是 LoaderActivity，则不修改

            /* 找到需要替换的 Activity, 修改 Activity 的父类为 targetSuperClsName */
            if (superCls != null) {
                def targetSuperClsName = loaderActivityRules.get(ctCls.name)
//                def targetSuperClsName1 = getTargetSuperClsName(ctCls.name)
//                Log.d("handleActivity targetSuperClsName1:${targetSuperClsName1}")
                Log.d("handleActivity ${ctCls.name} 的父类 $superCls.name 将替换为 ${targetSuperClsName}")
                if (targetSuperClsName == null || targetSuperClsName.isEmpty()) {
                    return
                }

                CtClass targetSuperCls = pool.getCtClass(targetSuperClsName)
                if (targetSuperCls == null) {
                    return
                }

                if (ctCls.isFrozen()) {
                    ctCls.defrost()
                }
                Log.d("handleActivity setSuperclass ${targetSuperCls.name}")
                ctCls.setSuperclass(targetSuperCls)
//                // 修改声明的父类后，还需要方法中所有的 super 调用。
//                ctCls.getDeclaredMethods().each { outerMethod ->
//                    outerMethod.instrument(new ExprEditor() {
//                        @Override
//                        void edit(MethodCall call) throws CannotCompileException {
//                            if (call.isSuper()) {
//                                if (call.getMethod().getReturnType().getName() == 'void') {
//                                    call.replace('{super.' + call.getMethodName() + '($$);}')
//                                } else {
//                                    call.replace('{$_ = super.' + call.getMethodName() + '($$);}')
//                                }
//                            }
//                        }
//                    })
//                }
                ctCls.writeFile(CommonData.getClassPath(ctCls.name))
                Log.d("handleActivity Replace ${ctCls.name}'s SuperClass ${superCls.name} to ${targetSuperCls.name} finished")
            }

        } catch (Throwable t) {
            Log.d("handleActivity [Error] --> ${t.toString()}")
        } finally {
            if (ctCls != null) {
                ctCls.detach()
            }
            if (stream != null) {
                stream.close()
            }
        }
    }

    def getTargetSuperClsName(String clsName) {
        for (String key: loaderActivityRules.keySet()) {
            def value = loaderActivityRules.get(key)
            Log.d("getTargetSuperClsName key:${key}, value:${loaderActivityRules.get(key)}")
            if(key.equals(clsName)) {
                return value
            }
        }
        return null
    }

    def private init() {
        /* 延迟初始化 loaderActivityRules */
        Log.d("init loaderActivityRules=${loaderActivityRules}")
        // todo 从配置中读取，而不是写死在代码中
        if (loaderActivityRules == null) {
            def buildSrcPath = project.project(':buildsrc').projectDir.absolutePath
            Log.d("init buildSrcPath=${buildSrcPath}")
            def loaderConfigPath = String.join(File.separator, buildSrcPath, 'res', LOADER_PROP_FILE)
            Log.d("init loaderConfigPath=${loaderConfigPath}")
            loaderActivityRules = new Properties()
            new File(loaderConfigPath).withInputStream {
                loaderActivityRules.load(it)
            }

            Log.d("Activity Rules：")
            loaderActivityRules.each {
                Log.d("loaderActivityRules it:${it}")
            }
        }
        Log.d("init end")
    }
}
