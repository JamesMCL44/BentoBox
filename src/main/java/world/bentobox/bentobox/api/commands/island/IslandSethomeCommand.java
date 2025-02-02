package world.bentobox.bentobox.api.commands.island;

import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.Nullable;

import world.bentobox.bentobox.api.commands.CompositeCommand;
import world.bentobox.bentobox.api.commands.ConfirmableCommand;
import world.bentobox.bentobox.api.configuration.WorldSettings;
import world.bentobox.bentobox.api.localization.TextVariables;
import world.bentobox.bentobox.api.user.User;
import world.bentobox.bentobox.database.objects.Island;
import world.bentobox.bentobox.managers.RanksManager;


public class IslandSethomeCommand extends ConfirmableCommand {

    private @Nullable Island island;

    public IslandSethomeCommand(CompositeCommand islandCommand) {
        super(islandCommand, "sethome");
    }

    @Override
    public void setup() {
        setPermission("island.sethome");
        setOnlyPlayer(true);
        setDescription("commands.island.sethome.description");
        setConfigurableRankCommand();
        setDefaultCommandRank(RanksManager.MEMBER_RANK);
    }

    @Override
    public boolean canExecute(User user, String label, List<String> args) {
        island = getIslands().getIsland(getWorld(), user);
        // Check island
        if (island == null || island.getOwner() == null) {
            user.sendMessage("general.errors.no-island");
            return false;
        }
        if (!island.onIsland(user.getLocation())) {
            user.sendMessage("commands.island.sethome.must-be-on-your-island");
            return false;
        }

        int rank = Objects.requireNonNull(island).getRank(user);
        if (rank < island.getRankCommand(getUsage())) {
            user.sendMessage("general.errors.insufficient-rank", TextVariables.RANK, user.getTranslation(getPlugin().getRanksManager().getRank(rank)));
            return false;
        }

        // Check number of homes
        int maxHomes = getIslands().getMaxHomes(island);
        if (getIslands().getNumberOfHomesIfAdded(island, String.join(" ", args)) > maxHomes) {
            user.sendMessage("commands.island.sethome.too-many-homes", TextVariables.NUMBER, String.valueOf(maxHomes));
            user.sendMessage("commands.island.sethome.homes-are");
            island.getHomes().keySet().stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s));
            return false;
        }
        return true;
    }

    @Override
    public boolean execute(User user, String label, List<String> args) {
        String number = String.join(" ", args);
        WorldSettings ws = getIWM().getWorldSettings(user.getWorld());
        // Check if the player is in the Nether
        if (getIWM().isNether(user.getWorld())) {
            // Check if he is (not) allowed to set his home here
            if (!ws.isAllowSetHomeInNether()) {
                user.sendMessage("commands.island.sethome.nether.not-allowed");
                return false;
            }

            // Check if a confirmation is required
            if (ws.isRequireConfirmationToSetHomeInNether()) {
                askConfirmation(user, user.getTranslation("commands.island.sethome.nether.confirmation"), () -> doSetHome(user, number));
            } else {
                doSetHome(user, number);
            }
        } else if (getIWM().isEnd(user.getWorld())) { // Check if the player is in the End
            // Check if he is (not) allowed to set his home here
            if (!ws.isAllowSetHomeInTheEnd()) {
                user.sendMessage("commands.island.sethome.the-end.not-allowed");
                return false;
            }

            // Check if a confirmation is required
            if (ws.isRequireConfirmationToSetHomeInTheEnd()) {
                askConfirmation(user, user.getTranslation("commands.island.sethome.the-end.confirmation"), () -> doSetHome(user, number));
            } else {
                doSetHome(user, number);
            }
        } else { // The player is in the Overworld, no need to run a check
            doSetHome(user, number);
        }
        return true;
    }

    private void doSetHome(User user, String name) {
        // Define a runnable as we will be using it often in the code below.
        getIslands().setHomeLocation(user, user.getLocation(), name);
        user.sendMessage("commands.island.sethome.home-set");
        if (island.getHomes().size() > 1) {
            user.sendMessage("commands.island.sethome.homes-are");
            island
            .getHomes()
            .keySet()
            .stream().filter(s -> !s.isEmpty()).forEach(s -> user.sendMessage("commands.island.sethome.home-list-syntax", TextVariables.NAME, s));
        }
    }
}
