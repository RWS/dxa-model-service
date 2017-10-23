dxa-model-service
===
SDL Digital Experience Accelerator Model Service

About
-----
This repository contains the source code of the DXA Model Service: a separate microservice which is to be deployed on an SDL Web 8 CIS backend.
This DXA Model Service is introduced in DXA 2.0 in order to get better performance (fewer CIS roundtrips) and a more lightweight DXA Framework in the Web Application.

The full DXA Model Service distribution is downloadable from the [SDL Community site](https://community.sdl.com/developers/tridion-developer/m/mediagallery/) (latest version)
or the Releases in GitHub: https://github.com/sdl/dxa-model-service/releases or https://github.com/sdl/dxa-web-application-java/releases (all versions)


Support
---------------
At SDL we take your investment in Digital Experience very seriously, and will do our best to support you throughout this journey. 
If you encounter any issues with the Digital Experience Accelerator, please reach out to us via one of the following channels:

- Report issues directly in [this repository](https://github.com/sdl/dxa-model-service/issues)
- Ask questions 24/7 on the SDL Web Community at https://tridion.stackexchange.com
- Contact Technical Support through the Customer Support Web Portal at https://www.sdl.com/support


Documentation
-------------
Documentation can be found online in the SDL documentation portal:

 - DXA Model Service [Installation](http://docs.sdl.com/LiveContent/content/en-US/SDL%20DXA-v9/GUID-2CF89E5B-D84C-498F-A65A-920EFC26A5A4)
 - DXA Model Service [Configuration](http://docs.sdl.com/LiveContent/content/en-US/SDL%20DXA-v9/GUID-53CC0D55-BD37-4874-A2F9-52F5DA831E13)


Repositories
------------
The following repositories with source code are available:

 - https://github.com/sdl/dxa-model-service - Model Service (Java)


Branches and Contributions
--------------------------
We are using the following branching strategy:

 - `master` - Represents the latest stable version. This may be a pre-release version (tagged as `DXA x.y Sprint z`). Updated each development Sprint (approx. bi-weekly).
 - `develop` - Represents the latest development version. Updated very frequently (typically nightly).
 - `release/x.y` - Represents the x.y Release. If hotfixes are applicable, they will be applied to the appropriate release branch, so that the release branch actually represent the initial release plus hotfixes.

All releases (including pre-releases and hotfix releases) are tagged. 

Note that development sources (on `develop` branch) have dependencies on SNAPSHOT versions of the DXA artifacts, which are available here: https://oss.sonatype.org/content/repositories/snapshots/com/sdl/dxa/

If you wish to submit a Pull Request, it should normally be submitted on the `develop` branch, so it can be incorporated in the upcoming release.

Fixes for really severe/urgent issues (which qualify as hotfixes) should be submitted as Pull Request on the appropriate release branch.

Please always submit an Issue for the problem and indicate whether you think it qualifies as a hotfix; Pull Requests on release branches will only be accepted after agreement on the severity of the issue.
Furthermore, Pull Requests on release branches are expected to be extensively tested by the submitter.

Of course, it's also possible (and appreciated) to report an Issue without associated Pull Requests.


Snapshots
---------
DXA Model service publishes SNAPSHOT versions to Sonatype. If you want to use them, you have to configure `https://oss.sonatype.org/content/repositories/snapshots` as a repository in your Maven settings. Read [this](https://maven.apache.org/settings.html#Repositories) for instructions.

License
-------
Copyright (c) 2014-2017 SDL Group.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and limitations under the License.
