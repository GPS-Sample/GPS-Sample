<div align="center">
  <a href="https://www.gpssample.org">
    <img alt="GPSSample logo" src="images/gpssample.png" height="128">
  </a>
  <h1>GPSSample</h1>
</div>

[![Platform](https://img.shields.io/badge/Platform-Android-3B90C4.svg?style=flat)](https://www.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-blue.svg?style=flat)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-GPL%20v3-blue.svg?style=flat)](LICENSE.md)
[![Chat](https://img.shields.io/badge/Chat-on%20Discord-60C53A.svg?style=flat)](https://discord.com/invite/A8EYY9xV)
[![Covenant](https://img.shields.io/badge/Contributor%20Covenant-1.19.24-469C9C.svg?style=flat)](CODE-OF-CONDUCT.pdf)

## About

**What is the GPSSample App?**

GPSSample is an Android-based mobile application to facilitate rapid
statistical sampling and navigation back to selected locations in the
field when conducting household surveys and rapid assessments. The app
is designed to streamline the enumeration and statistical sampling
process and can be used in areas with limited internet and cell phone
connectivity.

**Why now:**  The current process for enumeration and sampling (described below) is inefficient and labor intensive. It is recommended that routine immunization coverage surveys use probability sampling methods and generally use census data derived from enumeration areas as the sampling frame. 

**Existing process:** As many countries lack up-to-date population sampling frames, teams are often sent to map and list households (HHs) in enumeration areas using a set of devices. Upon returning to an area with Wi-Fi, data from each device must be uploaded to a server, collated, and reviewed to ensure that no sections of the enumeration areas were missed during mapping. Once the sampling frame is verified, a sample of HHs can be drawn and another team is deployed to navigate to selected HHs to conduct the coverage survey. 

**Improved process:** By streamlining this enumeration, statistical sampling, and survey process into a single field visit, GPSSample can substantially reduce time and costs associated with conducting coverage surveys, which has broad benefits to ministries of health and public health partners. In GPSSample, teams can collect data used in providing services to the most vulnerable populations such as locations of health facilities and vaccination points and missing villages in hard-to-reach places that are often overlooked in microplanning efforts for immunization services.

**GPSSample's Tiered roles:**

- The **Admin** sets up the configuration for an area, study questions,
  and defines sampling design

- The **Supervisor** defines teams, manages field work, selects sample,
  reviews data from listing and from survey

- The **Enumerator** maps and lists households, collects points of
  interest, syncs data with supervisor

- The **Data Collector** navigates back to selected sample, and
  completes the longer survey

<div>
  <br/>
</div>

**Selected Use Cases:**

1.	**HH surveys:** A district-level HH malaria survey is planned to estimate malaria prevalence with two strata: urban and rural clusters. 10 HHs will be selected in each urban cluster and 14 HHs will be selected each rural cluster. High precision is required as the country is approaching elimination. GPSSample is used to map HHs in clusters and determine eligibility for the HH survey and for an upcoming indoor residual spraying (IRS) campaign. Three teams working in a cluster synchronize their data with the field supervisor who draws the sample in GPSSample the same day the listing is completed. In GPSSample, teams use points of interest and HH listing details collected during enumeration to navigate to HHs selected for the HH survey. A month later, IRS teams use IRS eligibility data to ensure all eligible HHs have been visited.

2.	**Zero dose children:** Children missing the first dose of diphtheria, tetanus, and pertussis (DTP) containing vaccine (“zero dose”) account for an estimated 50% of vaccine-preventable deaths. Zero dose children are challenging to identify since they often reside in urban areas, remote communities, or conflict-affected areas, where population estimates are unreliable. By offering the ability to rapidly conduct enumeration and mapping of targeted areas, GPSSample provides a valuable solution for identifying zero dose children. Through HH listing, GPS coordinates and contextual information can be collected, ensuring precise HH location and incorporating details into digital catchment maps. In conflict zones with restricted team mobility, satellite imagery is used to identify inhabited structures (HHs). Once structures are identified, teams can safely conduct HH listing to gather information on zero dose individuals. GPSSample streamlines the zero-dose listing process, enhances mapping, and facilitates effective offline navigation to efficiently verify vaccination status. This allows the Ministry of Health to plan targeted immunization campaigns for areas with zero dose children. 

What are your ideal use cases for GPSSample? Please reach out and let us know!

**Technical Specifications:** Android 8+ mobile app, handshakes with existing survey tool (ODK, Kobo, or Ona), uses Mapbox basemaps, QR code data transfer, data export to local device, encrypted.

**Sampling Methods:** Simple random sample, cluster-based sample

**Advantages:** Open Source, designed for use in low resource settings, can add clusters in four ways: import GeoJSON files, draw them in the map, walk the boundaries, or tap on the map and specify the cluster size, leverages existing spatial datasets, free, and available in multiple languages (English, French, Spanish, Portuguese, Russian, and Bahasa)

## Training Guides

Visit [https://www.gpssample.org/resources/training-guides](https://www.gpssample.org/resources/training-guides) to view the training guides and to watch videos on using GPSSample.

## Development Environment

* Mac or PC
* Android Studio
  
## Contributing

There are many ways in which you can participate in this project, for example:

* [Submit bugs and feature requests](https://github.com/GPS-Sample/GPS-Sample/issues), and help us verify as they are checked in
* Review [source code changes](https://github.com/GPS-Sample/GPS-Sample/pulls)
* Review the [documentation](https://github.com/GPS-Sample/GPS-Sample/tree/main/Documents) and make pull requests for anything from typos to additional and new content

If you are interested in fixing issues and contributing directly to the code base, please see the document [How to Contribute](How-to-Contribute.md).

## Build Instructions

* Install Android Studio on your PC or Mac
* Clone the repo
* The main branch is master and is read-only on GitHub.  You can build/modify the master branch, but you will not be allowed to push your changes to master to GitHub. See [How to Contribute](How-to-Contribute.md) for instructions.
* Open the project in Android Studio
* If you are using the MapBox map engine:

  Edit the local.properties file (in the root directory of the project) and replace ENTER-YOUR-TOKEN-HERE with your MapBox private token
  
  Edit app/src/main/res/values/mapbox_access_token.xml and replace ENTER-YOUR-MAPBOX-PUBLIC-TOKEN-HERE with your MapBox public token
  
* Sync the project (requires internet) and build the solution
  
## Community

The GPSSample community can be found on [Discord](https://discord.com/invite/A8EYY9xV) where you can ask questions, voice ideas, and share your projects with other people.

Do note that our [Code of Conduct](CODE-OF-CONDUCT.pdf) applies to all GPSSample community channels. Users are **highly encouraged** to read and adhere to them to avoid repercussions.

## Code of Conduct

This project has adopted the [Contributor Covenant Code of Conduct](CODE-OF-CONDUCT.pdf). For more information see the [Code of Conduct FAQ](https://www.contributor-covenant.org/faq/).

## License

Copyright (c) Georgia Tech Research Institute. All rights reserved.

Licensed under the [GPL v3](LICENSE.md) license.

## Security

If you believe you have found a security vulnerability in the GPSSample application, we encourage you to **_responsibly disclose this and NOT open a public issue_**. We will investigate all legitimate reports. Email `gpssample23@gmail.com` to disclose any security vulnerabilities.
