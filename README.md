# Introduction

CATMA is a web based application for text annotation and analysis.

It has two main components: an annotation component that allows the user to annotate text and an analysis component that supports pattern analysis of text and annotations in combination through a custom but easy query language and a data visualization facility based on [Vega](https://vega.github.io/vega/). 

It also allows the project-centric management of text corpora, annotation collections, tagsets and team members. 

CATMA uses a Gitlab instance as its backend to store and manage the project's resources and team. All resources are versioned and available through either the CATMA web interface or the Gitlab REST API.

# Installation & Setup

CATMA uses a stock Gitlab installation with the following mandatory configuration:

The default dev ops pipeline needs to be switched off.
The default branch protection needs to be "partially protected".

The CATMA war file can be build with 
`mvn clean compile package`

CATMA is tested with the jetty servlet container. 

Copy and configure the catma.properties file from the resource folder to the web app root folder.
