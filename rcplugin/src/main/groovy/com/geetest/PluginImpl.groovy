package com.geetest;

import com.android.build.gradle.AppExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class PluginImpl implements Plugin<Project> {
    @Override
    void apply(Project project) {
        System.out.println("========================================")
        System.out.println("Hello Geetest RebaseClass gradle plugin!")
        System.out.println("========================================")

        def android = project.extensions.findByType(AppExtension)
        android.registerTransform(new RCTransform(project))
    }
}