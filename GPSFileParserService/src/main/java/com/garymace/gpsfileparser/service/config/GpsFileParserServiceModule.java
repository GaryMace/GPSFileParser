package com.garymace.gpsfileparser.service.config;

import com.garymace.gpsfileparser.service.resource.GPSFileParserResource;
import com.google.inject.AbstractModule;

public class GpsFileParserServiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(GPSFileParserResource.class);
    }
}
