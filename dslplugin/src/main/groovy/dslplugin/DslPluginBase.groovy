package dslplugin

import org.apache.velocity.runtime.RuntimeConstants
import org.gradle.api.Plugin
import org.gradle.api.Project

class DslPluginBase implements Plugin<Project> {

    /**
     * Convenient way to get access to list of tasks generated by dsl language
     */
    List<DslTaskBase> dslTasks

    @Override
    void apply(Project project) {
        setupDsl(project)

        // Default configure dsl tasks
        configureDslTasks(project)
    }

    /**
     * Sets up the plugin language block
     * @param project
     */
    def setupDsl(Project project) {
        // Create the dsl extension
        Dsl dsl = project.extensions.create('dsl', Dsl)

        // Create velocity inner extension for dsl
        dsl.extensions.create('velocity', VelocityExtension)

        // Add NamedDomainObjectContainer for java configs
        dsl.extensions.add("generate", project.container(DslOperation))
    }

    def configureDslTasks(Project project) {
        dslTasks = new ArrayList<DslTaskBase>()

        project.dsl.generate.all { DslOperation info ->
            def taskName = "dsl${info.name.capitalize()}"

            // Assign property values to task inputs
            DslTaskBase task
            if (info.outputPath) {
                task = project.tasks.create(taskName, DslMultiFileTask) { DslMultiFileTask t ->
                    t.outputPath = info.outputPath
                    t.formatOutput = info.formatOutput
                }
            } else {
                task = project.tasks.create(taskName, DslSingleFileTask) { DslSingleFileTask t ->
                    t.outFile.set info.outFile
                }
            }

            task.group = 'omero'
            task.description = 'parses ome.xml files and compiles velocity template'
            task.velocityProps = createVelocityProperties(project.dsl.velocity)
            task.template = new File(info.template)
            task.omeXmlFiles = info.omeXmlFiles

            // Add dsl task to list of tasks
            dslTasks.add(task)
        }
    }

    static Properties createVelocityProperties(VelocityExtension extension) {
        final def props = new Properties()

        if (extension.loggerClassName) {
            props.setProperty(RuntimeConstants.RUNTIME_LOG_NAME,
                    extension.loggerClassName.get())
        }

        if (extension.resourceLoader) {
            props.setProperty(RuntimeConstants.RESOURCE_LOADER,
                    extension.resourceLoader.get())
        }

        if (extension.fileResourceLoaderPath) {
            props.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_PATH,
                    extension.fileResourceLoaderPath.get())
        }

        if (extension.fileResourceLoaderCache) {
            props.setProperty(RuntimeConstants.FILE_RESOURCE_LOADER_CACHE,
                    extension.fileResourceLoaderCache.get() as String)
        }

        if (extension.resourceLoaderClass) {
            extension.resourceLoaderClass.get().each { String k, String v ->
                props.setProperty(k, v)
            }
        }

        return props
    }

}
