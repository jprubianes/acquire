# Acquire test case scenarios instructions to run

The following project is using selenium framework with java, in addition the project is built also using Frisky-Browser, which is a java wrapper library created on top of selenium Webdriver, more information:
https://github.com/friskysoft/friskybrowser

For the CI, there is a jenkinsfile created that can be use in a jenkins env. This can be done in the configuration section on build configuration and use jenkinsFile

**To run the test from the command line:**

0. Clone the project
1. Install Java 8 and Gradle
2. Run `gradle test`

**Things to be improved:**
0. Add more testing scenarios for deleting, adding users and other scenarios related to CRUD departments
1. Create a docker friendly environment and use a sh script to run the project locally.
2. Add api testing to the current suite

