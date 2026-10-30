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

package dev.aoqia.leaf.installer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;

import dev.aoqia.leaf.installer.util.Utils;

public class InstallerGui extends JFrame {
    public static InstallerGui instance;

    private JTabbedPane contentPane;

    public InstallerGui() {
        initComponents();
        setContentPane(contentPane);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Image iconImage = Toolkit
            .getDefaultToolkit()
            .getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
        setIconImage(iconImage);
        setTaskBarImage(iconImage);

        instance = this;

        Main.loadMetadata();
    }

    public static void selectInstallLocation(Supplier<String> initalDir, Consumer<String> selectedDir) {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(initalDir.get()));
        chooser.setDialogTitle(Utils.BUNDLE.getString("prompt.select.location"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            selectedDir.accept(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    public static void start() {
        FlatMacDarkLaf.setup();
        InstallerGui dialog = new InstallerGui();
        dialog.updateSize(true);
        dialog.setTitle(Utils.BUNDLE.getString("installer.title"));
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private static void setTaskBarImage(Image image) {
        try {
            // Only supported in Java 9 +
            Class<?> taskbarClass = Class.forName("java.awt.Taskbar");
            Method getTaskbar = taskbarClass.getDeclaredMethod("getTaskbar");
            Method setIconImage = taskbarClass.getDeclaredMethod("setIconImage", Image.class);
            Object taskbar = getTaskbar.invoke(null);
            setIconImage.invoke(taskbar, image);
        } catch (Exception e) {
            // Ignored, running on Java 8
        }
    }

    public void updateSize(boolean updateMinimum) {
        if (updateMinimum) {
            setMinimumSize(null);
        }
        setPreferredSize(null);
        pack();
        Dimension size = getPreferredSize();
        if (updateMinimum) {
            setMinimumSize(size);
        }
        setPreferredSize(new Dimension(Math.max(450, size.width), size.height));
        setSize(getPreferredSize());
    }

    private void initComponents() {
        contentPane = new JTabbedPane(JTabbedPane.TOP);
        Main.HANDLERS.forEach(
            handler -> contentPane.addTab(Utils.BUNDLE.getString("tab." + handler.name().toLowerCase(Locale.ROOT)),
                handler.makePanel(this)));
    }
}
