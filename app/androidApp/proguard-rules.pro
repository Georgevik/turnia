# R8 rules for the release build. The libraries bring their own (kotlinx.serialization, Firebase,
# Koin, Compose); what is here is only what the app itself needs on top.

# Crashlytics deobfuscates stack traces with the mapping file, which needs the line numbers kept.
# The source file name is replaced, so the APK does not carry the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
