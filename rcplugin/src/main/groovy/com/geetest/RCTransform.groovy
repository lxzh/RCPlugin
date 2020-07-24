package com.geetest;

import com.android.build.api.transform.Context
import com.android.build.api.transform.DirectoryInput
import com.android.build.api.transform.Format
import com.android.build.api.transform.JarInput;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformException;
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation;
import com.android.build.api.transform.TransformOutputProvider
import com.android.build.gradle.internal.pipeline.TransformManager
import javassist.ClassPool
import javassist.NotFoundException
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException;
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.regex.Pattern;

public class RCTransform extends Transform {

    Project project;

    /* 需要处理的 jar 包 */
    def includeJars = [] as Set
    /* 需要处理的 jar 包 */
    def relativeJars = [] as Set
    def map = [:]
    def variantDir = ""

    public RCTransform(Project project) {
        this.project = project;
    }

    @Override
    public String getName() {
        return "RCPlugin";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    // 指定Transform的作用范围
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs,
                   Collection<TransformInput> referencedInputs,
                   TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {
        File rootLocation = null
        try {
            rootLocation = outputProvider.rootLocation
        } catch (Throwable e) {
            //android gradle plugin 3.0.0+ 修改了私有变量，将其移动到了IntermediateFolderUtils中去
            rootLocation = outputProvider.folderUtils.getRootFolder()
        }
        if (rootLocation == null) {
            throw new GradleException("can't get transform root location")
        }
        Log.d("rootLocation: ${rootLocation}")
        // Compatible with path separators for window and Linux, and fit split param based on 'Pattern.quote'
        variantDir = rootLocation.absolutePath.split(getName() + Pattern.quote(File.separator))[1]
        Log.d("variantDir: ${variantDir}")

//        CommonData.appModule = config.appModule

        def injectors = includedInjectors(variantDir)
        Log.d("transform... injectors=" + injectors)
        if (injectors.isEmpty()) {
            Log.d("transform -> copyResult")
            copyResult(inputs, outputProvider) // 跳过 reclass
        } else {
            Log.d("transform -> doTransform")
            doTransform(inputs, outputProvider, injectors) // 执行 reclass
        }
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
    }
/**
     * 返回用户未忽略的注入器的集合
     */
    def includedInjectors(String variantDir) {
        def injectors = []
        Injectors.values().each {
            Log.d("includedInjectors it.injector=" + it.injector)
            //设置project
            it.injector.setProject(project)
            //设置variant关键dir
            it.injector.setVariantDir(variantDir)
            injectors << it.nickName
        }
        injectors
    }
    /**
     * 执行 Transform
     */
    def doTransform(Collection<TransformInput> inputs,
                    TransformOutputProvider outputProvider,
                    def injectors) {
        Log.d("doTransform...")
        /* 初始化 ClassPool */
        Object pool = initClassPool(inputs)

        /* 进行注入操作 */
        Util.newSection()
        Injectors.values().each {
            if (it.nickName in injectors) {
                Log.d("doTransform Do it.nickName:${it.nickName}")
                // 将 NickName 的第 0 个字符转换成小写，用作对应配置的名称
//                def configPre = Util.lowerCaseAtIndex(it.nickName, 0)
                doInject(inputs, pool, it.injector)
            } else {
                Log.d("doTransform Skip it.nickName:${it.nickName}")
            }
        }

//        if (config.customInjectors != null) {
//            config.customInjectors.each {
//                doInject(inputs, pool, it)
//            }
//        }

        /* 重打包 */
        repackage()

        /* 拷贝 class 和 jar 包 */
        copyResult(inputs, outputProvider)

        Util.newSection()
//
//
//        System.out.println(">>>doTransform inputs=" + inputs)
//        // Transform的inputs有两种类型，一种是目录，一种是jar包，要分开遍历
//        inputs.each { TransformInput input ->
//            System.out.println(">>>inputs.each input.directoryInputs=" + input.directoryInputs)
//            //对类型为“文件夹”的input进行遍历
//            input.directoryInputs.each { DirectoryInput directoryInput ->
//                System.out.println(">>>inputs.each directoryInput.file.absolutePath=" + directoryInput.file.absolutePath)
//                //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等
//                MyInject.injectDir(directoryInput.file.absolutePath,"com\\", project, classPool)
//                // 获取output目录
//                def dest = outputProvider.getContentLocation(directoryInput.name,
//                        directoryInput.contentTypes, directoryInput.scopes,
//                        Format.DIRECTORY)
//
//                // 将input的目录复制到output指定目录
//                FileUtils.copyDirectory(directoryInput.file, dest)
//            }
//            //对类型为jar文件的input进行遍历
//            input.jarInputs.each { JarInput jarInput ->
//
//                //jar文件一般是第三方依赖库jar文件
//
//                // 重命名输出文件（同目录copyFile会冲突）
//                def jarName = jarInput.name
//                def md5Name = DigestUtils.md5Hex(jarInput.file.getAbsolutePath())
//                if (jarName.endsWith(".jar")) {
//                    jarName = jarName.substring(0, jarName.length() - 4)
//                }
//                //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等
//                MyInject.injectDir(jarInput.file.absolutePath,"com\\", project, classPool)
//                //生成输出路径
//                def dest = outputProvider.getContentLocation(jarName + md5Name,
//                        jarInput.contentTypes, jarInput.scopes, Format.JAR)
//                //将输入内容复制到输出
//                FileUtils.copyFile(jarInput.file, dest)
//            }
//        }
    }


    /**
     * 拷贝处理结果
     */
    def copyResult(def inputs, def outputs) {
        // Util.newSection()
        Log.d("copyResult...")
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput dirInput ->
                Log.d("copyResult dirInput:${dirInput}")
                copyDir(outputs, dirInput)
            }
            input.jarInputs.each { JarInput jarInput ->
//                if(!(jarInput.file.absolutePath in includeJars)) {
                    Log.d("copyResult jarInput:${jarInput}")
                    copyJar(outputs, jarInput)
//                }
            }
        }
    }

    /**
     * 将解压的 class 文件重新打包，然后删除 class 文件
     */
    def repackage() {
        Util.newSection()
        Log.d("Repackage...")
        relativeJars.each {
            File jar = new File(it)
            String jarAfterZip = map.get(jar.getParent() + File.separatorChar + jar.getName())
            String dirAfterUnzip = jarAfterZip.replace('.jar', '')
            if (it in includeJars) {
                Log.d("压缩目录 $dirAfterUnzip")

                Util.zipDir(dirAfterUnzip, jarAfterZip)
//              Util.zipJar(dirAfterUnzip, jarAfterZip)
            }

            Log.d("删除目录 $dirAfterUnzip")
            FileUtils.deleteDirectory(new File(dirAfterUnzip))
        }
    }

    /**
     * 执行注入操作
     */
    def doInject(Collection<TransformInput> inputs, ClassPool pool, IClassInjector injector) {
        try {
            inputs.each { TransformInput input ->
//                println 'input:' + input
//                input.directoryInputs.each {
////                    println 'handleDir it:' + it.name
//                    handleDir(pool, it, injector)
//                }
                input.jarInputs.each {
                    if(it.name.contains("onelogin")) {
                        Log.d("handleJar start it:${it.name}")
                        handleJar(pool, it, injector)
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace()
            Log.d("doInject t=" + t.toString())
        }
    }

    /**
     * 初始化 ClassPool
     */
    def initClassPool(Collection<TransformInput> inputs) {
        Util.newSection()
        def pool = new ClassPool(true)
        pool.appendSystemPath()
        // 添加编译时需要引用的到类到 ClassPool, 同时记录要引用的 jar 到 relativeJars，记录要修改的 jar 到 includeJars
        Util.getClassPaths(project, inputs, relativeJars, includeJars, map, variantDir).each {
            pool.insertClassPath(it)
        }
        // 加入本地 android 包
        pool.appendClassPath(project.android.bootClasspath[0].toString())
        Log.d("initClassPool bootClasspath:${project.android.bootClasspath[0]}")
        pool
    }

    def initClassPool1(TransformInvocation transformInvocation) {
        Util.newSection()
        def pool = new ClassPool(true)
        Collection<File> files = transformInvocation.get
        transformInvocation.runtimeClasspath.each {
            try {
                pool.appendClassPath(it.absolutePath)
                Log.d("initClassPool1 appendClassPath:${it.absolutePath}")
            } catch (Exception e) {
                Log.d("initClassPool1:${e}")
            }
        }
        pool
    }

    def initClassPool2(List<JarInput> jarInputs, List<DirectoryInput> directoryInputs) {
        Log.d("initClassPool2")
        FieldUtils.writeStaticField(ClassPool.class, "defaultPool", null, true);

        final ClassPool pool = ClassPool.getDefault();
        pool.appendSystemPath()
        // 加入本地 android 包
        pool.appendClassPath(project.android.bootClasspath[0].toString())
        try {
            for (JarInput jarInput : jarInputs) {
                pool.insertClassPath(jarInput.getFile().getAbsolutePath());
            }
            for (DirectoryInput directoryInput : directoryInputs) {
                pool.appendClassPath(directoryInput.getFile().getAbsolutePath());
            }
        } catch (NotFoundException e) {
            throw new StopExecutionException(e.getMessage());
        }
        Log.d("transform init pool success")
        pool
    }
//
//    def getInputCollections(TransformInvocation transformInvocation, Project project) {
//        def inputFiles  = []
//        transformInvocation.getInputs().each {
//            Log.d("getInputCollections 1 :${it.jarInputs}"
//            Log.d("getInputCollections 2 :${it.metaClass}"
//            inputFiles << it.jarInputs
//        }
//        transformInvocation.getReferencedInputs().each {
//            Log.d("getInputCollections 3 :${it.jarInputs}"
//            Log.d("getInputCollections 4 :${it.metaClass}"
//            inputFiles << it.jarInputs
//        }
//        project.getAndroid<BaseExtension>.bootClasspath
//    }

    /**
     * 处理 jar
     */
    def handleJar(ClassPool pool, JarInput input, IClassInjector injector) {
        File jar = input.file
        if (jar.absolutePath in includeJars) {
            Log.d("handleJar: absolutePath:${jar.absolutePath}, ${jar.name}")
            String dirAfterUnzip = map.get(jar.getParent() + File.separatorChar + jar.getName()).replace('.jar', '')
            Log.d("handleJar: dirAfterUnzip:${dirAfterUnzip}")
            injector.injectClass(pool, dirAfterUnzip)
        }
    }

    /**
     * 拷贝 Jar
     */
    def copyJar(TransformOutputProvider output, JarInput input) {
        File jar = input.file
        // 替换名称，拷贝处理后的 jar 到目标目录
        String jarPath = map.get(jar.absolutePath);
        if (jarPath != null) {
            jar = new File(jarPath)
        }

        // 解压但未处理的包，仍旧拷贝原始包
        if (jarPath != null && !jar.exists()) {
            jar = input.file
        }

        if(!jar.exists()){
            return
        }

        String destName = input.name
        def hexName = DigestUtils.md5Hex(jar.absolutePath)
        if (destName.endsWith('.jar')) {
            destName = destName.substring(0, destName.length() - 4)
        }
        File dest = output.getContentLocation(destName + '_' + hexName, input.contentTypes, input.scopes, Format.JAR)
        Log.d("copyJar jar:${jar} dest:${dest}")
        FileUtils.copyFile(jar, dest)

/*
        def path = jar.absolutePath
        if (path in CommonData.relativeJars) {
            Log.d("拷贝Jar ${path} 到 ${dest.absolutePath}"
        }
*/
    }

    /**
     * 处理目录中的 class 文件
     */
    def handleDir(ClassPool pool, DirectoryInput input, IClassInjector injector) {
        Log.d("handleDir: ${input.file.absolutePath}")
        injector.injectClass(pool, input.file.absolutePath)
    }

    /**
     * 拷贝目录
     */
    def copyDir(TransformOutputProvider output, DirectoryInput input) {
        File dest = output.getContentLocation(input.name, input.contentTypes, input.scopes, Format.DIRECTORY)
        Log.d("copyDir dest:${dest}")
        FileUtils.copyDirectory(input.file, dest)
//        Log.d("拷贝目录 ${input.file.absolutePath} 到 ${dest.absolutePath}")
    }
}