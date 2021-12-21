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
package org.openhab.binding.globalcache.internal;

import static org.openhab.binding.globalcache.internal.GlobalCacheBindingConstants.SUPPORTED_THING_TYPES_UIDS;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.globalcache.internal.handler.GlobalCacheHandler;
import org.openhab.core.net.NetworkAddressService;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link GlobalCacheHandlerFactory} is responsible for creating thing handlers.
 *
 * @author Mark Hilbush - Initial contribution
 */
@NonNullByDefault
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.globalcache")
public class GlobalCacheHandlerFactory extends BaseThingHandlerFactory {

    private final NetworkAddressService networkAddressService;

    @Activate
    public GlobalCacheHandlerFactory(@Reference NetworkAddressService networkAddressService) {
        this.networkAddressService = networkAddressService;
    }

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected @Nullable ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID)) {
            return new GlobalCacheHandler(thing, networkAddressService.getPrimaryIpv4HostAddress());
        }
        return null;
    }
}
