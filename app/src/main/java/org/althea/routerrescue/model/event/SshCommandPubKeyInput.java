/*
 * Mercury-SSH
 * Copyright (C) 2018 Skarafaz
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

package org.althea.routerrescue.model.event;

import org.althea.routerrescue.ssh.SshCommandDrop;

public class SshCommandPubKeyInput {
    SshCommandDrop<String> drop;

    public SshCommandPubKeyInput(SshCommandDrop<String> drop) {
        this.drop = drop;
    }

    public SshCommandDrop<String> getDrop() {
        return drop;
    }
}
