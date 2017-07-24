DXA Model Service Container
===
This project is a holder for the Model Service resources. Also needed for development purposes.
 
Explanation
---
In order to be aligned with industry common practices, resources folder should be named `src/main/resources`. 

Since this name is default for Maven projects it's added to JAR file when project is built, we in turn don't want resources to be packed into a JAR because resources should be external for CD Service Containers.

In case the folder is renamed to any other name, we need a special handling of the folder to run it in development (we need to set resources folder explicitly).

If we exclude `src/main/resources` from Maven build, we also get an artifact that doesn't follow industry common practices and also it should be again set explicitly for most IDEs.

So in order to solve all these problems, this artifact was introduced. It has resources in expected place and is dependent on a `dxa-model-service-controller` artifact that allows to
- Skip explicit setting of a resources folder in IDEs;
- Not to pack resources in a resulting JAR;
- Run application in development with this artifact in a classpath so that all resources are available in classpath in runtime too;
- Pack this resources into the assembly since the project's structure is known and fixed.

Running Spring Boot application from IntelliJ IDEA
---
1. Running class is `com.sdl.dxa.DxaModelServiceApplication` from `dxa-model-service-controller`.
2. Use classpath of the `dxa-model-service-container` module.
3. Pass program arguments `--logging.config=dxa-model-service/java/dxa-model-service/dxa-model-service-container/src/main/resources/logback.xml`
4. (optional) If needed override settings from configuration using environment variables.
