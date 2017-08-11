/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.miio.internal;

import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.openhab.binding.miio.MiIoBindingConstants;

/**
 * MiIO Devices
 *
 * @author Marcel Verpaalen - Initial contribution
 */
public enum MiIoDevices {
    VACUUM("rockrobo.vacuum.v1", "Mi Robot Vacuum", MiIoBindingConstants.THING_TYPE_VACUUM),
    AIR_PURIFIER("zhimi.airpurifier.m1", "Mi Air Purifier", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    AIR_PURIFIER1("zhimi.airpurifier.v1", "Mi Air Purifier v1", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    AIR_PURIFIER2("zhimi.airpurifier.v2", "Mi Air Purifier v2", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    AIR_PURIFIER3("zhimi.airpurifier.v3", "Mi Air Purifier v3", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    AIR_PURIFIER6("zhimi.airpurifier.v6", "Mi Air Purifier v6", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    HUMIDIFIER("zhimi.humidifier.v1", "Mi Humdifier", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    POWERPLUG("chuangmi.plug.m1", "Mi Power-plug", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    POWERPLUG1("chuangmi.plug.v1", "Mi Power-plug v1", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    POWERPLUG2("chuangmi.plug.v2", "Mi Power-plug v2", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    POWERSTRIP("qmi.powerstrip.v1", "Mi Power-strip v1", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    POWERSTRIP2("zimi.powerstrip.v2", "Mi Ppower-strip v2", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    GATEWAY1("lumi.gateway.v1", "Mi Smart Home Gateway 1", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    GATEWAY2("lumi.gateway.v2", "Mi Smart Home Gateway 2", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    GATEWAY3("lumi.gateway.v3", "Mi Smart Home Gateway 3", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    YEELIGHT_L1("yeelink.light.lamp1", "Yeelight", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    YEELIGHT_M1("yeelink.light.mono1", "Yeelight White Bulb", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    YEELIGHT_C1("yeelink.light.color1", "Yeelight Color Bulb", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    TOOTHBRUSH("soocare.toothbrush.x3", "Mi Toothbrush", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    WIFISPEAKER("xiaomi.wifispeaker.v1", "Mi Internet Speaker", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    PHILIPSBULB("philips.light.bulb", "Xiaomi Philips Bulb", MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    PHILIPS("philips.light.sread1", "Xiaomi Philips Eyecare Smart Lamp 2",
            MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    PHILIPS2("philips.light.ceiling", "Xiaomi Philips LED Ceiling Lamp",
            MiIoBindingConstants.THING_TYPE_UNSUPPORTED),
    UNKNOWN("unknown", "Unknown Mi IO Device", MiIoBindingConstants.THING_TYPE_UNSUPPORTED);

    private final String model;
    private final String description;
    private final ThingTypeUID thingType;

    MiIoDevices(String model, String description, ThingTypeUID thingType) {
        this.model = model;
        this.description = description;
        this.thingType = thingType;
    }

    public static MiIoDevices getType(String modelString) {
        for (MiIoDevices mioDev : MiIoDevices.values()) {
            if (mioDev.getModel().equals(modelString)) {
                return mioDev;
            }
        }
        return UNKNOWN;
    }

    public String getModel() {
        return model;
    }

    public String getDescription() {
        return description;
    }

    public ThingTypeUID getThingType() {
        return thingType;
    }

    @Override
    public String toString() {
        return description + " (" + model + ")";
    }
}
