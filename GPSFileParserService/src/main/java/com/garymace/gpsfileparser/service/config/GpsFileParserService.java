package com.garymace.gpsfileparser.service.config;


import io.dropwizard.Application;

public class GpsFileParserService {
    public static void main(String[] args) throws Exception {
        buildRestApplication().run(args);
    }

    private static Application buildRestApplication() {
        return RestApplication.newBuilder()
                .modules(
                        new AutoCloseModule(),
                        ConfiguredCommonAuthModule.newAppAuth(Scope.INTEGRATIONS_ACCESS),
                        new GoToWebinarServiceModule())
                .build();
    }
}
