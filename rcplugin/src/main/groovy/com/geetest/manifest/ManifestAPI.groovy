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

package com.geetest.manifest

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.regex.Pattern

import com.geetest.Log

/**
 * @author RePlugin Team
 */
public class ManifestAPI {

    def IManifest sManifestAPIImpl

    def getActivities(Project project, String variantDir) {
        Log.d("getActivities variantDir=${variantDir}")
        if (sManifestAPIImpl == null) {
            sManifestAPIImpl = new ManifestReader(manifestPath(project, variantDir))
        }
        sManifestAPIImpl.activities
    }

    /**
     * 获取 AndroidManifest.xml 路径
     */
    def static manifestPath(Project project, String variantDir) {
        // Compatible with path separators for window and Linux, and fit split param based on 'Pattern.quote'
        def variantDirArray = variantDir.split(Pattern.quote(File.separator))
        Log.d("manifestPath variantDirArray=${variantDirArray}")
        String variantName = ""
        variantDirArray.each {
            //首字母大写进行拼接
            variantName += it.capitalize()
        }
        Log.d("variantName:${variantName}")

        //获取processManifestTask
        def processManifestTask = project.tasks.getByName("process${variantName}Manifest")
        Log.d("manifestPath processManifestTask=${processManifestTask.name}")

        //如果processManifestTask存在的话
        //transform的task目前能保证在processManifestTask之后执行
        if (processManifestTask) {
            File result = null
            //正常的manifest
            File manifestOutputFile = null
            try {
                manifestOutputFile = processManifestTask.getManifestOutputFile()
                Log.d("manifestPath manifestOutputFile=${manifestOutputFile}")
            } catch (Exception e) {
//                manifestOutputFile = new File(processManifestTask.getManifestOutputDirectory(), "AndroidManifest.xml")
                def dir = processManifestTask.getManifestOutputDirectory()
                Log.d("manifestPath dir=${dir}")
                if (dir instanceof File || dir instanceof String) {
                    manifestOutputFile = new File(dir, "AndroidManifest.xml")
                } else {
                    manifestOutputFile = new File(dir.getAsFile().get(), "AndroidManifest.xml")
                }
            }
            if (manifestOutputFile == null) {
                Log.d("manifestPath manifestOutputFile is null")
                throw new GradleException("can't get manifest file")
            }
            //输出路径
            Log.d("manifestPath AndroidManifest.xml 路径:${manifestOutputFile} exist:${manifestOutputFile.exists()}")

            //先设置为正常的manifest
            result = manifestOutputFile

            //最后检测文件是否存在，打印
            if (!result.exists()) {
                Log.d("AndroidManifest.xml not exist")
            }

            return result.absolutePath
        }
        Log.d("manifestPath return empty")
        return ""
    }
}
