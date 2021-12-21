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

import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.THING_TYPE_PHILIPS_TV;

import java.util.Collections;
import java.util.Set;

import org.openhab.binding.philipstv.internal.PhilipsTvDynamicStateDescriptionProvider;
import org.openhab.core.config.discovery.DiscoveryServiceRegistry;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.binding.BaseThingHandlerFactory;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.thing.binding.ThingHandlerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link PhilipsTvHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author Benjamin Meyer - Initial contribution
 */
@Component(service = ThingHandlerFactory.class, configurationPid = "binding.philipstv")
public class PhilipsTvHandlerFactory extends BaseThingHandlerFactory {

    private static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.singleton(THING_TYPE_PHILIPS_TV);

    private DiscoveryServiceRegistry discoveryServiceRegistry;

    private PhilipsTvDynamicStateDescriptionProvider stateDescriptionProvider;

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (THING_TYPE_PHILIPS_TV.equals(thingTypeUID)) {
            return new PhilipsTvHandler(thing, discoveryServiceRegistry, stateDescriptionProvider);
        }

        return null;
    }

    @Reference
    protected void setDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        this.discoveryServiceRegistry = discoveryServiceRegistry;
    }

    protected void unsetDiscoveryServiceRegistry(DiscoveryServiceRegistry discoveryServiceRegistry) {
        this.discoveryServiceRegistry = null;
    }

    @Reference
    protected void setDynamicStateDescriptionProvider(
            PhilipsTvDynamicStateDescriptionProvider stateDescriptionProvider) {
        this.stateDescriptionProvider = stateDescriptionProvider;
    }

    protected void unsetDynamicStateDescriptionProvider(
            PhilipsTvDynamicStateDescriptionProvider stateDescriptionProvider) {
        this.stateDescriptionProvider = null;
    }
}
