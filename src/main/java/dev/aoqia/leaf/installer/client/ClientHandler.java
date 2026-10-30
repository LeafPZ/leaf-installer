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

package dev.aoqia.leaf.installer.client;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import dev.aoqia.leaf.installer.Handler;
import dev.aoqia.leaf.installer.InstallerGui;
import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.util.ArgumentParser;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.NoopCaret;
import dev.aoqia.leaf.installer.util.Utils;

public class ClientHandler extends Handler {
    @Override
    public String cliHelp() {
        return "-dir <install dir> -- (default: current dir) "
            + "-game <version> -- (default: latest) "
            + "-loader <version> -- (default: latest) "
            + "-manual -- (default: null)";
    }

    @Override
    public void install(boolean proxy) {
        installInternal(proxy);
    }

    @Override
    public void installCli(ArgumentParser args) throws Exception {
        Path path = Paths.get(args.getOrDefault("dir", () -> Utils.getClientGamePath().toString()));
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Game directory not found at " + path);
        }

        boolean useProxy = !args.has("manual");
        boolean createConfig = args.has("createConfig");

        String gameVersion = getGameVersion(args);

        final LoaderVersion loaderVersion;
        if (!useProxy) {
            loaderVersion = new LoaderVersion(getLoaderVersion(args));
        } else {
            try {
                loaderVersion = new LoaderVersion(Utils.getLatestLoaderProxy().version);
            } catch (IOException exc) {
                error(exc);
                return;
            }
        }

        new ClientInstaller(path, gameVersion, loaderVersion, InstallerProgress.CONSOLE)
            .install(useProxy, createConfig);
    }

    @Override
    public String name() {
        return Utils.BUNDLE.getString("tab.client");
    }

    @Override
    public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
        installLocation.setText(Utils.getClientGamePath().toString());
    }

    private void installInternal(boolean proxy) {
        String gameVersion = (String) gameVersionComboBox.getSelectedItem();
        boolean createConfig = createConfigCheckbox.isEnabled() && createConfigCheckbox.isSelected();

        final LoaderVersion loaderVersion;
        if (!proxy) {
            loaderVersion = queryLoaderVersion();
        } else {
            try {
                loaderVersion = new LoaderVersion(Utils.getLatestLoaderProxy().version);
            } catch (IOException exc) {
                error(exc);
                return;
            }
        }

        System.out.println("Installing");

        new Thread(() -> {
            try {
                Path pzPath = Utils.normaliseClientGamePath(Paths.get(installLocation.getText())).toAbsolutePath();
                if (!Files.exists(pzPath)) {
                    throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.directory"));
                }

                new ClientInstaller(pzPath, gameVersion, loaderVersion, this).install(proxy, createConfig);
                SwingUtilities.invokeLater(() -> showInstalledMessage(proxy, loaderVersion, gameVersion));
            } catch (Exception e) {
                error(e);
            } finally {
                buttonInstall.setEnabled(true);
            }
        }).start();
    }

    private void showInstalledMessage(boolean proxy, LoaderVersion loaderVersion, String gameVersion) {
        JEditorPane pane = new JEditorPane("text/html",
            String.format("<html><body style=\"%s\">%s</body></html>", buildEditorPaneStyle(),
                new MessageFormat(Utils.BUNDLE.getString("prompt.install.successful")).format(
                    new Object[] { (proxy ? "Proxy " : "") + loaderVersion.name, gameVersion })));
        pane.setBackground(new Color(0, 0, 0, 0));
        pane.setEditable(false);
        pane.setCaret(new NoopCaret());

        pane.addHyperlinkListener(e -> {
            try {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } else {
                        throw new UnsupportedOperationException("Failed to open " + e.getURL().toString());
                    }
                }
            } catch (Throwable throwable) {
                error(throwable);
            }
        });

        final Image iconImage = Toolkit
            .getDefaultToolkit()
            .getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
        JOptionPane.showMessageDialog(null, pane, Utils.BUNDLE.getString("prompt.install.successful.title"),
            JOptionPane.INFORMATION_MESSAGE, new ImageIcon(iconImage.getScaledInstance(64, 64, Image.SCALE_DEFAULT)));
    }
}
