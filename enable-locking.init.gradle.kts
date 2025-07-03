// enable-locking.init.gradle.kts  (works on Gradle 8.x)

allprojects {
    // 1. Turn on dependency locking everywhere
    dependencyLocking { lockAllConfigurations() }
}

/**
 * Root-level task that resolves every resolvable configuration in every sub-project,
 * except those that belong to java-platform (BOM) projects or are *DependenciesMetadata.
 *
 * Run it with:  ./gradlew --init-script enable-locking.init.gradle.kts \
 *                         writeLocksForScan --write-locks --no-daemon
 */
gradle.rootProject {
    tasks.register("writeLocksForScan") {
        doLast {
            val skipped = mutableListOf<String>()

            allprojects.forEach { proj ->
                val isBom = proj.plugins.hasPlugin("java-platform")

                proj.configurations
                    .filter { cfg ->
                        cfg.isCanBeResolved &&
                        !isBom &&
                        !cfg.name.endsWith("DependenciesMetadata")
                    }
                    .forEach { cfg ->
                        try {
                            cfg.resolve()               // <- triggers lockfile write
                        } catch (ex: Exception) {
                            logger.warn("⚠️  Skipping ${proj.path}:${cfg.name}: ${ex.message}")
                            skipped += "${proj.path}:${cfg.name}"
                        }
                    }
            }

            if (skipped.isNotEmpty()) {
                logger.lifecycle("Finished; skipped ${skipped.size} configurations that could not be resolved.")
            }
        }
    }
}
