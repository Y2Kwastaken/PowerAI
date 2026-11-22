package sh.miles.powerai.util;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.regex.Pattern;

public class PlayerStorage {

    private static final String NAMESPACE = "powerai";
    private static final String SCRIPT_STORAGE = "scripts";

    public static final int MAX_SCRIPT_NAME_LENGTH = 32;
    // Script names are used as NamespacedKey keys, which only allow [a-z0-9/._-]. '/' is excluded to keep names flat.
    private static final Pattern SCRIPT_NAME_PATTERN = Pattern.compile("[a-z0-9._-]{1," + MAX_SCRIPT_NAME_LENGTH + "}");

    public static PersistentDataContainer getSubcontainer(Player player, String name) {
        final var container = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(NAMESPACE, name);
        if (container.has(key)) {
            return container.get(key, PersistentDataType.TAG_CONTAINER);
        }

        return container.getAdapterContext().newPersistentDataContainer();
    }

    public static void setSubcontainer(Player player, String name, PersistentDataContainer subcontainer) {
        final var container = player.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(NAMESPACE, name);
        container.set(key, PersistentDataType.TAG_CONTAINER, subcontainer);
    }

    public static boolean isValidScriptName(String scriptName) {
        return scriptName != null && SCRIPT_NAME_PATTERN.matcher(scriptName).matches();
    }

    public static List<String> getScripts(Player player) {
        var scripts = getSubcontainer(player, SCRIPT_STORAGE);
        return scripts.getKeys().stream().map(NamespacedKey::getKey).sorted().toList();
    }

    public static String getScript(Player player, String scriptName) {
        if (!isValidScriptName(scriptName)) {
            return null;
        }

        var scripts = getSubcontainer(player, SCRIPT_STORAGE);
        return scripts.get(new NamespacedKey(NAMESPACE, scriptName), PersistentDataType.STRING);
    }

    public static boolean hasScript(Player player, String scriptName) {
        return getScript(player, scriptName) != null;
    }

    public static boolean saveScript(Player player, String scriptName, String script) {
        if (!isValidScriptName(scriptName)) {
            return false;
        }

        var scripts = getSubcontainer(player, SCRIPT_STORAGE);
        scripts.set(new NamespacedKey(NAMESPACE, scriptName), PersistentDataType.STRING, script);
        // the sub container is a copy, so it has to be written back to the player
        setSubcontainer(player, SCRIPT_STORAGE, scripts);
        return true;
    }

    public static void deleteScript(Player player, String scriptName) {
        if (!isValidScriptName(scriptName)) {
            return;
        }

        var scripts = getSubcontainer(player, SCRIPT_STORAGE);
        scripts.remove(new NamespacedKey(NAMESPACE, scriptName));
        setSubcontainer(player, SCRIPT_STORAGE, scripts);
    }
}
