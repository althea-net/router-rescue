/*
 * Mercury-SSH
 * Copyright (C) 2017 Skarafaz
 *
 * This file is part of Mercury-SSH.
 *
 * Mercury-SSH is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Mercury-SSH is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Mercury-SSH.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.althea.routerrescue.manager;

import org.althea.routerrescue.MercuryApplication;

import android.Manifest;
import android.os.Environment;
import android.content.res.AssetManager;

import org.althea.routerrescue.jackson.ServerMapper;
import org.althea.routerrescue.jackson.ValidationException;
import org.althea.routerrescue.model.config.Server;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class ConfigManager {
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    private static final String CONFIG_DIR = "Mercury-SSH";
    private static final String JSON_EXT = "json";
    private static ConfigManager instance;
    private ServerMapper mapper;
    private List<Server> servers;
    private AssetManager assetManager;

    private ConfigManager() {
        mapper = new ServerMapper();
        servers = new ArrayList<>();
        assetManager = MercuryApplication.getContext().getAssets();
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public List<Server> getServers() {
        return servers;
    }

    public LoadConfigFilesStatus loadConfigFiles() {
        servers.clear();

        LoadConfigFilesStatus status = LoadConfigFilesStatus.SUCCESS;

        try {
            servers.add(mapper.readValue(assetManager.open("stable.config")));
            // servers.add(mapper.readValue(assetManager.open("pre-release.config")));
            servers.add(mapper.readValue(assetManager.open("updates.config")));
            servers.add(mapper.readValue(assetManager.open("reboot.config")));
        } catch (IOException | ValidationException e) {
            status = LoadConfigFilesStatus.ERROR;
            logger.error(e.getMessage().replace("\n", " "));
        }

        return status;
    }
}
