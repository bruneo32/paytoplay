# PayToPlay
A simple Minecraft plugin for keeping out players that didn't pay their share of the server costs.

> *Disclaimer*: This plugin does not handle monetary transactions. The users pay you via third party services, and you register the transaction in the [accounting.csv](#accountingcsv) file.

# In-game commands
- `/owe` Check your debt
- `/owe <user>` Check another player's debt

# Server config
The first time that the server runs on it have the PayToPlay plugin, it crates two new files.
- `accounting.csv`
- `config.yml`

## config.yml
```yml
PayToPlay:
  HoursPerCharge: 15 # Number of hours to count as one charge
  AmountPerCharge: 1 # Number of dollars/euros to make per charge
  DebtToKick: 3 # Number of accumulated debt to kick a player, until they pay
  CurrencyChar: € # Character to use for currency display
  WorkerTicks: 1200 # Number of ticks to wait between debt checks (20 ticks = 1 second, 1200 ticks = 1 minute)
```

## accounting.csv
This file is used to store the player's hours and the amount they paid.
- The **first** column is the player's name (*case insensitive*)
- The **second** column is the total of money that they have paid to you.
```csv
bruneo,32.0
johndoe,15.23
javasucks,0
```

## Why is the db CSV instead of a SQLite or something else?
The reason is simple, for admin flexibility.
- If someone pays you, and you are on vacation or something:
  - You can edit the CSV file from your phone (if you have server access).
- If someone pays you, and the server is offline:
  - You can edit the CSV with the server offline
  - With SQLite, the server would need to be active in order to update the database.

# Build
## Prerequisites

In order to try this out you need the following software to be installed on your machine:

- Java version 17 or above (e.g. [OpenJDK](https://openjdk.java.net/install/))
- [git](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git)
- [docker](https://docs.docker.com/v17.09/engine/installation/)

```sh
sudo apt update
sudo apt install git openjdk-17-jdk docker.io
```

## Quickstart

Clone the template project to your system:
````bash
git clone https://github.com/bruneo32/paytoplay.git
````

This project uses [Maven](https://maven.apache.org/) for building. So on your command line run

````bash
mvn package
````

To test the plugin we fire up the spigot Minecraft server using an existing docker image.
In order for it to find the jar containing our plugin we need to mount the `target` folder to `/data/plugins`

```bash
docker run --rm -e EULA=true  -p 25565:25565 -v $(pwd)/target:/data/plugins cmunroe/spigot:1.20.1
```

To test it with bukkit do:

```bash
docker run --rm -e EULA=true  -p 25565:25565 -v $(pwd)/target:/data/plugins cmunroe/bukkit:1.20.1
```

In the log produced by the server on the command line watch out for the following lines indicating that the plugin
was deployed properly:

```
[19:45:41] [Server thread/INFO]: [PayToPlay] Enabling PayToPlay v1.0
```

Start the Mincraft client on your computer and connect to the local Minecraft server by specifying `localhost` as Server Address.

To install your plugin in another Minecraft server just copy the file `target/paytoplay-plugin-1.0-1.jar` to
that server's `plugin` folder.
