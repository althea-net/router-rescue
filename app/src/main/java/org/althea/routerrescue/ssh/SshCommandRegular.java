/*
 * Mercury-SSH
 * Copyright (C) 2019 Skarafaz
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

package org.althea.routerrescue.ssh;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.UserInfo;
import org.althea.routerrescue.manager.SshManager;
import org.althea.routerrescue.model.config.Command;
import org.althea.routerrescue.model.event.SshCommandConfirm;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Locale;
import java.util.Properties;

public class SshCommandRegular extends SshCommand {
    private static final Logger logger = LoggerFactory.getLogger(SshCommandRegular.class);

    public SshCommandRegular(Command command) {
        super();

        this.host = command.getServer().getHost();
        this.port = command.getServer().getPort();
        this.user = command.getServer().getUser();
        this.password = command.getServer().getPassword();
        this.shellPath = command.getServer().getShellPath();
        this.cmd = command.getCmd();
        this.description = command.getDescription();
        this.confirm = command.getConfirm();
    }

    @Override
    protected boolean beforeExecute() {
        if (confirm) {
            SshCommandDrop<Boolean> drop = new SshCommandDrop<>();
            EventBus.getDefault().postSticky(new SshCommandConfirm(cmd, description, drop));

            if (!drop.take()) {
                return false;
            }
        }

        return super.beforeExecute();
    }

    @Override
    protected boolean initConnection() {
        boolean success = true;
        try {
            jsch.setKnownHosts(SshManager.getInstance().getKnownHosts().getAbsolutePath());
            jsch.addIdentity(SshManager.getInstance().getPrivateKey().getAbsolutePath());
        } catch (IOException | JSchException e) {
            logger.error(e.getMessage().replace("\n", " "));
            success = false;
        }
        return success;
    }

    @Override
    protected UserInfo getUserInfo() {
        return new SshCommandUserInfo();
    }

    @Override
    protected Properties getSessionConfig() {
        Properties config = super.getSessionConfig();
        config.put("PreferredAuthentications", "publickey,password,none");
        config.put("MaxAuthTries", "1");
        return config;
    }

    @Override
    protected String formatCmd(String cmd) {
        return String.format("%s -c \"(%s) &> /dev/null 2>&1\"", shellPath, escapeQuotes(cmd));
    }

    private String formatServerLabel() {
        StringBuilder sb = new StringBuilder(String.format("%s@%s", user, host));
        if (port != 22) {
            sb.append(String.format(Locale.getDefault(), ":%d", port));
        }
        return sb.toString();
    }

    private String escapeQuotes(String str) {
        return str.replace("'", "\'");
    }
}
