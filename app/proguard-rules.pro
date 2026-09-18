-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

-keep class com.brenninho.trimly.model.** { *; }
-keep class com.brenninho.trimly.data.** { *; }

-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
