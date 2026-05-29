package org.example.rentathingproba.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;


@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = "cucumber.glue", value = "org.example.rentathingproba.e2e.steps")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty, html:build/reports/cucumber/e2e.html, json:build/reports/cucumber/e2e.json")
@ConfigurationParameter(key = "cucumber.publish.quiet", value = "true")
public class CucumberE2ESuite {
}
