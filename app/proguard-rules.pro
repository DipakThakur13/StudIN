# MediaPipe / ML Kit use JNI and reflection.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**
-dontwarn com.google.auto.value.**
-keep class com.google.mlkit.** { *; }

# LiteRT-LM (on-device Qwen models): the AAR ships no consumer rules, and its native library
# looks up classes, callbacks and fields by name over JNI. Keep the package intact.
-keep class com.google.ai.edge.litertlm.** { *; }
-keep interface com.google.ai.edge.litertlm.** { *; }
-dontwarn com.google.ai.edge.litertlm.**
