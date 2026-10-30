// spotless:off

/*
 * Copyright 2026 aoqia
 *
 * This file was originally released under LICENSE_UPSTREAM (see below for the original header) and has been relicensed
 * to GNU's AGPLv3 license, with additional terms in the LICENSE_EXCEPTION.md file.
 *
 * All modifications made to this file after the original version were created separate to the original copyright.
 * These modifications are provided under the AGPLv3 license (including exception terms). The portions of this file that
 * were not modified remain subject to the original copyright notice and license, and the original LICENSE is kept in
 * LICENSE_UPSTREAM located alongside LICENSE.
 *
 * The complete record of changes to this file is publicly available in the project's Git history, which anyone may view
 * at any time for any reason.
 *
 * Copyright (c) 2016-2025 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// spotless:on

package dev.aoqia.leaf.installer.server;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

import dev.aoqia.leaf.installer.InstallerGui;
import dev.aoqia.leaf.installer.util.Utils;

public class ServerPostInstallDialog extends JDialog {
    private final JPanel panel = new JPanel();

    private final ServerHandler serverHandler;
    private final Path installDir;

    private ServerPostInstallDialog(ServerHandler handler) throws HeadlessException {
        super(InstallerGui.instance, true);
        this.serverHandler = handler;
        this.installDir = Paths.get(handler.installLocation.getText());

        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        initComponents();
        setContentPane(panel);
        setIconImage(Toolkit.getDefaultToolkit().getImage(ClassLoader.getSystemClassLoader().getResource("icon.png")));
    }

    public static void show(ServerHandler serverHandler) {
        ServerPostInstallDialog dialog = new ServerPostInstallDialog(serverHandler);
        dialog.pack();
        dialog.setTitle(Utils.BUNDLE.getString("installer.title"));
        dialog.setLocationRelativeTo(InstallerGui.instance);
        dialog.setVisible(true);
    }

    private void initComponents() {
        addRow(panel, panel -> panel.add(fontSize(new JLabel(Utils.BUNDLE.getString("progress.done.server")), 20)));

        addRow(panel, panel -> {
            JButton closeButton = new JButton(Utils.BUNDLE.getString("progress.done"));
            closeButton.addActionListener(e -> {
                setVisible(false);
                dispose();
            });
            panel.add(closeButton);
        });
    }

    private JLabel fontSize(JLabel label, int size) {
        label.setFont(new Font(label.getFont().getName(), Font.PLAIN, size));
        return label;
    }

    private JLabel color(JLabel label, Color color) {
        label.setForeground(color);
        return label;
    }

    private void addRow(Container parent, Consumer<JPanel> consumer) {
        JPanel panel = new JPanel(new FlowLayout());
        consumer.accept(panel);
        parent.add(panel);
    }
}
