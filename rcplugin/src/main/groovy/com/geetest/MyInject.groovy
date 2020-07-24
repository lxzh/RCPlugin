package com.geetest;

import javassist.ClassPool
import javassist.CtClass
import javassist.CtConstructor
import org.gradle.api.Project;

public class MyInject {
    private static ClassPool pool = ClassPool.getDefault()

    public static void injectDir(String path, String packageName, Project project, ClassPool mPool) {
        pool.appendClassPath(path)
        pool.appendClassPath(project.android.bootClasspath[0].toString())
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->

                String filePath = file.absolutePath
                // 确保当前文件是class文件，并且不是系统自动生成的class文件
                if (filePath.endsWith(".class")
                        && !filePath.contains('R$')
                        && !filePath.contains('R.class')
                        && !filePath.contains("BuildConfig.class")) {
                    Log.d("filePath:" + filePath);
                    // 判断当前目录是否是在我们的应用包里面
                    int index = filePath.indexOf(packageName);
                    boolean isMyPackage = index != -1;
                    Log.d("isMyPackage:" + isMyPackage);
                    if (isMyPackage) {
                        int end = filePath.length() - 6 // .class = 6
                        String className = filePath.substring(index, end).replace('\\', '.').replace('/', '.')
                        //开始修改class文件
                        Log.d("className:" + className);
                        CtClass c = mPool.getCtClass(className)
                        Log.d("c:" + c + ", superClass=" + c.getSuperclass());
                        if (c.getSuperclass().name.equals("com.geetest.onelogin.activity.OneLoginActivity")) {
                            Log.d("update super class");
                            c.setSuperclass(mPool.get("androidx.fragment.app.FragmentActivity"))
                        }
                        c.writeFile(path)
                        c.detach()
                    }
                }
            }
        }
    }
}