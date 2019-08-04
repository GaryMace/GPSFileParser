package com.garymace.gpsfileparser.service.config;

import com.hubspot.dropwizard.guice.GuiceBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class GpsFileParserService extends Application<GpsFileParserConfiguration> {
    @Override
    public void initialize(Bootstrap<GpsFileParserConfiguration> bootstrap) {
        GuiceBundle<GpsFileParserConfiguration> guiceBundle = GuiceBundle.<GpsFileParserConfiguration>newBuilder()
                .addModule(new GpsFileParserServiceModule())
                .setConfigClass(GpsFileParserConfiguration.class)
                .enableAutoConfig(getClass().getPackage().getName())
                .build();
        bootstrap.addBundle(guiceBundle);
    }

    @Override
    public void run(GpsFileParserConfiguration gpsFileParserConfiguration, Environment environment) throws Exception {
    }

    public static void main(String[] args) throws Exception {
        new GpsFileParserService().run(args);
    }
}