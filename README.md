#  CONTRIBUTING

DO NOT CREATE PULL REQUESTS FOR THIS PROJECT.

ANY PULL REQUESTS YOU CREATE WILL NOT BE MERGED IN.

This project does not currently accept pull requests from the public.

Having said this, please do file issues if you notice something broken or undesirable.

#  Terminal Block Mining Simulation Game

![Terminal Block Mining Simulation Game](block-mining-simulation-game-thumbnail.png "Terminal Block Mining Simulation Game")

This project contains the 'Terminal Block Mining Simulation Game', a video game where you simulate mining blocks of iron ore in the terminal.  The game uses procedural terrain generation and the game world itself is infinite.  The terrain will start generating automatically in the background near the player.  All generated terrain and player data is stored in a SQLite database file.  The location of this world file defaults to the directory where you launch the game, but you can configure it with the '--block-world-file' flag.  You can also use the '--log-file' to set the location of a log file.  If the '--log-file' flag is omitted, logging will be disabled.

#  Player Movement

You can use the 'w', 'a', 's' 'd' keys to move around on the screen.  You can use the space bar to go up and the 'x' key to move down (assuming there isn't a solid block in the way on a different level).

#  Exiting/Quitting The Game

Press the 'q' key to quit the game.

#  Mining Blocks

Press the 'm' key to mine blocks.

#  Crafting

You can press the 'c' key to try and craft new blocks, such as metallic iron, and an iron pickaxe.  Currently, the game only supports these two crafting recipes.

#  Place Blocks

Press the 'p' key to place blocks (currently only supports placing rock blocks).

#  Supported Platforms

Currently, the game has only been tested to work on a default installation of Ubuntu Linux.

#  Launching The Game

Compiling the game from scratch is not necessary.  You can download pre-compiled .jar files from GitHub in the 'Releases' section for this repo:

```
wget https://github.com/RobertElderSoftware/robert-elder-software-java-modules/releases/download/0.0.3/v2_block_schema.json
wget https://github.com/RobertElderSoftware/robert-elder-software-java-modules/releases/download/0.0.3/block-manager-single-player-client-0.0.3.jar
java -jar block-manager-single-player-client-0.0.3.jar
```

The game should immediately launch and fill up the terminal with graphics.  You can exit the game by pressing the 'q' key.  By default, the game saves it's world data into a SQLite database file that lives in the current directory.

#  Verify The Jar Signature (Optional)

If you are concerned about the authenticity of the .jar file, you can also verify the signature using GPG:

```
wget https://github.com/RobertElderSoftware/robert-elder-software-java-modules/releases/download/0.0.3/block-manager-single-player-client-0.0.3.jar.asc
gpg --search-keys robert@robertelder.org
#  Should match the key for 'robert@robertelder.org'
gpg --recv-keys ECBD481DBCA5C48804FBD08720B9852CF0558BAA
gpg --verify block-manager-single-player-client-0.0.3.jar.asc block-manager-single-player-client-0.0.3.jar
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

To build the game, you will need to set up a development environment that can support Java 17 and a version of maven that can support Java 17.

Next, you can compile the game from source by running this command:

```
./res-modules/block-manager-single-player-client/run_single_player_client.sh
```

Once it finishes building, it should launch right into the game.

#  License

See LICENSE.md
