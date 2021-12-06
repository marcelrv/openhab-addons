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
package org.openhab.binding.miio.internal;

import java.util.Arrays;
import java.util.List;

import org.openhab.binding.miio.internal.cloud.CloudConnector;
import org.openhab.core.io.console.Console;
import org.openhab.core.io.console.extensions.AbstractConsoleCommandExtension;
import org.openhab.core.io.console.extensions.ConsoleCommandExtension;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link MiIoConsoleCommandExtension} class provides additional options through the console command line.
 *
 * @author Marcel Verpaalen - Initial contribution
 */
@Component(service = ConsoleCommandExtension.class, immediate = true, configurationPid = "console.miio")
public class MiIoConsoleCommandExtension extends AbstractConsoleCommandExtension {

    private static final String SUBCMD_SEND = "send";
    private static final String SUBCMD_LOGIN = "login";
    private CloudConnector cloudConnector;

    @Activate
    public MiIoConsoleCommandExtension(@Reference CloudConnector cloudConnector) {
        super("miio", "Xiaomi Cloud Commands.");
        this.cloudConnector = cloudConnector;
    }

    @Override
    public void execute(String[] args, Console console) {
        if (args.length > 0) {
            switch (args[0]) {
                case SUBCMD_SEND:
                    sendCloudRequest(args, console);
                    break;
                case SUBCMD_LOGIN:
                    console.println(String.format("Xiaomi cloud login succeeded %b", cloudConnector.isConnected()));
                    break;
                default:
                    console.println(String.format("Unknown miio sub command '%s'", args[0]));
                    printUsage(console);
                    break;
            }
        } else {
            printUsage(console);
        }
    }

    private void sendCloudRequest(String[] args, Console console) {
        String server = "cn";
        String url = "";
        String parameters = "";
        if (args.length > 1 && args.length < 3) {
            if (args.length == 2) {
                url = args[1];
                parameters = args[2];
            } else {
                server = args[1];
                url = args[2];
                parameters = args[3];
            }
            try {
                console.println(String.format("Sending command (server: '%s') %s - %s", server, url, parameters));
                final String response = cloudConnector.sendCloudCommand(server, url, parameters);
                console.println(response);
            } catch (Exception e) {
                console.println(String.format("Error sending command (server: '%s') %s - %s", server, url, parameters));
            }
        } else {
            console.println("Specify path and content to send.");
        }
    }

    @Override
    public List<String> getUsages() {
        return Arrays.asList(
                new String[] { buildCommandUsage(SUBCMD_SEND + " <path> <json to submit>", "Send to the Xiaomi cloud"),
                        buildCommandUsage(SUBCMD_LOGIN, "Login") });
    }
}
