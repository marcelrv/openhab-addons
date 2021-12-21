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
package org.openhab.binding.philipstv.internal.pairing;

import static org.openhab.binding.philipstv.internal.ConnectionManager.OBJECT_MAPPER;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.BASE_PATH;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.EMPTY;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.PASSWORD;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.SLASH;
import static org.openhab.binding.philipstv.internal.PhilipsTvBindingConstants.USERNAME;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Formatter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpHost;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.openhab.binding.philipstv.internal.ConnectionManager;
import org.openhab.binding.philipstv.internal.ConnectionManagerUtil;
import org.openhab.binding.philipstv.internal.config.PhilipsTvConfiguration;
import org.openhab.binding.philipstv.internal.pairing.model.AuthDto;
import org.openhab.binding.philipstv.internal.pairing.model.DeviceDto;
import org.openhab.binding.philipstv.internal.pairing.model.FinishPairingDto;
import org.openhab.binding.philipstv.internal.pairing.model.PairingDto;
import org.openhab.binding.philipstv.internal.pairing.model.RequestCodeDto;
import org.openhab.core.config.core.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PhilipsTvPairing} is responsible for the initial pairing process with the Philips TV.
 * The outcome of this one-time pairing is a registered user with password, which will be used for
 * controlling the tv.
 *
 * @author Benjamin Meyer - Initial contribution
 */
public class PhilipsTvPairing {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static String authTimestamp;

    private static String authKey;

    private static String deviceId;

    private final String pairingBasePath = BASE_PATH + "pair" + SLASH;

    public void requestPairingPin(HttpHost target)
            throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        RequestCodeDto requestCodeDto = new RequestCodeDto();
        requestCodeDto.setScope(Stream.of("read", "write", "control").collect(Collectors.toList()));
        requestCodeDto.setDevice(createDeviceSpecification());

        CloseableHttpClient httpClient = ConnectionManagerUtil.createSharedHttpClient(target, EMPTY, EMPTY);
        ConnectionManager connectionManager = new ConnectionManager(httpClient, target);
        String requestCodeJson = OBJECT_MAPPER.writeValueAsString(requestCodeDto);
        String requestPairingCodePath = pairingBasePath + "request";
        logger.debug("Request pairing code with json: {}", requestCodeJson);
        PairingDto pairingDto = OBJECT_MAPPER
                .readValue(connectionManager.doHttpsPost(requestPairingCodePath, requestCodeJson), PairingDto.class);

        authTimestamp = pairingDto.getTimestamp();
        authKey = pairingDto.getAuthKey();

        logger.info("The pairing code is valid for {} seconds.", pairingDto.getTimeout());
    }

    public void finishPairingWithTv(PhilipsTvConfiguration config, Configuration thingConfig, HttpHost target)
            throws NoSuchAlgorithmException, InvalidKeyException, IOException, KeyStoreException,
            KeyManagementException {
        String pairingCode = config.pairingCode;
        FinishPairingDto finishPairingDto = new FinishPairingDto();
        finishPairingDto.setDevice(createDeviceSpecification());

        AuthDto authDto = new AuthDto();
        authDto.setAuthAppId("1");
        authDto.setAuthSignature(calculateRFC2104HMAC(authTimestamp + pairingCode));
        authDto.setAuthTimestamp(authTimestamp);
        authDto.setPin(pairingCode);

        finishPairingDto.setAuth(authDto);
        String grantPairingJson = OBJECT_MAPPER.writeValueAsString(finishPairingDto);

        try (CloseableHttpClient client = ConnectionManagerUtil.createSharedHttpClient(target, deviceId, authKey)) {
            logger.debug("{} and device id: {} and auth_key: {}", grantPairingJson, deviceId, authKey);

            String grantPairingCodePath = pairingBasePath + "grant";
            HttpPost httpPost = new HttpPost(grantPairingCodePath);
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setEntity(new StringEntity(grantPairingJson));

            DigestScheme digestAuth = new DigestScheme();
            // Prevent ERROR logging from HttpAuthenticator
            digestAuth.overrideParamter("realm", "unknown");
            digestAuth.overrideParamter("nonce", "unknown");

            AuthCache authCache = new BasicAuthCache();
            authCache.put(target, digestAuth);

            HttpClientContext localContext = HttpClientContext.create();
            localContext.setAuthCache(authCache);

            try (CloseableHttpResponse response = client.execute(target, httpPost, localContext)) {
                String jsonContent = EntityUtils.toString(response.getEntity());
                logger.debug("----------------------------------------");
                logger.debug("{}", response.getStatusLine());
                logger.debug("{}", jsonContent);
            }

            config.username = deviceId;
            thingConfig.put(USERNAME, deviceId);
            config.password = authKey;
            thingConfig.put(PASSWORD, authKey);
        }
    }

    private String createDeviceId() {
        StringBuilder deviceIdBuilder = new StringBuilder();
        String chars = "abcdefghkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ123456789";
        for (int i = 0; i < 16; i++) {
            int index = (int) Math.floor(Math.random() * chars.length());
            deviceIdBuilder.append(chars.charAt(index));
        }
        return deviceIdBuilder.toString();
    }

    private DeviceDto createDeviceSpecification() {
        DeviceDto deviceDto = new DeviceDto();
        deviceDto.setAppName("ApplicationName");
        deviceDto.setAppId("app.id");
        deviceDto.setDeviceName("heliotrope");
        deviceDto.setDeviceOs("Android");
        deviceDto.setType("native");
        if (deviceId == null) {
            deviceId = createDeviceId();
        }
        deviceDto.setId(deviceId);
        return deviceDto;
    }

    private String toHexString(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }

            return formatter.toString();
        }
    }

    private String calculateRFC2104HMAC(String data)
            throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        String hmacSHA1 = "HmacSHA1";
        // Key used for generated the HMAC signature
        String secretKey = "ZmVay1EQVFOaZhwQ4Kv81ypLAZNczV9sG4KkseXWn1NEk6cXmPKO/MCa9sryslvLCFMnNe4Z4CPXzToowvhHvA==";
        Key signingKey = new SecretKeySpec(Base64.getDecoder().decode(secretKey), hmacSHA1);
        Mac mac = Mac.getInstance(hmacSHA1);
        mac.init(signingKey);
        return Base64.getEncoder().encodeToString(toHexString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8.name())))
                .getBytes(StandardCharsets.UTF_8.name()));
    }
}
