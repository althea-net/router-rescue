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

package it.skarafaz.mercury.ssh;

import android.content.res.AssetManager;

import com.jcraft.jsch.*;

import it.skarafaz.mercury.MercuryApplication;
import it.skarafaz.mercury.model.event.SshCommandEnd;
import it.skarafaz.mercury.model.event.SshCommandStart;

import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.io.ByteArrayOutputStream;
import java.util.Date;

public abstract class SshCommand extends Thread {
    protected static final int TIMEOUT = 30000;
    private static final Logger logger = LoggerFactory.getLogger(SshCommand.class);
    protected JSch jsch;
    protected Session session;
    protected String host;
    protected Integer port;
    protected String user;
    protected String password;
    protected String shellPath;
    protected String cmd;
    protected String description;
    protected Boolean confirm;

    public SshCommand() {
        this.jsch = new JSch();
    }

    @Override
    public void run() {
        if (beforeExecute()) {
            SshCommandStatus status = execute();
            afterExecute(status);
        }
    }

    protected boolean beforeExecute() {
        EventBus.getDefault().postSticky(new SshCommandStart());
        return true;
    }

    private SshCommandStatus execute() {
        SshCommandStatus status = SshCommandStatus.COMMAND_SENT;

        if (initConnection()) {
            if (connect()) {
                if (!send(formatCmd(cmd))) {
                    status = SshCommandStatus.EXECUTION_FAILED;
                }
                disconnect();
            } else {
                status = SshCommandStatus.CONNECTION_FAILED;
            }
        } else {
            status = SshCommandStatus.CONNECTION_INIT_ERROR;
        }

        return status;
    }

    protected void afterExecute(SshCommandStatus status) {
        EventBus.getDefault().postSticky(new SshCommandEnd(status));
    }

    protected boolean initConnection() {
        return true;
    }

    protected boolean connect() {
        boolean success = true;
        try {
            session = jsch.getSession(user, host, port);

            session.setUserInfo(getUserInfo());
            session.setConfig(getSessionConfig());
            session.setPassword(password);

            session.connect(TIMEOUT);
        } catch (JSchException e) {
            logger.error(e.getMessage().replace("\n", " "));
            success = false;
        }
        return success;
    }

    byte[] convert(InputStream in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            while (true) {
                int val = in.read();
                out.write(val);
            }
        } catch (IOException e) {
            logger.debug("end of file found!");
        }
        return out.toByteArray();
    }

    boolean copyCommand(String name, Boolean preserve_state) {
        logger.debug("Detected firmware upload!");
        AssetManager assetManager = MercuryApplication.getContext().getAssets();
        try {
            boolean ptimestamp = true;
            String lfile = name;

            // exec 'scp -t rfile' remotely
            String rfile = "/tmp/sysupgrade.bin";
            rfile = rfile.replace("'", "'\"'\"'");
            rfile = "'" + rfile + "'";
            String command = "scp " + (ptimestamp ? "-p" : "") + " -t " + rfile;
            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out = channel.getOutputStream();
            InputStream in = channel.getInputStream();


            channel.connect();

            if (checkAck(in) != 0) {
                System.exit(0);
            }


            Date date = new Date();
            //This method returns the time in millis
            long now = date.getTime();
            if (ptimestamp) {
                command = "T " + (now / 1000) + " 0";
                // The access time should be sent here,
                // but it is not accessible with JavaAPI ;-<
                command += (" " + (now / 1000) + " 0\n");
                out.write(command.getBytes());
                out.flush();
                if (checkAck(in) != 0) {
                    System.exit(0);
                }
            }

            // send "C0644 filesize filename", where filename should not include '/'
            // get a file handle, available may be an inaccurate size count.
            InputStream fis = assetManager.open(lfile);
            int filesize = fis.available();
            command = "C0644 " + filesize + " ";
            if (lfile.lastIndexOf('/') > 0) {
                command += lfile.substring(lfile.lastIndexOf('/') + 1);
            } else {
                command += lfile;
            }
            command += "\n";
            out.write(command.getBytes());
            out.flush();
            if (checkAck(in) != 0) {
                System.exit(0);
            }

            byte[] buf = new byte[1024];
            while (true) {
                int len = fis.read(buf, 0, buf.length);
                if (len <= 0) break;
                out.write(buf, 0, len); //out.flush();
            }
            fis.close();
            fis = null;

            // send '\0'
            buf[0] = 0;
            out.write(buf, 0, 1);
            out.flush();
            if (checkAck(in) != 0) {
                return false;
            }
            out.close();

            channel.disconnect();
        } catch (Exception e) {
            logger.error(String.valueOf(e));
            return false;
        }
        logger.debug("Finished copying, running upgrade command");

        String sysupgrade_args;

        if(preserve_state) {
            sysupgrade_args = "-c -v";
        }
        else {
            sysupgrade_args="-n -v";
        }
        String sysupgrade_cmd = String.format("sysupgrade %s /tmp/sysupgrade.bin", sysupgrade_args);


        String upgrade_cmd = String.format("%s -c \"(%s) &> /dev/null 2>&1\"", shellPath, sysupgrade_cmd);
        return normalCommand(upgrade_cmd);
    }


    static int checkAck(InputStream in) throws IOException {
        int b = in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if (b == 0) return b;
        if (b == -1) return b;

        if (b == 1 || b == 2) {
            StringBuffer sb = new StringBuffer();
            int c;
            do {
                c = in.read();
                sb.append((char) c);
            }
            while (c != '\n');
            if (b == 1) { // error
                logger.error("error in scp!");
                logger.error(sb.toString());
            }
            if (b == 2) { // fatal error
                logger.error("Fatal error in scp!");
                logger.error(sb.toString());
            }
        }
        return b;
    }


    boolean normalCommand(String cmd) {
        logger.debug("Running command: {}", cmd);
        ChannelExec channel = null;

        boolean success = true;
        try {
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(cmd);
            channel.setInputStream(null);

            InputStream stdout = channel.getInputStream();
            InputStream stderr = channel.getErrStream();

            channel.connect(TIMEOUT);
            success = waitForChannelClosed(channel, stdout, stderr);
        } catch (IOException | JSchException e) {
            logger.error(e.getMessage().replace("\n", " "));
            success = false;
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
        return success;
    }

    protected boolean send(String cmd) {
        logger.debug("sending command: {}", cmd);
        if (cmd.contains("GLB1300-FIRMWARE-UPGRADE-PRESERVE")) {
            return copyCommand("glb1300.bin", true);
        } else if (cmd.contains("N600-FIRMWARE-UPGRADE-PRESERVE"))
        {
            return copyCommand("n600.bin", true);
        } else if (cmd.contains("N750-FIRMWARE-UPGRADE-PRESERVE")) {
            return copyCommand("n750.bin", true);
        } else if (cmd.contains("WRT3200ACM-FIRMWARE-UPGRADE-PRESERVE")) {
            return copyCommand("wrt3200acm.bin", true);
        } else if (cmd.contains("WRT32X-FIRMWARE-UPGRADE-PRESERVE")) {
            return copyCommand("wrt32x.bin", true);
        } else if (cmd.contains("EA6350-FIRMWARE-UPGRADE-PRESERVE")) {
            return copyCommand("ea6350.bin", true);
        } else if (cmd.contains("GLB1300-FIRMWARE-UPGRADE")) {
            return copyCommand("glb1300.bin", false);
        } else if (cmd.contains("N600-FIRMWARE-UPGRADE")) {
            return copyCommand("n600.bin", false);
        } else if (cmd.contains("N750-FIRMWARE-UPGRADE")) {
            return copyCommand("n750.bin", false);
        } else if (cmd.contains("WRT3200ACM-FIRMWARE-UPGRADE")) {
            return copyCommand("wrt3200acm.bin", false);
        } else if (cmd.contains("WRT32X-FIRMWARE-UPGRADE")) {
            return copyCommand("wrt32x.bin", false);
        } else if (cmd.contains("EA6350-FIRMWARE-UPGRADE")) {
            return copyCommand("ea6350.bin", false);
        }

        else {
            return normalCommand(cmd);
        }
    }

    protected boolean waitForChannelClosed(ChannelExec channel, InputStream stdout, InputStream stderr) {
        return true;
    }

    protected void disconnect() {
        session.disconnect();
    }

    protected UserInfo getUserInfo() {
        return null;
    }

    protected Properties getSessionConfig() {
        return new Properties();
    }

    protected String formatCmd(String cmd) {
        return cmd;
    }
}
