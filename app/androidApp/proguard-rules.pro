# R8 rules for the release build. The libraries bring their own (kotlinx.serialization, Firebase,
# Koin, Compose); what is here is only what the app itself needs on top.

# Crashlytics deobfuscates stack traces with the mapping file, which needs the line numbers kept.
# The source file name is replaced, so the APK does not carry the original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# AdMob pulls in WorkManager 2.7, and with it Room 2.2.5, whose rule keeps `WorkDatabase_Impl` but not
# its constructor. R8 full mode strips it, Room cannot instantiate the database by reflection, and the
# app crashes on start. Newer Room ships this same rule.
-keep class * extends androidx.room.RoomDatabase { void <init>(); }
