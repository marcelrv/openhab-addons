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
package org.openhab.binding.philipstv.internal.service.model.volume;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@link VolumeDto} class defines the Data Transfer Object
 * for the Philips TV API /audio/volume endpoint.
 *
 * @author Benjamin Meyer - Initial contribution
 */
public class VolumeDto {

    @JsonProperty("current")
    private String currentVolume;

    @JsonProperty
    private boolean muted;

    public String getCurrentVolume() {
        return currentVolume;
    }

    public void setCurrentVolume(String currentVolume) {
        this.currentVolume = currentVolume;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }
}
