# Structure Helpers

Structure Helpers is a library that makes designing jigsaw structures easier and more straightforward. Its basic features include:

- Removing the restriction on jigsaw structures to 160x160x160 blocks.
- Adding new structure data blocks for commonly used structure metadata.
- Ensuring that structure pool elements are placed a certain number of times in each structure.
- Allowing structure pool elements to have sub-elements.
- New structure processors.

Generated code documentation is available per version on the GitHub project pages website.

- [2.0.0](https://kvverti.github.io/structure-helpers/2.0.0/javadoc/)

Usage documentation may be found on the [wiki pages](https://github.com/kvverti/structure-helpers/wiki).

## Dependents

Structure Helpers is available through GitHub releases. JitPack provides a somewhat nice maven interface for these releases.

In `build.gradle`

```groovy
repositories {
  maven { url 'https://jitpack.io' }
}

dependencies {
  modImplementation 'com.github.kvverti:structure-helpers:${structure_helpers_version}'
}
```

where `structure_helpers_version` is your version of choice. Versions may be found under [releases](https://github.com/kvverti/structure-helpers/releases).
