plugins {
    kotlin("js")
//    id("kotlin2js")
//    id("org.jetbrains.kotlin.frontend")
    id("kotlin-dce-js")
}

repositories {
    mavenLocal()
    jcenter()
    maven("https://dl.bintray.com/kotlin/kotlin-js-wrappers")
    maven("https://dl.bintray.com/kotlin/kotlinx")
    mavenCentral()
}

repositories {
    mavenCentral()
}

kotlin {
    target {
        // new kotlin("js") stuff
        useCommonJs()
        browser {
            runTask {
                sourceMaps = true
            }
            webpackTask {
                sourceMaps = true
            }
        }
        compilations.all {
            kotlinOptions {
                sourceMap = true
//                metaInfo = true
                moduleKind = "amd"
                sourceMapEmbedSources = "always"
            }
        }
    }


    sourceSets {
        val main by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))

//                implementation(npm("react", "^16.9.0"))
//                implementation(npm("react-dom", "^16.9.0"))
//                implementation(npm("styled-components", "^4.4.1"))
//                implementation(npm("inline-style-prefixer", "^5.1.0"))
//                implementation(npm("core-js", "^3.4.7"))
//                implementation(npm("css-in-js-utils", "^3.0.2"))
//                implementation(npm("redux", "^4.0.0"))
//                implementation(npm("react-redux", "^5.0.7"))
//
//                val kotlinWrappersVersion = "pre.88-kotlin-1.3.60"
//                implementation("org.jetbrains:kotlin-react:16.9.0-${kotlinWrappersVersion}")
//                implementation("org.jetbrains:kotlin-react-dom:16.9.0-${kotlinWrappersVersion}")
//                implementation("org.jetbrains:kotlin-css:1.0.0-${kotlinWrappersVersion}")
//                implementation("org.jetbrains:kotlin-css-js:1.0.0-${kotlinWrappersVersion}")
//                implementation("org.jetbrains:kotlin-styled:1.0.0-${kotlinWrappersVersion}")
//
//                implementation("org.jetbrains:kotlin-redux:4.0.0-${kotlinWrappersVersion}")
//                implementation("org.jetbrains:kotlin-react-redux:5.0.7-${kotlinWrappersVersion}")

//                implementation(npm("kotlinx-coroutines-core","^1.3.2"))
//                implementation(npm("kotlinx-html","0.6.12"))


                implementation(project(":shared"))

                // temp fix ?
                implementation(npm("text-encoding"))
            }
        }

        val test by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
    }
}


val browserWebpack = tasks.getByName<org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack>("browserWebpack") {

}


val JsJar = tasks.getByName<Jar>("JsJar")

val unzipJsJar = tasks.create<Copy>("unzipJsJar") {
    dependsOn(JsJar)
    group = "build"
    from(zipTree(JsJar.archiveFile))
    into(JsJar.destinationDirectory.file(JsJar.archiveBaseName))
}

//kotlin {
//    target {
//        compilations.all {
//            kotlinOptions {
//                sourceMap = true
////                metaInfo = true
//                moduleKind = "amd"
//                sourceMapEmbedSources = "always"
//            }
//        }
//    }
//
//    sourceSets {
//        val main by getting {
//            dependencies {
//                implementation(kotlin("stdlib-js"))
//                implementation(project(":shared"))
//
//                // temp fix ?
////                implementation(npm("text-encoding"))
//            }
//        }
//
//        val test by getting {
//            dependencies {
//                implementation(kotlin("stdlib-js"))
//            }
//        }
//    }
//}

//kotlinFrontend {
//    sourceMaps = true
//    npm {
//        devDependency("terser")
//    }
//    bundle("webpack", delegateClosureOf<org.jetbrains.kotlin.gradle.frontend.webpack.WebPackExtension> {
////        bundleName = "this-will-be-overwritten" // NOTE: for example purposes this is overwritten in `webpack.config.d/filename.js`.
//        contentPath = file("src/main/resources/web")
//        if (project.hasProperty("prod")) {
//            mode = "production"
//        }
//    })
//}

/*
clean.doFirst() {
    delete("${web_dir}")
}


bundle.doLast() {
    copy {
        from "${buildDir}/resources/main/web"
        from "${buildDir}/bundle"
        into "${web_dir}"
    }
}
 */

