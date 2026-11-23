# Keep the Retrofit interface and the data classes Gson reflects over.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepattributes Signature
-keepattributes *Annotation*

-keep class com.example.communityvoice.data.** { *; }
