/*
 * Copyright (c) 2016-2025 FabricMC, aoqia
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
package dev.aoqia.leaf.installer;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Locale;

import dev.aoqia.leaf.installer.util.ArgumentParser;
import dev.aoqia.leaf.installer.util.GameMetaHandler;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.LoaderMetaHandler;
import dev.aoqia.leaf.installer.util.OperatingSystem;
import dev.aoqia.leaf.installer.util.Utils;

import static dev.aoqia.leaf.installer.Main.GAME_VERSION_META;
import static dev.aoqia.leaf.installer.Main.LOADER_META;

public abstract class Handler implements InstallerProgress {
    protected static final int HORIZONTAL_SPACING = 4;
    protected static final int VERTICAL_SPACING = 6;

    private static final String SELECT_CUSTOM_ITEM = "(select custom)";
    public JButton buttonInstall;
    public JButton copyArgButton;
    public JComboBox<String> gameVersionComboBox;
    public JTextField installLocation;
    public JButton selectFolderButton;
    public JLabel statusLabel;
    public JCheckBox unstableCheckbox;
    public JCheckBox loaderProxyCheckbox;
    public JComboBox<String> loaderVersionComboBox;
    public JCheckBox createConfigCheckbox;
    private JPanel pane;
    private JTextArea launchArguments;
    private String proxyJarName;
    private LoaderVersion customLoaderVersion;

    protected static Component createSpacer() {
        return Box.createRigidArea(new Dimension(4, 0));
    }

    public abstract String name();

    public abstract void install(boolean proxy);

    public abstract void installCli(ArgumentParser args) throws Exception;

    public abstract String cliHelp();

    // this isnt great, but works
    public void setupPane1(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
    }

    public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
    }

    protected Component makeLaunchOptionsPanel(JPanel manualPanel) {
        return manualPanel;
    }

    public JPanel makePanel(InstallerGui installerGui) {
        pane = new JPanel(new GridBagLayout());
        pane.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(VERTICAL_SPACING, HORIZONTAL_SPACING, VERTICAL_SPACING, HORIZONTAL_SPACING);
        c.gridx = c.gridy = 0;

        setupPane1(pane, c, installerGui);

        gameVersionComboBox = new JComboBox<>();
        gameVersionComboBox.setEnabled(false);

        unstableCheckbox = new JCheckBox(Utils.BUNDLE.getString("option.show.unstable"));
        unstableCheckbox.setEnabled(false);
        unstableCheckbox.setSelected(false);
        unstableCheckbox.addActionListener(e -> {
            if (GAME_VERSION_META.isComplete()) {
                updateGameVersions(GAME_VERSION_META.getVersions());
            }
        });

        addRow(pane, c, "prompt.game.version", gameVersionComboBox, createSpacer(), unstableCheckbox);
        GAME_VERSION_META.onComplete(this::updateGameVersions);

        loaderVersionComboBox = new JComboBox<>();
        loaderVersionComboBox.setEnabled(false);

        loaderProxyCheckbox = new JCheckBox(Utils.BUNDLE.getString("option.use.proxy"));
        loaderProxyCheckbox.setToolTipText(Utils.BUNDLE.getString("tooltip.use.proxy"));
        loaderProxyCheckbox.setSelected(true);
        loaderProxyCheckbox.addActionListener(e -> {
            final boolean selected = loaderProxyCheckbox.isSelected();

            gameVersionComboBox.setEnabled(!selected);
            unstableCheckbox.setEnabled(!selected);
            loaderVersionComboBox.setEnabled(!selected);
            createConfigCheckbox.setEnabled(!selected);

            if (!selected && Main.LOADER_META.isComplete()) {
                updateLoaderVersions(Main.LOADER_META.getVersions());
            }
            updateLaunchArguments();
        });

        addRow(pane, c, "prompt.loader.version", loaderVersionComboBox, createSpacer(), loaderProxyCheckbox);
        Main.LOADER_META.onComplete(this::updateLoaderVersions);

        addRow(pane, c, "prompt.select.location", installLocation = new JTextField(20),
            selectFolderButton = new JButton());
        selectFolderButton.setText("...");
        selectFolderButton.setPreferredSize(
            new Dimension(installLocation.getPreferredSize().height, installLocation.getPreferredSize().height));
        selectFolderButton.addActionListener(
            e -> InstallerGui.selectInstallLocation(() -> installLocation.getText(), s -> installLocation.setText(s)));

        setupPane2(pane, c, installerGui);

        createConfigCheckbox = new JCheckBox(Utils.BUNDLE.getString("option.create.config"), true);
        createConfigCheckbox.setEnabled(false);
        addRow(pane, c, null, createConfigCheckbox);

        statusLabel = new JLabel(Utils.BUNDLE.getString("prompt.loading.versions"));

        launchArguments = new JTextArea();
        launchArguments.setEditable(false);

        copyArgButton = new JButton(Utils.BUNDLE.getString("prompt.copy.arg"));
        copyArgButton.setEnabled(false);
        copyArgButton.addActionListener(e -> {
            StringSelection s = new StringSelection(launchArguments.getText().replace('\n', ' '));
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(s, null);
        });

        buttonInstall = new JButton(Utils.BUNDLE.getString("prompt.install"));
        buttonInstall.addActionListener(e -> {
            buttonInstall.setEnabled(false);
            install(loaderProxyCheckbox.isEnabled() && loaderProxyCheckbox.isSelected());
        });

        JPanel manualPanel = new JPanel(new GridBagLayout());
        GridBagConstraints manualConstraints = new GridBagConstraints();
        manualConstraints.gridx = manualConstraints.gridy = 0;
        manualConstraints.insets = c.insets;
        addRow(manualPanel, manualConstraints, null,
            new JLabel(Utils.BUNDLE.getString("prompt.launch.arguments")));
        JPanel argumentsPanel = new JPanel();
        argumentsPanel.setOpaque(false);
        argumentsPanel.setBorder(UIManager.getBorder("TextField.border"));
        argumentsPanel.add(launchArguments);
        argumentsPanel.add(copyArgButton);
        addRow(manualPanel, manualConstraints, null, argumentsPanel);
        addRow(pane, c, null, makeLaunchOptionsPanel(manualPanel));
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        pane.add(new JSeparator(), c);
        c.gridy++;
        addRow(pane, c, null, statusLabel);
        c.insets = new Insets(VERTICAL_SPACING, HORIZONTAL_SPACING, 10, HORIZONTAL_SPACING);
        addLastRow(pane, c, null, buttonInstall);

        gameVersionComboBox.addActionListener(e -> updateLaunchArguments());
        loaderVersionComboBox.addActionListener(e -> {
            if (SELECT_CUSTOM_ITEM.equals(loaderVersionComboBox.getSelectedItem())) {
                customLoaderVersion = null;
                customLoaderVersion = queryLoaderVersion();
            }
            updateLaunchArguments();
        });
        updateLaunchArguments();
        new Thread(() -> {
            try {
                String jarName;
                synchronized (Utils.class) {
                    jarName = Utils.getLatestLoaderProxy().getJarName();
                }
                SwingUtilities.invokeLater(() -> {
                    proxyJarName = jarName;
                    updateLaunchArguments();
                });
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> error(e));
            }
        }).start();

        installerGui.updateSize(true);

        return pane;
    }

    private void updateLaunchArguments() {
        if (launchArguments == null) {
            return;
        }

        String arguments = "";
        if (loaderProxyCheckbox.isSelected()) {
            if (proxyJarName != null) {
                if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS && Main.LATEST_PROXY_NATIVE_TAG != null) {
                    String tag = Main.LATEST_PROXY_NATIVE_TAG;
                    arguments = String.format("-agentpath:.leaf/lib/native-%s.dll\n",
                        tag.startsWith("v") ? tag.substring(1) : tag);
                }
                arguments += String.format("-javaagent:.leaf/lib/%s", proxyJarName);
                arguments += " --";
            }
        } else if (loaderVersionComboBox.getSelectedItem() != null && gameVersionComboBox.getSelectedItem() != null) {
            String version = (String) loaderVersionComboBox.getSelectedItem();
            if (SELECT_CUSTOM_ITEM.equals(version)) {
                version = customLoaderVersion == null ? null : customLoaderVersion.name;
            }
            if (version != null) {
                arguments = String.format("-pzexeconfig leaf-%s-%s.json",
                    version, gameVersionComboBox.getSelectedItem());
            }
        }
        launchArguments.setText(arguments);
        copyArgButton.setEnabled(true);
        if (InstallerGui.instance != null) {
            InstallerGui.instance.updateSize(false);
        }
    }

    private void updateGameVersions(List<GameMetaHandler.Version> versions) {
        gameVersionComboBox.removeAllItems();

        for (GameMetaHandler.Version version : versions) {
            if (!unstableCheckbox.isSelected() && version.isUnstable()) {
                continue;
            }

            gameVersionComboBox.addItem(version.id());
        }
        gameVersionComboBox.setSelectedIndex(0);

        InstallerGui.instance.updateSize(false);
    }

    private void updateLoaderVersions(List<LoaderMetaHandler.Version> versions) {
        loaderVersionComboBox.removeAllItems();

        for (LoaderMetaHandler.Version version : versions) {
            loaderVersionComboBox.addItem(version.id());
        }

        loaderVersionComboBox.addItem(SELECT_CUSTOM_ITEM);

        loaderVersionComboBox.setSelectedIndex(0);
        statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
        InstallerGui.instance.updateSize(false);
    }

    protected LoaderVersion queryLoaderVersion() {
        String ret = (String) loaderVersionComboBox.getSelectedItem();
        assert ret != null;

        if (!ret.equals(SELECT_CUSTOM_ITEM)) {
            return new LoaderVersion(ret);
        } else {
            if (customLoaderVersion != null) {
                return customLoaderVersion;
            }
            // ask user for loader jar

            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Select Leaf Loader JAR");
            chooser.setFileFilter(new FileNameExtensionFilter("Java Archive", "jar"));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            File file = chooser.getSelectedFile();

            // determine loader version from fabric.mod.json

            try {
                return new LoaderVersion(file.toPath());
            } catch (IOException e) {
                error(e);
                return null;
            }
        }
    }

    @Override
    public void updateProgress(String text) {
        statusLabel.setText(text);
        statusLabel.setForeground(UIManager.getColor("Label.foreground"));
    }

    @Override
    public void error(Throwable throwable) {
        StringWriter sw = new StringWriter(800);

        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }

        String st = sw.toString().trim();
        System.err.println(st);

        String html = String.format("<html><body style=\"%s\">%s</body></html>", buildEditorPaneStyle(),
            st.replace(System.lineSeparator(), "<br>").replace("\t", "&ensp;"));
        JEditorPane textPane = new JEditorPane("text/html", html);
        textPane.setEditable(false);

        statusLabel.setText(throwable.getLocalizedMessage());
        statusLabel.setForeground(Color.RED);

        JOptionPane.showMessageDialog(pane, textPane, Utils.BUNDLE.getString("prompt.exception.occurrence"),
            JOptionPane.ERROR_MESSAGE);
    }

    protected String buildEditorPaneStyle() {
        JLabel label = new JLabel();
        Font font = label.getFont();
        Color color = label.getBackground();
        return String.format(Locale.ENGLISH,
            "font-family:%s;font-weight:%s;font-size:%dpt;background-color: rgb(%d,%d," + "%d);", font.getFamily(),
            (font.isBold() ? "bold" : "normal"), font.getSize(), color.getRed(), color.getGreen(), color.getBlue());
    }

    protected void addRow(Container parent, GridBagConstraints c, String label, Component... components) {
        addRow(parent, c, false, label, components);
    }

    protected void addLastRow(Container parent, GridBagConstraints c, String label, Component... components) {
        addRow(parent, c, true, label, components);
    }

    private void addRow(Container parent, GridBagConstraints c, boolean last, String label, Component... components) {
        if (label != null) {
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.LINE_END;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            parent.add(new JLabel(Utils.BUNDLE.getString(label)), c);
            c.gridx++;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.HORIZONTAL;
        } else {
            c.gridwidth = 2;
            if (last) {
                c.weighty = 1;
            }
            c.anchor = last ? GridBagConstraints.PAGE_START : GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.NONE;
        }

        c.weightx = 1;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        for (Component comp : components) {
            panel.add(comp);
        }

        parent.add(panel, c);

        c.gridy++;
        c.gridx = 0;
    }

    protected String getGameVersion(ArgumentParser args) {
        return GAME_VERSION_META.parseVersion(args.get("gameVersion"), args.has("unstable")).id();
    }

    protected String getLoaderVersion(ArgumentParser args) {
        return LOADER_META.parseVersion(args.get("loader"), false).id();
    }
}
