package com.garymace.gpsfileparser.service.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.dropwizard.Application;
import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class RestApplication extends Application<Configuration> {
    private final ArrayList<Module> userModules;
    private final Consumer<Bootstrap<Configuration>> bootstrapModifier;

    private RestApplication(ArrayList<Module> userModules, Consumer<Bootstrap<Configuration>> bootstrapModifier) {
        this.userModules = userModules;
        this.bootstrapModifier = bootstrapModifier;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public List<Module> getUserModules() {
        return ImmutableList.copyOf(userModules);
    }

    public void setUserModules(List<Module> modules) {
        this.userModules.clear();
        this.userModules.addAll(modules);
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrapModifier.accept(bootstrap);
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
    }

    @Override
    public void run(String... arguments) throws Exception {
        try {
            super.run(arguments);
        } catch (CreationException e) {
            // Logging is a bust here
            System.out.println("Unable to create Guice Injector:\n" + Throwables.getStackTraceAsString(e));
            onFatalError();
        }
    }


    public static class Builder {
        private final GuiceBundle.Builder<Configuration> bundle;
        private final ArrayList<Module> userModules;
        private Consumer<Bootstrap<Configuration>> bootstrapModifier;

        private Builder() {
            this.bundle = GuiceBundle
                    .defaultBuilder(Configuration.class)
                    .enableGuiceEnforcer(false)
                    .stage(Stage.DEVELOPMENT)
                    .modules(
                            new DefaultModule(),
                            new ConnectJdbcBridgeModule(),
                            new HubSpotConnectModule(),
                            new RodanReporterModule(),
                            new ZkConfigModule(),
                            new StashModule(),
                            new ConfigModule(),
                            new SecCompModule(),
                            new OpenTracingModule()
                    );

            this.userModules = new ArrayList<>();
            this.bootstrapModifier = bootstrap -> {
                bundle.modules(userModules);
                bootstrap.addBundle(bundle.build());
                bootstrap.addBundle(new MultiPartBundle());
                bootstrap.getObjectMapper().registerModule(new Jdk8Module());
                bootstrap.setMetricRegistry(new RestApplicationMetricRegistry());
            };
            injectorFactory(Guice::createInjector);
        }

        public final Builder modules(Module... modules) {
            return modules(Arrays.asList(modules));
        }

        public final Builder modules(Iterable<? extends Module> modules) {
            modules.forEach(userModules::add);
            return this;
        }

        public final Builder bundles(Bundle... bundles) {
            return bootstrapModifier(bootstrap -> Arrays.asList(bundles).forEach(bootstrap::addBundle));
        }

        @SafeVarargs
        public final Builder bundles(ConfiguredBundle<Configuration>... bundles) {
            return bootstrapModifier(bootstrap -> Arrays.asList(bundles).forEach(bootstrap::addBundle));
        }

        public final Builder bootstrapModifier(Consumer<Bootstrap<Configuration>> bootstrapModifier) {
            this.bootstrapModifier = this.bootstrapModifier.andThen(bootstrapModifier);
            return this;
        }

        public final Builder injectorFactory(InjectorFactory injectorFactory) {
            bundle.injectorFactory(new InstrumentedInjectorFactory(injectorFactory));
            return this;
        }

        public final Builder guiceStage(Stage guiceStage) {
            bundle.stage(guiceStage);
            return this;
        }

        public RestApplication build() {
            return new RestApplication(userModules, bootstrapModifier);
        }
    }

    private static class InstrumentedInjectorFactory implements InjectorFactory {
        private final InjectorFactory delegate;

        private InstrumentedInjectorFactory(InjectorFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public Injector create(Stage stage, Module module) {
            try (VireoTimer ignored = Vireo.time("dropwizard.create.injector")) {
                return delegate.create(stage, module);
            }
        }
    }
}