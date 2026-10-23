import com.base.app.convention.AndroidConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/** Cho module JVM thuần (:domain) — không đụng tới Android. */
class KotlinJvmConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.jvm")

        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = AndroidConfig.JAVA_VERSION
            targetCompatibility = AndroidConfig.JAVA_VERSION
        }
        extensions.configure<KotlinProjectExtension> {
            jvmToolchain(AndroidConfig.JVM_TOOLCHAIN)
        }
    }
}
