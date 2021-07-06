# Paster

Used in the projects [FastAsyncWorldEdit](https://github.com/IntellectualSites/FastAsyncWorldEdit) and [PlotSquared](https://github.com/IntellectualSites/PlotSquared) as "debugpaste".

### Add the paster to your project:

```kotlin
repositories {
    maven {
        name = "IntellectualSites"
        url = uri("https://mvn.intellectualsites.com/content/groups/public/")
    }
}

dependencies {
    implementation("com.intellectualsites.paster:Paster:1.0.2-SNAPSHOT")
}
```
You need to shade Paster into your software by either using maven shade or gradle shadow.
