package dslplugin

import ome.dsl.SemanticType
import ome.dsl.velocity.Generator
import ome.dsl.velocity.MultiFileGenerator
import ome.dsl.velocity.SingleFileGenerator
import org.apache.velocity.app.VelocityEngine
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Transformer
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import java.util.function.Function

class DslTask extends DefaultTask {

    @Internal
    @Optional
    Function<SemanticType, String> formatOutput

    /**
     * Set this when you want to generate multiple files
     * Note: also requires setting {@link this.formatOutput}
     */
    @OutputDirectory
    @Optional
    File outputPath

    /**
     * Set this when you only want to generate a single file
     */
    @OutputFile
    @Optional
    File outFile

    /**
     * The .vm Velocity template file we want to use to generate
     * our sources
     */
    @InputFile
    File template

    @Input
    String profile

    @Input
    Properties velocityProperties

    @InputFiles
    FileCollection omeXmlFiles


    def formatOutput(Closure formatter) {
        formatOutput = new Function<SemanticType, String>() {
            @Override
            String apply(SemanticType semanticType) {
                return formatter(semanticType)
            }
        }
        // project.configure(formatOutput, formatter)
    }

    def setFormatOutput(Closure formatter) {
        formatOutput(formatter)
    }


    @TaskAction
    def apply() {
        // Determine which type of file generator to use
        def builder
        if (outputPath) {
            builder = createMultiFileGenerator()
        } else if (outFile) {
            builder = createSingleFileGenerator()
        } else {
            throw new GradleException("If this is a multi file " +
                    "generator, you need to set outputPath. " +
                    "otherwise set outFile")
        }

        // Create and init velocity engine
        VelocityEngine ve = new VelocityEngine()
        ve.init(velocityProperties)

        builder.velocityEngine = ve
        builder.omeXmlFiles = omeXmlFiles.files
        builder.template = template
        builder.profile = profile
        builder.build().run()
    }

    Generator.Builder createMultiFileGenerator() {
        return new MultiFileGenerator.Builder()
                .setOutputDir(outputPath)
                .setFileNameFormatter(formatOutput)
    }

    Generator.Builder createSingleFileGenerator() {
        return new SingleFileGenerator.Builder()
                .setOutFile(outFile)
    }
}
