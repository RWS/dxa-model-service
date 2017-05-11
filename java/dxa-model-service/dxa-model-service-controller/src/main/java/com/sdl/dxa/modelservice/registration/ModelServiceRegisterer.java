package com.sdl.dxa.modelservice.registration;

import com.sdl.delivery.configuration.Configuration;
import com.sdl.delivery.configuration.ConfigurationException;
import com.sdl.delivery.configuration.XPathConfigurationPath;
import com.sdl.delivery.configuration.xml.XMLConfigurationReaderImpl;
import com.sdl.odata.client.BasicODataClientQuery;
import com.sdl.web.discovery.datalayer.model.ContentServiceCapability;
import com.sdl.web.discovery.datalayer.model.Environment;
import com.sdl.web.discovery.datalayer.model.KeyValuePair;
import com.sdl.web.discovery.registration.ODataClientProvider;
import com.sdl.web.discovery.registration.SecuredODataClient;
import com.sdl.web.discovery.registration.capability.ContentServiceCapabilityBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This bean reads a configuration and tries to register the service as an extension property of ContentServiceCapability.
 */
@Slf4j
@Service
@Conditional(AutoRegistrationCondition.class)
public class ModelServiceRegisterer {

    static final String CONFIG_FILE_NAME = "cd_storage_conf.xml";

    private static final XPathConfigurationPath CONTENT_SERVICE_CAPABILITY_ROLE_XPATH =
            new XPathConfigurationPath("/Roles/Role[@Name=\"ContentServiceCapability\"]");

    private final SecuredODataClient dataClient;

    private final Configuration configuration;

    public ModelServiceRegisterer() throws ConfigurationException {
        configuration = readConfiguration();
        dataClient = new ODataClientProvider(configuration).provideClient();
    }

    private Configuration readConfiguration() throws ConfigurationException {
        return new XMLConfigurationReaderImpl().readConfiguration(CONFIG_FILE_NAME).getConfiguration("ConfigRepository");
    }

    @PostConstruct
    public void register() throws ConfigurationException {
        log.debug("Automatically registering of a Model Service in Content Service Capability");

        Configuration role = this.configuration.getConfiguration(CONTENT_SERVICE_CAPABILITY_ROLE_XPATH);

        Environment environment = loadEnvironment();
        ContentServiceCapability storedCapability = loadStoredCapability();
        log.trace("Loaded an existing capability {}", storedCapability);

        ContentServiceCapability newCapability = loadNewCapability(role, environment);
        log.trace("Loaded a capability from local configuration file {}, {}", CONFIG_FILE_NAME, storedCapability);

        List<KeyValuePair> mergedProperties = mergeExtensionProperties(newCapability, storedCapability);
        log.debug("Merged properties: {}", mergedProperties);

        storedCapability.setExtensionProperties(mergedProperties);
        storedCapability.setEnvironment(environment);
        dataClient.updateEntity(storedCapability);
    }

    private Environment loadEnvironment() {
        return (Environment) dataClient.getEntity(new BasicODataClientQuery.Builder().withEntityType(Environment.class).build());
    }

    private ContentServiceCapability loadStoredCapability() throws ConfigurationException {
        return dataClient.<ContentServiceCapability>getEntities(new BasicODataClientQuery.Builder().withEntityType(ContentServiceCapability.class).build())
                .stream()
                .filter(ContentServiceCapability.class::isInstance)
                .map(ContentServiceCapability.class::cast)
                .findFirst()
                .orElseThrow(() -> new ConfigurationException("Cannot load ContentServiceCapability, so cannot register a Model Service"));
    }

    private ContentServiceCapability loadNewCapability(Configuration role, Environment environment) throws ConfigurationException {
        return (ContentServiceCapability) new ContentServiceCapabilityBuilder().buildCapability(role, environment);
    }

    private List<KeyValuePair> mergeExtensionProperties(ContentServiceCapability newCapability, ContentServiceCapability storedCapability) {
        List<KeyValuePair> newProperties = newCapability.getExtensionProperties();

        Predicate<KeyValuePair> notInNewProperties = knownProp -> newProperties.stream()
                .noneMatch(newProp -> Objects.equals(knownProp.getKey(), newProp.getKey()));

        List<KeyValuePair> knownProperties = storedCapability.getExtensionProperties().stream()
                .filter(notInNewProperties)
                .peek(pair -> log.debug("Property {} is not in MS config, so just leave it as is", pair))
                .collect(Collectors.toList());

        return Stream.of(newProperties, knownProperties).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
