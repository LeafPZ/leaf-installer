<div align="center">

<h1>
    The universal installer for
    <a href="https://pzwiki.net/wiki/Leaf">
        <img src="https://github.com/LeafPZ.png" width="24px" alt="LeafPZ Icon">
        leaf
    </a>
</h1>

![License](https://img.shields.io/github/license/LeafPZ/leaf-installer?label=License)
![Gradle version](https://img.shields.io/badge/Gradle-9.7.1-teal?logo=gradle)
![Build status](https://github.com/LeafPZ/leaf-installer/actions/workflows/build.yml/badge.svg?branch=main&label=build)
![Downloads](https://img.shields.io/github/downloads/LeafPZ/leaf-installer/total?label=Downloads)
![Code Size](https://img.shields.io/github/languages/code-size/LeafPZ/leaf-installer?label=Code%20Size)
![Maven status](https://img.shields.io/website?url=https%3A%2F%2Fmaven.aoqia.dev%2F&label=Maven)

</div>

This project is an installer that helps guide a user with installing leaf. It allows the user to choose between two
methods of installation, those being via proxy-loader and manual installation.

### Requirements

- Java 17 or higher
- **(optional)** [JarFix][JarFix] \
  <small>Useful if you are on Windows and double-clicking the installer JAR doesn't open it.</small>

### Usage

The primary use-case for this project is to install [leaf-loader][LeafLoader] and its dependencies. The only thing you
need to do is download the JAR linked in the [latest release][LeafInstallerLatestRelease].

> [!CAUTION]
> You should be informed about the potential risks of Java modding before installing *any* unofficial modding toolchain.
> There are safeguards that are provided such as leaf mod verification and hashing, but leaf **WILL NOT** stop you from
> loading a malicious mod from the Workshop or any other source if you allow it to do so.
>
> It is always recommended to **deny** loading any mod that you do not trust. If you need help verifying the trust of a
> given mod, ask around for advice from people with experience. A good place to ask is in the official or unofficial
> Project Zomboid Discord servers.

<details open>
<summary>Recommended</summary>

1. Download the installer JAR from the [latest release][LeafInstallerLatestRelease] 
2. Run it via double-click \
   **Double-click may not work on Linux or Mac** - if so, run it via the terminal: `java -jar installer-1.3.0.jar`
3. Choose the environment - client or server
4. Check the game path is set correctly
5. Press install
6. Click the `Copy arg` button (it should give you something like `-javaagent:.leaf/lib/loader-proxy-0.2.1.jar`)
7. Paste the arguments into your Steam launch options for Project Zomboid (read below for help)
8. Add `--` after the arguments (see [Startup parameters - JVM arguments](https://pzwiki.net/wiki/Startup_parameters#JVM_arguments))
9. Subscribe to the **[Leaf Loader][LeafLoaderWorkshop]** mod on the Steam Workshop

You do not need to enable the leaf loader mod in-game for it to be loaded by the proxy, but it is recommended to do so.
You may now subscribe to any Leaf mod on the Workshop and the loader will discover enabled game mods at the next game
start. It will prompt you to allow or deny discovered mods -- remember to only allow mods you trust!

If you need some help with usage of game arguments, you should read the
[Startup parameters - From the Steam application][StartupParamsSub] which shows the correct way to use them.

</details>

<details>
<summary>Manual</summary>

Using the proxy is the **recommended** method. You may choose to manually install the loader if you want, however it
will not auto-update the loader or any of its dependencies. It is on you to update where necessary, but it is safer.

Manual installation requires unchecking the `Use proxy` box and by selecting the appropriate game and loader versions.
Follow the **Recommended** usage instructions steps 1-4. Configure the game version, loader version, and any other
options accordingly -- only then you can click "install". After installing, keep following the recommended instructions.

</details>

<details>
<summary>Alternative</summary>

If you are using the proxy and do not want to use the installer, you may download the proxy JAR from the
[latest release][LeafLoaderProxyLatestRelease] and place it in the `.leaf/lib` folder; where `.leaf` is a folder that is
(usually) alongside `projectzomboid.jar` (depends on JVM working directory).

</details>

### Configuration

#### Installer

| Option             | Description                                                                         |
|--------------------|-------------------------------------------------------------------------------------|
| `Game Version`     | Game version to target. Is disabled when `Use proxy` is active                      |
| `Loader Version`   | Loader version to install. Is disabled when `Use proxy` is active                   |
| `Game Location`    | Path to Project Zomboid Steam game installation                                     |
| `Show pre-release` | Shows pre-release game versions                                                     |
| `Use proxy`        | Installs the [loader proxy][LeafLoaderProxy] instead of the loader itself           |
| `Create config`    | Creates a game launcher config. Is disabled when `Use proxy` is active              |
| `Create script`    | **NOT RECOMMENDED**. Creates a modified launch script. Only for server environments |

#### Environment

If you use leaf regularly either for development (as a developer) or production (as a user), `LEAF_CLIENT_GAME_PATH`
and `LEAF_SERVER_GAME_PATH` environment variables may be set to allow leaf to automatically detect the location of the
game.

### What Actually Happens

At the time of installation (the 'install' button) the installer will, depending on installation method, download either
[leaf-loader-proxy][LeafLoaderProxy] or [leaf-loader][LeafLoader] and its dependencies.

The installer contacts both the [leaf][Leaf] GitHub repository and the configured Maven repository. The leaf manifest
repostiory hosts manifest content for both the game (file hashes, version information, etc) and for the loader's
`leaf-installer.json` meta (loader dependencies to be downloaded, etc). The configured Maven is used to download
artefacts from (loader-proxy, loader, etc).

### Development

You can build the project like so:

```shell
./gradlew build
```

For developing your own Leaf mods, you should read the [Leaf][PZWikiPage] wiki page which will contain a top-down
summary of the entire leaf project.

### Support

If you need any help whatsoever with Leaf, you can discuss anything leaf-related on Discord through the
official [Project Zomboid Modding Community](https://discord.gg/2Vr6Wyh6Am) Discord using the appropriate channels.

### Special Thanks

- The entire [FabricMC team](https://github.com/FabricMC/)!
- [albion](https://github.com/demiurgeQuantified)
- GigaWatte
- electrisoma
- [SimKDT](https://github.com/SimKDT)

[JarFix]: https://johann.loefflmann.net/en/software/jarfix/index.html
[LeafInstallerLatestRelease]: https://github.com/LeafPZ/leaf-installer/releases/latest
[Leaf]: https://github.com/LeafPZ/leaf
[LeafLoader]: https://github.com/LeafPZ/leaf-loader
[LeafLoaderProxy]: https://github.com/LeafPZ/leaf-loader-proxy
[LeafLoaderProxyLatestRelease]: https://github.com/LeafPZ/leaf-loader-proxy/releases/latest
[LeafLoaderWorkshop]: https://steamcommunity.com/sharedfiles/filedetails/?id=3776625738
[PZWikiPage]: https://pzwiki.net/wiki/Leaf
[StartupParams]: https://pzwiki.net/wiki/Startup_parameters
[StartupParamsSub]: https://pzwiki.net/wiki/Startup_parameters#From_the_Steam_application
