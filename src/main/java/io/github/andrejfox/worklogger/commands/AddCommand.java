package io.github.andrejfox.worklogger.commands;

import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;

import static io.github.andrejfox.worklogger.Util.*;

public class AddCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("add")) {
            int year;
            String month;
            Date date;
            try {
                if (event.getOption("date") == null) {
                    date = new Date();
                } else {
                    date = parseDate(Objects.requireNonNull(event.getOption("date")).getAsString());
                }
                year = getYear(date);
                month = getMonthName(date);
            } catch (ParseException e) {
                System.out.println("Invalid date format:\n" + e);
                event.reply("Invalid date format:\n" + e).queue();
                return;

            }
            Path path = Path.of("data/" + year + "/" + month + "_" + year + ".json");
            int index = Objects.requireNonNull(event.getOption("type")).getAsInt();
            WorkDetail workDetail = new WorkDetail(
                    date,
                    Objects.requireNonNull(event.getOption("duration")).getAsInt(),
                    Objects.requireNonNull(event.getOption("note")).getAsString()
            );

            createMonthJsonIfNotExists(path);
            addData(getPaymentTypeFromIndex(index), workDetail, path);

            updateNotPayedBoard();

            String[] pathArr = path.toString().split("/");
            String fileName = pathArr[pathArr.length - 1];
            String fileName2 = fileName.substring(0, fileName.length() - 5);
            fileName2 = fileName2.replace("_", " ");
            System.out.println("/add: [" + fileName + "] <" + getPaymentTypeFromIndex(index).tag() + "> " + workDetail);
            event.reply("Added " + fileName2 + " [" + getPaymentTypeFromIndex(index).tag() + "]: " + workDetail).setEphemeral(true).queue();
        }
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("add") && event.getFocusedOption().getName().equals("type")) {
            String userInput = event.getFocusedOption().getValue();
            List<Command.Choice> options = collectTypes(userInput);
            boolean isValidInput = options.stream().anyMatch(choice -> choice.getName().equalsIgnoreCase(userInput));

            if (!isValidInput) {
                event.replyChoices(options).queue();
            }
        }
    }

    public static CommandData register() {
        return Commands.slash("add", "Add work.")
                .addOption(OptionType.INTEGER, "type", "Type of payment.", true, true)
                .addOption(OptionType.STRING, "duration", "Duration of work.", true)
                .addOption(OptionType.STRING, "note", "Description of work.", true)
                .addOption(OptionType.STRING, "date", "Date of work. Format: dd/mm/yyyy", false);
    }
}