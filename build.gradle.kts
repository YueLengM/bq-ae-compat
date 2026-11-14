
import com.gtnewhorizons.retrofuturagradle.minecraft.RunMinecraftTask

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

val guideResourcePackSources = layout.projectDirectory.dir("src/main/resources")

tasks.withType<RunMinecraftTask>().configureEach {
    if (name.startsWith("runClient")) {
        extraJvmArgs.add(
            "-Dguideme.resourcePacks.sources=${guideResourcePackSources.asFile.absolutePath}",
        )
    }
}
