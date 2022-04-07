# OpenGAL Plumy
**A [Open GAL](https://github.com/liplum/OpenGAL) compiler.**

## Functions
- Converts source code (.gal) to node language (.node)
- Generate Node Tree by source code (.gal).
## Usages
### Using it in runtime.

Add it in your root build.gradle at the end of repositories:
```groovy
allprojects {  
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```
Add the dependency
```groovy
dependencies {
    implementation 'com.github.liplum:OpenGalPlumy:v0.1'
}
```

### Using it in compile-time

- Build this or use the release.
```shell
# To get help
java -jar PlumyCompiler.jar -help
# To compile
java -jar PlumyCompiler.jar -c xxx.gal -t xxx.node
java -jar PlumyCompiler.jar -c -batch=folder -recursive=true /projects
```