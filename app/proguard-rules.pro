# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class de.dgstudios.spinkingdom.**$$serializer { *; }
-keepclassmembers class de.dgstudios.spinkingdom.** { *** Companion; }
-keepclasseswithmembers class de.dgstudios.spinkingdom.** { kotlinx.serialization.KSerializer serializer(...); }

# The game code itself is small: keep it un-obfuscated so enum/serializer lookups and crash reports stay safe.
# Libraries are still shrunk and optimized by R8 via their consumer rules.
-keep class de.dgstudios.spinkingdom.** { *; }
-keepattributes SourceFile,LineNumberTable
