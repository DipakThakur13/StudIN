# MediaPipe / ML Kit use JNI and reflection.
-keep class com.google.mediapipe.** { *; }
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.mediapipe.**
-dontwarn com.google.protobuf.**
-dontwarn javax.annotation.**
-dontwarn com.google.auto.value.**
-keep class com.google.mlkit.** { *; }
