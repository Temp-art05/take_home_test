import com.base.app.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * Dùng cho các module :feature:*. Gộp sẵn library + compose + những dependency
 * mà mọi feature đều cần, để build file của feature chỉ còn phần riêng của nó.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("base.android.library")
        pluginManager.apply("base.android.compose")
        // BẮT BUỘC: Destination của ScreenProvider là @Serializable data object/class.
        // Không có plugin này thì annotation vẫn compile nhưng KHÔNG sinh serializer,
        // và app crash lúc dựng NavHost: "Serializer for class 'X' is not found".
        pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

        dependencies {
            add("implementation", project(":core:utils"))
            add("implementation", project(":core:navigation"))
            add("implementation", project(":core:analytics"))
            add("implementation", project(":core:ui-foundation"))
            add("implementation", project(":core:design-system"))
            add("implementation", project(":domain"))

            add("implementation", libs.findLibrary("androidx-compose-material-icons-core").get())
            add("implementation", libs.findLibrary("androidx-activity-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
            add("implementation", libs.findLibrary("androidx-lifecycle-runtime-compose").get())
            add("implementation", libs.findLibrary("androidx-navigation-compose").get())
            add("implementation", libs.findLibrary("kotlinx-serialization-json").get())
            add("implementation", libs.findLibrary("koin-androidx-compose").get())
            add("implementation", libs.findLibrary("timber").get())
        }
    }
}
