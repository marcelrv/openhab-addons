/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.miio.internal.robot;

import java.math.BigInteger;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * This DTO class wraps the history record message json structure
 *
 * @author Marcel Verpaalen - Initial contribution
 */
public class HistoryRecordDTO {

    @SerializedName("begin")
    @Expose
    private BigInteger begin;
    @SerializedName("end")
    @Expose
    private BigInteger end;
    @SerializedName("duration")
    @Expose
    private BigInteger duration;
    @SerializedName("area")
    @Expose
    private BigInteger area;
    @SerializedName("clean_time")
    @Expose
    private BigInteger cleanTime;
    @SerializedName("error")
    @Expose
    private BigInteger error;
    @SerializedName("complete")
    @Expose
    private BigInteger complete;
    @SerializedName("start_type")
    @Expose
    private BigInteger startType;
    @SerializedName("clean_type")
    @Expose
    private BigInteger cleanType;
    @SerializedName("finish_reason")
    @Expose
    private BigInteger finishReason;
    @SerializedName("dust_collection_status")
    @Expose
    private BigInteger dustCollectionStatus;

    public final BigInteger getBegin() {
        return begin;
    }

    public final void setBegin(BigInteger begin) {
        this.begin = begin;
    }

    public final BigInteger getEnd() {
        return end;
    }

    public final void setEnd(BigInteger end) {
        this.end = end;
    }

    public final BigInteger getDuration() {
        return duration;
    }

    public final void setDuration(BigInteger duration) {
        this.duration = duration;
    }

    public final BigInteger getArea() {
        return area;
    }

    public final void setArea(BigInteger area) {
        this.area = area;
    }

    public final BigInteger getError() {
        return error;
    }

    public final void setError(BigInteger error) {
        this.error = error;
    }

    public final BigInteger getComplete() {
        return complete;
    }

    public final void setComplete(BigInteger complete) {
        this.complete = complete;
    }

    public final BigInteger getStartType() {
        return startType;
    }

    public final void setStartType(BigInteger startType) {
        this.startType = startType;
    }

    public final BigInteger getCleanType() {
        return cleanType;
    }

    public final void setCleanType(BigInteger cleanType) {
        this.cleanType = cleanType;
    }

    public final BigInteger getFinishReason() {
        return finishReason;
    }

    public final void setFinishReason(BigInteger finishReason) {
        this.finishReason = finishReason;
    }

    public final BigInteger getDustCollectionStatus() {
        return dustCollectionStatus;
    }

    public final void setDustCollectionStatus(BigInteger dustCollectionStatus) {
        this.dustCollectionStatus = dustCollectionStatus;
    }

    public final void setCleanTime(BigInteger cleanTime) {
        this.cleanTime = cleanTime;
    }
}
