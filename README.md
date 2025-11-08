#  CONTRIBUTING

DO NOT CREATE PULL REQUESTS FOR THIS PROJECT.

ANY PULL REQUESTS YOU CREATE WILL NOT BE MERGED IN.

This project does not currently accept pull requests from the public.

Having said this, please do file issues if you notice something broken or undesirable.

#  Terminal Block Mining Simulation Game

![Terminal Block Mining Simulation Game](images/block-mining-simulation-game-thumbnail.png "Terminal Block Mining Simulation Game")

This project contains the 'Terminal Block Mining Simulation Game', a video game where you simulate mining blocks of iron ore in the terminal.  The game uses procedural terrain generation and the game world itself is infinite.  The terrain will start generating automatically in the background near the player.  All generated terrain and player data is stored in a SQLite (or Postgres) database file.  The location of this world file defaults to the directory where you launch the game, but you can configure it with the '--block-world-file' flag.  You can also use the '--log-file' to set the location of a log file.  If the '--log-file' flag is omitted, logging will be disabled.

#  Devlog Videos

###  Overview Video For v0.0.7 Release (2025-07-25)

[A Frame Based UI For Terminal Games - TBMSG v0.0.7](https://www.youtube.com/watch?v=0pePUFmaAtw)

[![A Frame Based UI For Terminal Games - TBMSG v0.0.7](images/0pePUFmaAtw.png)](http://www.youtube.com/watch?v=0pePUFmaAtw)

###  Shorts Overview Video (2025-07-09)

[Terminal Block Mining Simulation Game](https://youtu.be/RQiNQfpacco)

[![Terminal Block Mining Simulation Game](images/RQiNQfpacco.png)](http://www.youtube.com/watch?v=RQiNQfpacco)

###  Overview Video For v0.0.6 Release (2024-11-07)

[My Terminal Based Video Game For Linux](https://www.youtube.com/watch?v=nRGTXZQg5Gg)

[![My Terminal Based Video Game For Linux](images/nRGTXZQg5Gg.png)](http://www.youtube.com/watch?v=nRGTXZQg5Gg)

#  Player Movement

You can use the 'w', 'a', 's' 'd' keys to move around on the screen.  You can use the space bar to go up and the 'x' key to move down (assuming there isn't a solid block in the way on a different level).

#  Exiting/Quitting The Game

Press the 'q' key to quit the game.

#  Mining Blocks

Press the 'm' key to mine blocks.

#  Crafting

You can press the 'c' key to try and craft new blocks, such as metallic iron, and an iron pickaxe.  Crafting will occur automatically if you have enough reagents. Currently, the game only supports four different crafting recipes:

-  Using wood to make a Wooden Pick Axe
-  Using stone and wood to make a Stone Pick Axe
-  Using iron oxide and wood to make Metallic Iron
-  Using metallic iron and wood to make an Iron Pick Axe

#  Place Blocks

Press the 'p' key to place whatever block is currently selected in your inventory.

#  In Game Help Menu

You can access an in-game help menu by pressing the 'ESC' key.

#  Switch Between Frames

You can switch focus between the 'frames' in the game's UI by pressing the 'TAB' key.

#  Command Line Arguments

You can run the .jar file with the '--help' flag to show a help menu:

```
java -jar block-mining-simulation-game-single-player-client-0.0.8.jar --help
```

```
Block Mining Simulation Game - Available Command-line Arguments:

--help                                     - Display this help menu.
--debug-arguments                          - Echo back info about the value of command line argument values were parsed, and what the default values are.
--log-file                        <arg>    - The name of the log file to use.  If not provided, there will be no logging.
--disable-jni                              - Disable the use of JNI (may cause some events like to window size changes to be ignored).
--use-ascii                                - Explicitly try to use only the simplest ASCII characters to produce graphics (for non-graphics ttys)
--use-emojis                               - Explicitly try to use more advanced Unicode character graphics like emojis
--right-to-left-print                      - Print screen updates from right to left instead of left to right.  Avoids display bugs in some terminals.
--compatibility-width             <arg>    - Specify a fixed width for all non-ASCII characters.
--fixed-width                     <arg>    - Specify a fixed width for the number of terminal columns.
--fixed-height                    <arg>    - Specify a fixed height for the number of terminal columns.
--allow-unrecognized-block-types           - Allow the game to run even when there are block types that aren't supported in the block schema.
--block-world-file                <arg>    - The name of the sqlite database file (SQLITE only).
--print-block-schema                       - Print current block schema and exit.
--block-schema-file               <arg>    - If specified, ignore the default built-in block schema and uses the one provided at file/path.
--print-user-interaction-config            - Print the current configuration that describes which keys control the game.
--user-interaction-config-file    <arg>    - If specified, ignore the default built-in user interaction config and uses the one provided at file/path.
--database-subprotocol            <arg>    - The protocol for the database connection string.  Currently supports 'postgresql' and 'sqlite'.
--database-hostname               <arg>    - The 'hostname' for the database connection. Can be IP address or DNS name.
--database-port                   <arg>    - The port for the database connection.
--database-name                   <arg>    - The 'name' of the database to connect to for the database connection string.
--database-username               <arg>    - The username for the database connection.
--database-password               <arg>    - The password for the database connection.
```

#  Compatibility Width

All of the graphics in this game are simple text and emoji characters.  As such, in order for the game to correctly
display characters and screen updates, it needs to be able to accurately calculate the width of a character in
the given terminal environment.  Certain terminals don't properly support multi-column Unicode characters which 
can lead to display issues.  If you experience these issues, it is suggested that you try running the game with
the '--compatibility-width' flag set to 3.  This will force all non-ASCII characters to have an assumed
width of 3 columns:

```
java -jar block-mining-simulation-game-single-player-client-0.0.8.jar --compatibility-width 3
```

#  Supported Platforms

Currently, the game has only been tested to work on a default installation of Ubuntu Linux.

#  Launching The Game

Compiling the game from scratch is not necessary.  You can download pre-compiled .jar files from GitHub in the 'Releases' section for this repo:

```
wget https://github.com/RobertElderSoftware/robert-elder-software-java-modules/releases/download/0.0.8/block-mining-simulation-game-single-player-client-0.0.8.jar
java -jar block-mining-simulation-game-single-player-client-0.0.8.jar
```

The game should immediately launch and fill up the terminal with graphics.  You can exit the game by pressing the 'q' key.  By default, the game saves it's world data into a SQLite database file that lives in the current directory.

#  Verify The Jar Signature (Optional)

If you are concerned about the authenticity of the .jar file, you can also verify the signature using GPG:

```
wget https://github.com/RobertElderSoftware/robert-elder-software-java-modules/releases/download/0.0.8/block-mining-simulation-game-single-player-client-0.0.8.jar.asc
gpg --search-keys robert@robertelder.org
#  Should match the key for 'robert@robertelder.org'
gpg --recv-keys ECBD481DBCA5C48804FBD08720B9852CF0558BAA
gpg --verify block-mining-simulation-game-single-player-client-0.0.8.jar.asc block-mining-simulation-game-single-player-client-0.0.8.jar
```

The output should look something like this:

```
gpg: Signature made Thu 01 Aug 2024 01:49:03 PM EDT
gpg:                using ECDSA key ECBD481DBCA5C48804FBD08720B9852CF0558BAA
gpg: Good signature from "Robert Elder (Created on 2024-07-31) <robert@robertelder.org>" [unknown]
gpg: WARNING: This key is not certified with a trusted signature!
gpg:          There is no indication that the signature belongs to the owner.
Primary key fingerprint: ECBD 481D BCA5 C488 04FB  D087 20B9 852C F055 8BAA
```

#  Building The Game

To build the game, you will need to set up a development environment that can support Java 21 and a version of maven that can support Java 21.

To build the JNI library, you will also need a c++ compiler and make

```
sudo apt-get install g++ make
```

Next, you can compile the game from source by running this command:

```
./res-modules/block-mining-simulation-game-single-player-client/run_single_player_client.sh
```

Once it finishes building, it should launch right into the game.

#  Run Unit Tests

You can run the unit tests with this command:

```
mvn test
```

You can run a specific unit test (ex.  'threeDimensionalCircularBufferTest') with a command like this:

```
mvn -pl res-modules/block-mining-simulation-game-unit-tests -Dtest=BlockManagerUnitTest#threeDimensionalCircularBufferTest test
```

#  Building In IntelliJ IDEA

Select Menu option:  Run -> Edit Configurations -> Add New Configuration -> Maven

Set name of configuration to be 'Core' and 'Run' command line as

```
-e clean install
```

Now create a second Maven run configuration:

Set name of configuration to be 'Single Player Client' and 'Run' command line as

```
-e -pl res-modules/block-mining-simulation-game-single-player-client -amd clean compile package spring-boot:repackage spring-boot:run
```

Click 'Modify Options' and click 'add before launch task' for a 'another run configuration' that runs the 'Core' run configuration automatically from this 'Single Player Client' configuration.

Click 'Modify Options' and select 'Emulate Terminal'.

Unselect 'Inherit from settings' under 'Java Options'.

Add a line with 'VM Options:'

```
-Dspring-boot.run.arguments="--log-file=/tmp/single-player-block-client-intellij.log"
```

or modify the above command line arguments to whatever you like.

Then, click 'run' icon to build and run single player client.

#  Remote JVM Debugging Command

Here is an example of how to run the game so that a debugger (such as IntelliJ) can be connected:

```
java -agentlib:jdwp=transport=dt_socket,address=5005,server=y,suspend=y -jar res-modules/block-mining-simulation-game-single-player-client/target/block-mining-simulation-game-single-player-client-0.0.8.jar --log-file /tmp/single-player-block-client.log --block-world-file /tmp/single-player-world.sqlite
```

#  Profiler Running Command

Here is an example of how to run the game so that a profiler like 'visualvm' can be connected.  This is useful for profiling the efficiency of the game and determining which functions consume the most CPU cycles or memory:

```
java -Dspring-boot.run.jvmArguments="-Xdebug -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.rmi.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=localhost" -jar res-modules/block-mining-simulation-game-single-player-client/target/block-mining-simulation-game-single-player-client-0.0.8.jar --log-file /tmp/single-player-block-client.log --block-world-file /tmp/single-player-world.sqlite
```

#  Alternative Key Mappings (Dvorak)

I received a couple requests to add support for reconfiguring the mapping of keyboard inputs, so I've added an option to specify a JSON config file where you can customize which input characters will trigger different actions in the game:

```
--user-interaction-config-file custom_key_config.json
```

For a Dvorak keyboard, I believe the following should work to give you the same experience that you'd get on a querty keyboard (although I can't say for sure as I don't have a Dvorak keyboard):

```
{
	"ACTION_TAB_NEXT_FRAME": "	",
	"ACTION_HELP_MENU_TOGGLE": "",
	"ACTION_Y_PLUS": ",",
	"ACTION_Y_MINUS": "o",
	"ACTION_X_PLUS": "e",
	"ACTION_X_MINUS": "a",
	"ACTION_Z_PLUS": " ",
	"ACTION_Z_MINUS": "q",
	"ACTION_MINING": "m",
	"ACTION_CRAFTING": "j",
	"ACTION_QUIT": "'",
	"ACTION_PLACE_BLOCK": "l"
}
```

You can see the default user interaction config file printed to standard out by running the jar with the following parameter:

```
--print-user-interaction-config
```

#  License

See LICENSE.md
