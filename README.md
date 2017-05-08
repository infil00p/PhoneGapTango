PhoneGap Tango PoC Plugin
===========================

This plugin was created during the SFVR meetup on May 30th, 2015.  This is designed to use
the Motion Sensors on the Project Tango Tablet to capture more sensitive data than the standard
tablet PoC.  This only supports Standalone projects, and does not support the CLI due to the fact
that we have to copy in a JAR file.  We hope to fix this soon.

Install the plugin from the project directory:

plugman install --platform android --project . --plugin PhoneGapTango


