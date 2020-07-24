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

import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.JarInput
import com.android.build.api.transform.TransformInput
import com.android.builder.model.AndroidProject
import com.google.common.base.Charsets
import com.google.common.hash.Hashing
import org.apache.commons.io.FileUtils
import org.gradle.api.Project

import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * @author RePlugin Team
 */
public class Util {

    /** 生成 ClassPool 使用的 ClassPath 集合，同时关联的 jar 写入 relativeJars，将要处理的 jar 写入 includeJars */
    def
    static getClassPaths(Project project, Collection<TransformInput> inputs, Set<String> relativeJars, Set<String> includeJars, Map<String, String> map, String variantDir) {
        def classpathList = []

        // 加入本地 android.jar 包
        classpathList.add(project.android.bootClasspath[0].toString())

        // 原始项目中引用的 classpathList
        getProjectClassPath(project, inputs, relativeJars, includeJars, map, variantDir).each {
            classpathList.add(it)
        }

        newSection()
        classpathList
    }

    /** 获取原始项目中的 ClassPath */
    def private static getProjectClassPath(Project project,
                                           Collection<TransformInput> inputs,
                                           Set<String> relativeJars,
                                           Set<String> includeJars,
                                           Map<String, String> map,
                                           String variantDir) {
        def classPath = []
        def visitor = new ClassFileVisitor()
        def projectDir = project.getRootDir().absolutePath
        Log.d("getProjectClassPath projectDir: ${projectDir}")
        String workDir = project.getBuildDir().path +
                File.separator + AndroidProject.FD_INTERMEDIATES + File.separator + "reclass-aar" + File.separator + variantDir
        File file = new File(workDir)
        try {
            file.deleteDir()
        } catch (Exception e) {
            e.printStackTrace()
        }

        inputs.each { TransformInput input ->

            input.directoryInputs.each { DirectoryInput dirInput ->
                def dir = dirInput.file.absolutePath
                classPath << dir

                visitor.setBaseDir(dir)
                Files.walkFileTree(Paths.get(dir), visitor)
            }

            input.jarInputs.each { JarInput jarInput ->
                File jar = jarInput.file
                def jarPath = jar.absolutePath

                if (jarPath.contains("onelogin") || jarPath.contains("fragment")) {
                    Log.d("getProjectClassPath jarPath:${jarPath}")

                    if (!jarPath.contains(projectDir)) {
                        relativeJars << jarPath
                        String jarZipDir = workDir + File.separator + Hashing.sha256().hashString(jarPath, Charsets.UTF_16LE).toString() + File.separator + "class";

                        if (unzip(jarPath, jarZipDir)) {
                            def jarZip = jarZipDir + ".jar"

                            Log.d("getProjectClassPath map->put -> 1")
                            classPath << jarZipDir
                            visitor.setBaseDir(jarZipDir)
                            Files.walkFileTree(Paths.get(jarZipDir), visitor)

                            if (jarPath.contains("onelogin")) {
                                includeJars << jarPath
                            }
                            map.put(jarPath, jarZip)
                        }
                    } else {
                        relativeJars << jarPath

                        map.put(jarPath, jarPath)

                        /* 将 jar 包解压，并将解压后的目录加入 classpath */
                        // Log.d("解压Jar${jarPath}"
                        String jarZipDir = jar.getParent() + File.separatorChar + jar.getName().replace('.jar', '')
                        if (unzip(jarPath, jarZipDir)) {
                            Log.d("getProjectClassPath map->put -> 1")

                            classPath << jarZipDir
                            if (jarPath.contains("onelogin")) {
                                includeJars << jarPath
                            }

                            visitor.setBaseDir(jarZipDir)
                            Files.walkFileTree(Paths.get(jarZipDir), visitor)
                        }

                        // 删除 jar
                        FileUtils.forceDelete(jar)
                    }
                }
            }
        }
        return classPath
    }

    /**
     * 压缩 dirPath 到 zipFilePath
     */
    def static zipDir(String dirPath, String zipFilePath) {
        File dir = new File(dirPath)
        if (dir.exists()) {
            new AntBuilder().zip(destfile: zipFilePath, basedir: dirPath)
        } else {
            Log.d("Zip file is empty! Ignore")
        }
    }

    /**
     * 重新打包jar
     * @param packagePath 将这个目录下的所有文件打包成jar
     * @param destPath 打包好的jar包的绝对路径
     */
    def static void zipJar(String packagePath, String destPath) {
        File file = new File(packagePath)
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(destPath))
        file.eachFileRecurse { File f ->
            String entryName = f.getAbsolutePath().substring(packagePath.length() + 1)
            outputStream.putNextEntry(new ZipEntry(entryName))
            if (!f.directory) {
                InputStream inputStream = new FileInputStream(f)
                outputStream << inputStream
                inputStream.close()
            }
        }
        outputStream.close()
    }

    /**
     * 解压 zipFilePath 到 目录 dirPath
     */
    def private static boolean unzip(String zipFilePath, String dirPath) {
        // 若这个Zip包是空内容的（如引入了Bugly就会出现），则直接忽略
        if (isZipEmpty(zipFilePath)) {
            Log.d("Zip file is empty! Ignore")
            return false;
        }

        new AntBuilder().unzip(src: zipFilePath, dest: dirPath, overwrite: 'true')
        return true;
    }

    /**
     * 获取 App Project 目录
     */
    def static appModuleDir(Project project) {
        appProject(project).projectDir.absolutePath
    }

    /**
     * 获取 App Project
     */
    def static appProject(Project project) {
        def modelName = CommonData.appModule.trim()
        if ('' == modelName || ':' == modelName) {
            project
        }
        project.project(modelName)
    }

    /**
     * 将字符串的某个字符转换成 小写
     *
     * @param str 字符串
     * @param index 索引
     *
     * @return 转换后的字符串
     */
    def public static lowerCaseAtIndex(String str, int index) {
        def len = str.length()
        if (index > -1 && index < len) {
            def arr = str.toCharArray()
            char c = arr[index]
            if (c >= 'A' && c <= 'Z') {
                c += 32
            }

            arr[index] = c
            arr.toString()
        } else {
            str
        }
    }

    def static newSection() {
        50.times {
            print '--'
        }
        println()
    }

    def static boolean isZipEmpty(String zipFilePath) {
        ZipFile z;
        try {
            z = new ZipFile(zipFilePath)
            return z.size() == 0
        } finally {
            z.close();
        }
    }
}
