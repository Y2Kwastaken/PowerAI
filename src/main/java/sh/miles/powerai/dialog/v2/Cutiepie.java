package sh.miles.powerai.dialog.v2;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import static net.kyori.adventure.text.minimessage.MiniMessage.Preset.NON_INTERACTABLE;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

final class Cutiepie {

    static final MiniMessage COLORS = miniMessage(NON_INTERACTABLE);
    static Component component(String string, TagResolver... resolvers) {
        return miniMessage().deserialize(string, resolvers);
    }

    static String scrapeScriptDescription(String content) {
        String[] lines = content.split("\n");
        StringBuilder description = new StringBuilder();
        for (String line : lines) {
            if (!line.startsWith("--")) {
                break;
            }

            description.append(line.trim().replace("--", "")).append("\n");
        }

        if (!description.isEmpty() && description.charAt(description.length() - 1) == '\n') {
            description.setLength(description.length() - 1);
        }

        return description.toString();
    }

    static String scrapeFirstNRows(String content, int rows) {
        StringBuilder description = new StringBuilder();
        String[] lines = content.split("\n");
        for (int i = 0; i < Math.min(lines.length, rows); i++) {
            description.append(lines[i]).append("\n");
        }

        if (!description.isEmpty() && description.charAt(description.length() - 1) == '\n') {
            description.setLength(description.length() - 1);
        }

        return description.toString();
    }

    static int getScriptLineCount(String content) {
        return content.split("\n").length;
    }
}
