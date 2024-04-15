# Paster

Used in the projects [FastAsyncWorldEdit](https://github.com/IntellectualSites/FastAsyncWorldEdit) and [PlotSquared](https://github.com/IntellectualSites/PlotSquared) as "debugpaste".

### Add the paster to your project:
Releases are published to the central repository, snapshots are published to S01 OSS Sonatype.

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.intellectualsites.paster:Paster:VERSION")
}
```
You need to shade Paster into your software by either using maven shade or gradle shadow.
