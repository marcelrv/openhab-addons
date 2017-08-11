/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miio.handler;

import static org.openhab.binding.miio.MiIoBindingConstants.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.cache.ExpiringCache;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.miio.MiIoBindingConfiguration;
import org.openhab.binding.miio.internal.Message;
import org.openhab.binding.miio.internal.MiIoCommand;
import org.openhab.binding.miio.internal.MiIoDevices;
import org.openhab.binding.miio.internal.MiIoCommunication;
import org.openhab.binding.miio.internal.MiIoCryptoException;
import org.openhab.binding.miio.internal.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link MiIoAbstractHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Marcel Verpaalen - Initial contribution
 */
public abstract class MiIoAbstractHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(MiIoAbstractHandler.class);

    protected ScheduledFuture<?> pollingJob;
    protected MiIoBindingConfiguration configuration;
    protected MiIoDevices miDevice = MiIoDevices.UNKNOWN;
    protected boolean isIdentified;

    protected JsonParser parser;
    protected byte[] token;

    protected MiIoCommunication miioCom;
    protected int lastId;

    protected ExpiringCache<String> network;

    protected final long CACHE_EXPIRY = TimeUnit.SECONDS.toMillis(5);

    public MiIoAbstractHandler(Thing thing) {
        super(thing);
        parser = new JsonParser();
    }

    @Override
    public abstract void handleCommand(ChannelUID channelUID, Command command);

    @Override
    public void initialize() {
        logger.debug("Initializing Mi IO device handler '{}' with thingType {}", getThing().getUID(),
                getThing().getThingTypeUID());
        configuration = getConfigAs(MiIoBindingConfiguration.class);
        if (!tolkenCheckPass(configuration.token)) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Token required. Configure token");
            return;
        }
        isIdentified = false;
        scheduler.schedule(this::initializeData, 0, TimeUnit.SECONDS);
        scheduler.schedule(this::defineDeviceType, 1, TimeUnit.SECONDS);
        int pollingPeriod = configuration.refreshInterval;
        if (pollingPeriod > 0) {
            pollingJob = scheduler.scheduleWithFixedDelay(this::updateData, 2, pollingPeriod, TimeUnit.SECONDS);
            logger.debug("Polling job scheduled to run every {} sec. for '{}'", pollingPeriod, getThing().getUID());
        } else {
            logger.debug("Polling job disabled. for '{}'", getThing().getUID());
        }
    }

    private boolean tolkenCheckPass(String tokenSting) {
        switch (tokenSting.length()) {
            case 16:
                token = tokenSting.getBytes();
                return true;
            case 32:
                if (!IGNORED_TOLKENS.contains(tokenSting)) {
                    token = Utils.hexStringToByteArray(tokenSting);
                    return true;
                }
            default:
                return false;
        }
    }

    @Override
    public void dispose() {
        logger.debug("Disposing Xiaomi MiIO handler '{}'", getThing().getUID());
        if (pollingJob != null) {
            pollingJob.cancel(true);
            pollingJob = null;
        }
        if (miioCom != null) {
            lastId = miioCom.getId();
            miioCom.close();
            miioCom = null;
        }
    }

    protected String sendCommand(MiIoCommand command) {
        return sendCommand(command, "");
    }

    protected String sendCommand(MiIoCommand command, String params) {
        if (!hasConnection()) {
            return null;
        }
        try {
            return getConnection().sendCommand(command, params);
        } catch (MiIoCryptoException | IOException e) {
            logger.debug("Command {} for {} failed (type: {}): {}", command.toString(), getThing().getUID(),
                    getThing().getThingTypeUID(), e.getLocalizedMessage());
        }
        return null;
    }

    /**
     * This is used to execute arbitrary commands by sending to the commands channel. Command parameters to be added
     * between
     * [] brackets. This to allow for unimplemented commands to be executed (e.g. get detailed historical cleaning
     * records)
     *
     * @param command to be executed
     * @return vacuum response
     */
    protected String sendCommand(String command) {
        if (!hasConnection()) {
            return null;
        }
        try {
            command = command.trim();
            String param = "";
            int loc = command.indexOf("[");
            if (loc > 0) {
                param = command.substring(loc + 1, command.length() - 1).trim();
                command = command.substring(0, loc).trim();
            }
            return miioCom.sendCommand(command, param);
        } catch (MiIoCryptoException | IOException e) {
            disconnected(e.getMessage());
        }
        return null;
    }

    protected abstract void updateData();

    protected boolean updateNetwork() {
        String response = network.getValue();
        if (response == null) {
            return false;
        }
        try {
            JsonObject networkData = getJsonResultHelper(response);
            updateState(CHANNEL_SSID, new StringType(networkData.getAsJsonObject("ap").get("ssid").getAsString()));
            updateState(CHANNEL_BSSID, new StringType(networkData.getAsJsonObject("ap").get("bssid").getAsString()));
            updateState(CHANNEL_RSSI, new DecimalType(networkData.getAsJsonObject("ap").get("rssi").getAsLong()));
            updateState(CHANNEL_LIFE, new DecimalType(networkData.get("life").getAsLong()));
            return true;
        } catch (Exception e) {
            logger.debug("Could not parse network response: {}", response);
        }
        return false;
    }

    protected boolean hasConnection() {
        return getConnection() != null;
    }

    protected void disconnectedNoResponse() {
        disconnected("No Response from device");
    }

    protected void disconnected(String message) {
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, message);
        try {
            lastId = miioCom.getId();
            lastId += 10;
        } catch (Exception e) {
            // Ignore
        }
        miioCom = null;
    }

    protected synchronized MiIoCommunication getConnection() {
        if (miioCom != null) {
            return miioCom;
        }
        String deviceId = configuration.deviceId;
        try {
            if (deviceId != null && deviceId.length() == 8 && tolkenCheckPass(configuration.token)) {
                logger.debug("Ping Mi IO device {} at {}", deviceId, configuration.host);
                miioCom = new MiIoCommunication(configuration.host, token, Utils.hexStringToByteArray(deviceId),
                        lastId);
                Message miIoResponse = miioCom.sendPing(configuration.host);
                ;
                if (miIoResponse != null) {
                    logger.debug("Ping response from device {} at {}. Time stamp: {}, OH time {}, delta {}",
                            Utils.getHex(miIoResponse.getDeviceId()), configuration.host, miIoResponse.getTimestamp(),
                            LocalDateTime.now(), miioCom.getTimeDelta());
                    return miioCom;
                }
            } else {
                logger.debug("No device ID defined. Retrieving MiIO device ID");
                MiIoCommunication idCom = new MiIoCommunication(configuration.host, token, new byte[0], lastId);
                Message miIoResponse = idCom.sendPing(configuration.host);
                updateProperty(Thing.PROPERTY_SERIAL_NUMBER, Utils.getSpacedHex(miIoResponse.getDeviceId()));
                Configuration config = editConfiguration();
                config.put(PROPERTY_DID, Utils.getHex(miIoResponse.getDeviceId()));
                updateConfiguration(config);
                logger.debug("Using retrieved MiIO device ID {}", Utils.getHex(miIoResponse.getDeviceId()));
                lastId = idCom.getId();
                idCom.close();
                configuration = getConfigAs(MiIoBindingConfiguration.class);
                if (tolkenCheckPass(configuration.token)) {
                    miioCom = new MiIoCommunication(configuration.host, token, miIoResponse.getDeviceId(), lastId);
                    miioCom.sendPing(configuration.host);
                    return miioCom;
                }
            }
            logger.debug("Ping response from device {} at {} FAILED", configuration.deviceId, configuration.host);
            disconnectedNoResponse();
            return null;
        } catch (IOException e) {
            logger.debug("Could not connect to {} at {}", getThing().getUID().toString(), configuration.host);
            disconnected(e.getMessage());
            return null;
        }
    }

    protected boolean initializeData() {
        this.miioCom = getConnection();
        if (miioCom != null) {
            updateStatus(ThingStatus.ONLINE);
        }
        initalizeNetworkCache();
        return true;
    }

    /**
     * Prepares the ExpiringCache for network data
     */
    protected void initalizeNetworkCache() {
        network = new ExpiringCache<String>(CACHE_EXPIRY, () -> {
            try {
                return sendCommand(MiIoCommand.MIIO_INFO);
            } catch (Exception e) {
                logger.debug("Error during network status refresh: {}", e.getMessage(), e);
            }
            return null;
        });
    }

    protected void defineDeviceType() {
        JsonObject miioInfo = getJsonResultHelper(network.getValue());
        if (miioInfo != null) {
            updateProperties(miioInfo);
            updateThingType(miioInfo);
        }
    }

    private void updateProperties(JsonObject miioInfo) {
        Map<String, String> properties = editProperties();
        properties.put(Thing.PROPERTY_MODEL_ID, miioInfo.get("model").getAsString());
        properties.put(Thing.PROPERTY_FIRMWARE_VERSION, miioInfo.get("fw_ver").getAsString());
        properties.put(Thing.PROPERTY_HARDWARE_VERSION, miioInfo.get("hw_ver").getAsString());
        updateProperties(properties);
    }

    protected boolean updateThingType(JsonObject miioInfo) {
        String model = miioInfo.get("model").getAsString();
        miDevice = MiIoDevices.getType(model);
        if (miDevice.getThingType().equals(getThing().getThingTypeUID())) {
            logger.info("Mi IO model {} identified as: {}. Matches thingtype {}", model, miDevice.toString(),
                    miDevice.getThingType().toString());
            return true;
        } else {
            ThingBuilder thingBuilder = editThing();
            thingBuilder.withLabel(miDevice.getDescription());
            updateThing(thingBuilder.build());
            if (getThing().getThingTypeUID().equals(THING_TYPE_MIIO)) {
                logger.info(
                        "Mi IO Device model {} identified as: {}. Does not matches thingtype {}. Changing thingtype to {}",
                        model, miDevice.toString(), getThing().getThingTypeUID().toString(),
                        miDevice.getThingType().toString());
                changeThingType(MiIoDevices.getType(model).getThingType(), getConfig());
            } else {
                logger.warn(
                        "Mi IO Device model {} identified as: {}. Does not matches thingtype {}. Unexpected, unless intentionally changed.",
                        miDevice.toString(), miDevice.getThingType(), getThing().getThingTypeUID().toString(),
                        miDevice.getThingType().toString());
                return true;
            }
        }
        return false;
    }

    protected JsonObject getJsonResultHelper(String response) {
        try {
            JsonObject result = (JsonObject) parser.parse(response);
            if (result.get("result").getClass().isAssignableFrom(JsonArray.class)) {
                return result.getAsJsonArray("result").get(0).getAsJsonObject();
            } else {
                return result.getAsJsonObject("result");
            }
        } catch (JsonSyntaxException e) {
            logger.debug("Could not parse result from response: '{}'", response);
        } catch (NullPointerException e) {
            logger.trace("Empty response received.");
        }
        return null;
    }
}
