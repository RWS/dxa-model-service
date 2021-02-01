SDL Digital Experience Accelerator Model Service
================================================
Build status
------------
- Develop: ![https://github.com/sdl/dxa-model-service/actions?query=workflow%3A%22Java+CI+with+Maven%22](https://github.com/sdl/dxa-model-service/workflows/Java%20CI%20with%20Maven/badge.svg?branch=develop)
- 2.2: ![https://github.com/sdl/dxa-model-service/actions?query=workflow%3A%22Java+CI+with+Maven%22](https://github.com/sdl/dxa-model-service/workflows/Java%20CI%20with%20Maven/badge.svg?branch=release/2.2)

About
-----
The SDL Digital Experience Accelerator (DXA) is a reference implementation of SDL Tridion Sites 9 and SDL Web 8 intended to help you create, design and publish an SDL Tridion/Web-based website quickly.

DXA is available for both .NET and Java web applications. Its modular architecture consists of a framework and example web application, which includes all core SDL Tridion/Web functionality as well as separate Modules for additional, optional functionality.

This repository contains the source code of the legacy DXA Model Service: a separate microservice that can be deployed on an SDL Web CIS backend, version 8 or higher. 

DXA 2.0 introduced the DXA Model Service to improve overall performance and to provide a more lightweight DXA framework in the web application.
As of DXA 2.1 and DXD 11.0, the Model Service functionality is provided by the new GraphQL Content Service (as an extension of DXD 11.0 called the DXA Model Extension). 
SDL continues to provide the standalone DXA Model Service for backward compatibility with SDL Web 8.x and to facilitate rolling upgrades.

The compiled code for the both the legacy, standalone DXA Model Service and the DXA Model Extension is downloadable from the [SDL AppStore](https://appstore.sdl.com/list/?search=dxa) as part of a DXA .NET or Java distributions.


Build
-----

You need Maven 3.2+ to build the Model Service from source. Maven should be available in the system `PATH`. 
    
To build DXA Model Service run the following command:

    mvn install 
    
Note, if you intend to just use the Model Service and not to make changes, you do not need to build it.  

Support
-------
At SDL we take your investment in Digital Experience very seriously, if you encounter any issues with the Digital Experience Accelerator, please use one of the following channels:

- Report issues directly in [this repository](https://github.com/sdl/dxa-model-service/issues)
- Ask questions 24/7 on the SDL Tridion Community at https://tridion.stackexchange.com
- Contact SDL Professional Services for DXA release management support packages to accelerate your support requirements


Documentation
-------------
Documentation can be found online in the SDL documentation portal: https://docs.sdl.com/sdldxa


Repositories
------------
The following repositories with DXA source code are available:

 - https://github.com/sdl/dxa-content-management - CM-side framework (.NET Template Building Blocks)
 - https://github.com/sdl/dxa-html-design - Whitelabel HTML Design
 - https://github.com/sdl/dxa-model-service - Model Service (Java)
 - https://github.com/sdl/dxa-modules - Modules (.NET and Java)
 - https://github.com/sdl/dxa-web-application-dotnet - ASP.NET MVC web application (including framework)
 - https://github.com/sdl/dxa-web-application-java - Java Spring MVC web application (including framework)


Branches and Contributions
--------------------------
We are using the following branching strategy:

 - `master` - Represents the latest stable version. This may be a pre-release version (tagged as `DXA x.y Sprint z`). Updated each development Sprint (approximately bi-weekly).
 - `develop` - Represents the latest development version. Updated very frequently (typically nightly).
 - `release/x.y` - Represents the x.y Release. If hotfixes are applicable, they will be applied to the appropriate release branch so that the branch actually represents the initial release plus hotfixes.

All releases (including pre-releases and hotfix releases) are tagged. 

Note that development sources (on `develop` branch) have dependencies on SNAPSHOT versions of the DXA artifacts, which are available here: https://oss.sonatype.org/content/repositories/snapshots/com/sdl/dxa/

If you wish to submit a Pull Request, it should normally be submitted on the `develop` branch so that it can be incorporated in the upcoming release.

Fixes for severe/urgent issues (that qualify as hotfixes) should be submitted as Pull Requests on the appropriate release branch.

Always submit an issue for the problem, and indicate whether you think it qualifies as a hotfix. Pull Requests on release branches will only be accepted after agreement on the severity of the issue.
Furthermore, Pull Requests on release branches are expected to be extensively tested by the submitter.

Of course, it is also possible (and appreciated) to report an issue without associated Pull Requests.


License
-------
Copyright (c) 2014-2020 SDL Group.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.
