/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.philipstv.internal.handler;

import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_BOTTOM_COLOR;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_COLOR;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_HUE_POWER;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_LEFT_COLOR;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_LOUNGE_POWER;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_POWER;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_RIGHT_COLOR;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_STYLE;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_AMBILIGHT_TOP_COLOR;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_APP_ICON;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_APP_NAME;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_BRIGHTNESS;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_CONTRAST;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_KEY_CODE;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_MUTE;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_PLAYER;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_POWER;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_SEARCH_CONTENT;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_SHARPNESS;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_TV_CHANNEL;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.CHANNEL_VOLUME;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.HOST;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.HTTPS;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.MAC_ADDRESS;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.STANDBY;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.TV_NOT_LISTENING_MSG;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.openhab.binding.philipstv.internal.ConnectionManager;
import org.openhab.binding.philipstv.internal.ConnectionManagerUtil;
import org.openhab.binding.philipstv.internal.PhilipsTvDynamicStateDescriptionProvider;
import org.openhab.binding.philipstv.internal.WakeOnLanUtil;
import org.openhab.binding.philipstv.internal.config.PhilipsTvConfiguration;
import org.openhab.binding.philipstv.internal.pairing.PhilipsTvPairing;
import org.openhab.binding.philipstv.internal.service.AmbilightService;
import org.openhab.binding.philipstv.internal.service.AppService;
import org.openhab.binding.philipstv.internal.service.KeyCodeService;
import org.openhab.binding.philipstv.internal.service.PowerService;
import org.openhab.binding.philipstv.internal.service.SearchContentService;
import org.openhab.binding.philipstv.internal.service.TvChannelService;
import org.openhab.binding.philipstv.internal.service.TvPictureService;
import org.openhab.binding.philipstv.internal.service.VolumeService;
import org.openhab.binding.philipstv.internal.service.api.PhilipsTvService;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.StateOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PhilipsTvHandler} is responsible for handling commands, which are sent to one of the
 * channels.
 *
 * @author Benjamin Meyer - Initial contribution
 */
public class PhilipsTvHandler extends BaseThingHandler implements DiscoveryListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public PhilipsTvConfiguration config;

    private DiscoveryServiceRegistry discoveryServiceRegistry;

    private PhilipsTvDynamicStateDescriptionProvider stateDescriptionProvider;

    private ThingUID upnpThingUID;

    private ScheduledFuture<?> refreshScheduler;

    private final Predicate<ScheduledFuture<?>> isRefreshSchedulerRunning = r -> (r != null) && !r.isCancelled();

    private final ReentrantLock lock = new ReentrantLock();

    /* Philips TV services */
    private Map<String, PhilipsTvService> channelServices;

    public PhilipsTvHandler(Thing thing, DiscoveryServiceRegistry discoveryServiceRegistry,
            PhilipsTvDynamicStateDescriptionProvider stateDescriptionProvider) {
        super(thing);

        logger.debug("Create a Philips TV Handler for thing '{}'", thing.getUID());
        config = getConfigAs(PhilipsTvConfiguration.class);
        logger.debug("UPnP discovery enabled: {}", config.useUpnpDiscovery);

        if (config.useUpnpDiscovery && discoveryServiceRegistry != null) {
            logger.debug("Discovery service registry was initialized.");
            this.discoveryServiceRegistry = discoveryServiceRegistry;
        }

        if (stateDescriptionProvider != null) {
            logger.debug("State description was initialized.");
            this.stateDescriptionProvider = stateDescriptionProvider;
        }

        if (!config.useUpnpDiscovery && isSchedulerInitializable()) {
            startRefreshScheduler();
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("Received channel: {}, command: {}", channelUID, command);

        if ((config.username == null) || (config.password == null)) {
            return; // pairing process is not finished
        }

        if ((getThing().getStatus() == ThingStatus.OFFLINE) && (!channelUID.getId().equals(CHANNEL_POWER)
                & !channelUID.getId().equals(CHANNEL_AMBILIGHT_LOUNGE_POWER))) {
            // Check if tv turned on meanwhile
            channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
            if (getThing().getStatus() == ThingStatus.OFFLINE) {
                // still offline
                logger.info(
                        "Cannot execute command {} for channel {}: PowerState of TV was checked and resolved to offline.",
                        command, channelUID.getId());
                return;
            }
        }

        String channel = channelUID.getId();
        long startTime = System.currentTimeMillis();
        // Delegate the other commands to correct channel service
        PhilipsTvService philipsTvService = channelServices.get(channel);

        if (philipsTvService == null) {
            logger.warn("Unknown channel for Philips TV Binding: {}", channel);
            return;
        }

        philipsTvService.handleCommand(channel, command);
        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        logger.trace("The command {} took : {} nanoseconds", command.toFullString(), elapsedTime);
    }

    @Override
    public void initialize() {
        logger.debug("Init of handler for Thing: {}", getThing().getLabel());
        config = getConfigAs(PhilipsTvConfiguration.class);

        if ((config.host == null) || (config.port == null)) {
            postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Cannot connect to Philips TV. Host and/or port are not set.");
            return;
        }

        HttpHost target = new HttpHost(config.host, config.port, HTTPS);

        if ((config.pairingCode == null) && (config.username == null) && (config.password == null)) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Pairing is not configured yet, trying to present a Pairing Code on TV.");
            try {
                initPairingCodeRetrieval(target); // TODO wirft keine Exception wenn URL auf Grund anderer Version nicht
                // gefunden wird
            } catch (IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Error occurred while trying to present a Pairing Code on TV.");
            }
            return;
        } else if ((config.pairingCode != null) && ((config.username == null) || (config.password == null))) {
            updateStatus(ThingStatus.ONLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Pairing Code is available, but credentials missing. Trying to retrieve them.");
            boolean hasFailed = initCredentialsRetrieval(target); // TODO hier fehlt authTimeStamp falls zu lange Zeit
            // vergangen ist - man MUSS von vorne anfangen
            if (hasFailed) {
                postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        "Error occurred during retrieval of credentials.");
                return;
            }
        }

        CloseableHttpClient httpClient;

        try {
            httpClient = ConnectionManagerUtil.createSharedHttpClient(target, config.username, config.password);
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    String.format("Error occurred during creation of HTTP client: %s", e.getMessage()));
            return;
        }

        ConnectionManager connectionManager = new ConnectionManager(httpClient, target);

        if (config.macAddress == null || config.macAddress.isEmpty()) {
            try {
                Optional<String> macAddress = WakeOnLanUtil.getMacFromEnabledInterface(connectionManager);
                if (macAddress.isPresent()) {
                    getConfig().put(MAC_ADDRESS, macAddress.get());
                } else {
                    logger.debug("MAC Address could not be determined for Wake-On-LAN support, "
                            + "because Wake-On-LAN is not enabled on the TV.");
                }
            } catch (IOException e) {
                logger.debug("Error occurred during retrieval of MAC Address: {}", e.getMessage());
            }
        }

        Map<String, PhilipsTvService> services = new HashMap<>();

        PhilipsTvService volumeService = new VolumeService(this, connectionManager);
        services.put(CHANNEL_VOLUME, volumeService);
        services.put(CHANNEL_MUTE, volumeService);

        PhilipsTvService tvPictureService = new TvPictureService(this, connectionManager);
        services.put(CHANNEL_BRIGHTNESS, tvPictureService);
        services.put(CHANNEL_SHARPNESS, tvPictureService);
        services.put(CHANNEL_CONTRAST, tvPictureService);

        PhilipsTvService keyCodeService = new KeyCodeService(this, connectionManager);
        services.put(CHANNEL_KEY_CODE, keyCodeService);
        services.put(CHANNEL_PLAYER, keyCodeService);

        PhilipsTvService appService = new AppService(this, connectionManager);
        services.put(CHANNEL_APP_NAME, appService);
        services.put(CHANNEL_APP_ICON, appService);

        PhilipsTvService ambilightService = new AmbilightService(this, connectionManager);
        services.put(CHANNEL_AMBILIGHT_POWER, ambilightService);
        services.put(CHANNEL_AMBILIGHT_HUE_POWER, ambilightService);
        services.put(CHANNEL_AMBILIGHT_LOUNGE_POWER, ambilightService);
        services.put(CHANNEL_AMBILIGHT_STYLE, ambilightService);
        services.put(CHANNEL_AMBILIGHT_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_LEFT_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_RIGHT_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_TOP_COLOR, ambilightService);
        services.put(CHANNEL_AMBILIGHT_BOTTOM_COLOR, ambilightService);

        services.put(CHANNEL_TV_CHANNEL, new TvChannelService(this, connectionManager));
        services.put(CHANNEL_POWER, new PowerService(this, connectionManager));
        services.put(CHANNEL_SEARCH_CONTENT, new SearchContentService(this, connectionManager));
        channelServices = Collections.unmodifiableMap(services);

        if (discoveryServiceRegistry != null) {
            discoveryServiceRegistry.addDiscoveryListener(this);
        }

        // Thing is initialized, check power state and available communication of the TV and set ONLINE or OFFLINE
        channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
    }

    /**
     * Starts the pairing Process with the TV, which results in a Pairing Code shown on TV.
     */
    private void initPairingCodeRetrieval(HttpHost target)
            throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        logger.info("Pairing code for tv authentication is missing. "
                + "Starting initial pairing process. Please provide manually the pairing code shown on the tv at the configuration of the tv thing.");
        PhilipsTvPairing pairing = new PhilipsTvPairing();
        pairing.requestPairingPin(target);
    }

    private boolean initCredentialsRetrieval(HttpHost target) {
        boolean hasFailed = false;
        logger.info(
                "Pairing code is available, but username and/or password is missing. Therefore we try to grant authorization and retrieve username and password.");
        PhilipsTvPairing pairing = new PhilipsTvPairing();
        try {
            pairing.finishPairingWithTv(config, getConfig(), target);
            postUpdateThing(ThingStatus.ONLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                    "Authentication with Philips TV device was successful. Continuing initialization of the tv.");
        } catch (Exception e) {
            postUpdateThing(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                    "Could not successfully finish pairing process with the TV.");
            logger.warn("Error during finishing pairing process with the TV: {}", e.getMessage(), e);
            hasFailed = true;
        }
        return hasFailed;
    }

    // callback methods for channel services
    public void postUpdateChannel(String channelUID, State state) {
        updateState(channelUID, state);
    }

    public synchronized void postUpdateThing(ThingStatus status, ThingStatusDetail statusDetail, String msg) {
        if (status == ThingStatus.ONLINE) {
            if (msg.equalsIgnoreCase(STANDBY)) {
                updateState(CHANNEL_POWER, OnOffType.OFF);
            } else {
                updateState(CHANNEL_POWER, OnOffType.ON);
            }
            if (isSchedulerInitializable()) { // Init refresh scheduler only, if pairing is completed
                startRefreshScheduler();
            }
        } else if (status == ThingStatus.OFFLINE) {
            updateState(CHANNEL_POWER, OnOffType.OFF);
            if (!TV_NOT_LISTENING_MSG.equals(msg)) { // avoid cancelling refresh if TV is temporarily not available
                if (config.useUpnpDiscovery && isRefreshSchedulerRunning.test(refreshScheduler)) {
                    stopRefreshScheduler();
                }
                // Reset app and channel list (if existing) for new retrieval during next startup
                if (channelServices != null) {
                    ((AppService) channelServices.get(CHANNEL_APP_NAME)).clearAvailableAppList();
                    ((TvChannelService) channelServices.get(CHANNEL_TV_CHANNEL)).clearAvailableTvChannelList();
                }
            }
        }
        updateStatus(status, statusDetail, msg);
    }

    private boolean isSchedulerInitializable() {
        return (config.username != null) && (config.password != null)
                && ((refreshScheduler == null) || refreshScheduler.isDone());
    }

    private void startRefreshScheduler() {
        int configuredRefreshRateOrDefault = Optional.ofNullable(config.refreshRate).orElse(10);
        if (configuredRefreshRateOrDefault > 0) { // If value equals zero, refreshing should not be scheduled
            logger.info("Starting Refresh Scheduler for Philips TV {} with refresh rate of {}.", getThing().getLabel(),
                    configuredRefreshRateOrDefault);
            refreshScheduler = scheduler.scheduleWithFixedDelay(this::refreshTvProperties, 10,
                    configuredRefreshRateOrDefault, TimeUnit.SECONDS);
        }
    }

    private void stopRefreshScheduler() {
        logger.info("Stopping Refresh Scheduler for Philips TV: {}", getThing().getLabel());
        refreshScheduler.cancel(true);
        refreshScheduler = null;
    }

    private void refreshTvProperties() {
        try {
            boolean isLockAcquired = lock.tryLock(1, TimeUnit.SECONDS);
            if (isLockAcquired) {
                try {
                    if (getThing().getStatus() == ThingStatus.OFFLINE || !config.useUpnpDiscovery) {
                        channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
                        if (getThing().getStatus() == ThingStatus.OFFLINE) {
                            return;
                        }
                    }

                    if (isLinked(CHANNEL_VOLUME) || isLinked(CHANNEL_MUTE)) {
                        channelServices.get(CHANNEL_VOLUME).handleCommand(CHANNEL_VOLUME, RefreshType.REFRESH);
                    }

                    if (isLinked(CHANNEL_APP_NAME)) {
                        channelServices.get(CHANNEL_APP_NAME).handleCommand(CHANNEL_APP_NAME, RefreshType.REFRESH);
                    }

                    if (isLinked(CHANNEL_TV_CHANNEL)) {
                        channelServices.get(CHANNEL_TV_CHANNEL).handleCommand(CHANNEL_TV_CHANNEL, RefreshType.REFRESH);
                    }
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            logger.warn("Exception occurred during refreshing the tv properties: {}", e.getMessage());
        }
    }

    public void updateChannelStateDescription(final String channelId, Map<String, String> values) {
        if (isLinked(channelId)) {
            List<StateOption> options = new ArrayList<>();
            values.forEach((key, value) -> options.add(new StateOption(key, value)));
            stateDescriptionProvider.setStateOptions(new ChannelUID(getThing().getUID(), channelId), options);
        }
    }

    @Override
    public void thingDiscovered(DiscoveryService source, DiscoveryResult result) {
        logger.debug("thingDiscovered: {}", result);

        if (config.useUpnpDiscovery && config.host.equals(result.getProperties().get(HOST))) {
            /*
             * Philips TV discovery services creates thing UID from UPnP UDN.
             * When thing is generated manually, thing UID may not match UPnP UDN, so store it for later use (e.g.
             * thingRemoved).
             */
            upnpThingUID = result.getThingUID();
            logger.debug("thingDiscovered, thingUID={}, discoveredUID={}", getThing().getUID(), upnpThingUID);
            channelServices.get(CHANNEL_POWER).handleCommand(CHANNEL_POWER, RefreshType.REFRESH);
        }
    }

    @Override
    public void thingRemoved(DiscoveryService discoveryService, ThingUID thingUID) {
        logger.debug("thingRemoved: {}", thingUID);

        if (thingUID.equals(upnpThingUID)) {
            postUpdateThing(ThingStatus.ONLINE, ThingStatusDetail.NONE, STANDBY);
        }
    }

    @Override
    public Collection<ThingUID> removeOlderResults(DiscoveryService discoveryService, long l,
            Collection<ThingTypeUID> collection, ThingUID thingUID) {
        return Collections.emptyList();
    }

    @Override
    public void dispose() {
        super.dispose();

        if (discoveryServiceRegistry != null) {
            discoveryServiceRegistry.removeDiscoveryListener(this);
        }

        if (isRefreshSchedulerRunning.test(refreshScheduler)) {
            stopRefreshScheduler();
        }
    }
}
