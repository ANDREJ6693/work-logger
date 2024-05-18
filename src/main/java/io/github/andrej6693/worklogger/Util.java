package io.github.andrej6693.worklogger;

import com.google.gson.*;
import com.moandjiezana.toml.Toml;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Util {
    private Util () {}

    public record Config(String botToken, String currency, List<PaymentType> paymentTypes) {}
    public static Config CONFIG = null;

    public record MonthData(PayStatus payStatus, List<WorkEntry> workEntries) {}
    public record WorkEntry(PaymentType paymentType, List<WorkDetail> workDetails) {}
    public record WorkDetail(Date date, int duration, String note) {}
    public record PayStatus(boolean payed, int amount) {}
    public record PaymentType(String tag, int type) {}

    public static void loadConfig() {
        CONFIG = new Toml().read(new File("config.toml")).to(Config.class);
    }
    public static void createDefaultConfigIfNotExists() {
        try (InputStream inputStream = Main.class.getResourceAsStream("/default-config.toml")) {
            assert inputStream != null;
            Files.copy(inputStream, Path.of("config.toml"));
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create default config file.", e);
        }
    }

    public static void addData(PaymentType paymentType, WorkDetail workDetail, Path path) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").setPrettyPrinting().create();
        MonthData monthData = readMonthDataFromFile(path);

        if (monthData == null) {
            monthData = new MonthData(new PayStatus(false, 0),new ArrayList<>());
        }

        if (monthData.workEntries.isEmpty()) {
            monthData.workEntries.add(new WorkEntry(paymentType, new ArrayList<>()));
        }

        int index;
        for (index = 0; index < monthData.workEntries.size(); index++) {
            if (Objects.equals(monthData.workEntries.get(index).paymentType, paymentType)) {
                break;
            }
        }

        if (index == monthData.workEntries.size()) {
            monthData.workEntries.add(new WorkEntry(paymentType, new ArrayList<>()));
            monthData.workEntries.getLast().workDetails.add(workDetail);
        } else {
            monthData.workEntries.get(index).workDetails.add(workDetail);
        }


        String jsonData = gson.toJson(monthData);
        try (FileWriter fileWriter = new FileWriter(path.toFile())) {
            fileWriter.write(jsonData);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void createMonthJsonIfNotExists(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.createFile(path);
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create data folder or file.", e);
        }

        try {
            Gson gson = new Gson();
            String jsonData = gson.toJson(new MonthData(new PayStatus(false, 0), new ArrayList<>()));
            Files.createDirectories(path.getParent());
            Files.createFile(path);
            try (FileWriter fileWriter = new FileWriter(path.toFile())) {
                fileWriter.write(jsonData);
            }
        } catch (FileAlreadyExistsException ignored) {
        } catch (IOException e) {
            throw new RuntimeException("Unable to create data folder or file.", e);
        }
    }

    public static Date parseDate(String stringDate) throws ParseException {
        DateFormat df = new SimpleDateFormat("d/M/y");
        return df.parse(stringDate);
    }

    public static String getMonthName(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return new SimpleDateFormat("MMMM", Locale.ENGLISH).format(date);
    }

    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static String getDateString(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag("sl-SI"));
        return df.format(calendar.getTime());
    }

    private static MonthData readMonthDataFromFile(Path path) {
        Gson gson = new Gson();
        try {
            return gson.fromJson(Files.readString(path), MonthData.class);
        } catch (IOException e) {
            throw new RuntimeException("Error reading MonthData from file", e);
        }
    }

    public static int getPaymentTypeIndex(PaymentType paymentType) {
        for (int i = 0; i < CONFIG.paymentTypes().size(); i++) {
            if (CONFIG.paymentTypes.get(i) == paymentType) {
                return i;
            }
        }
        return 0;
    }

    public static PaymentType getPaymentTypeFromIndex(int index) {
        return CONFIG.paymentTypes.get(index);
    }

    public static List<Command.Choice> collectJsonFiles(String query) {
        List<String> jsonFiles;
        try (Stream<Path> paths = Files.walk(Paths.get("./data/"), FileVisitOption.FOLLOW_LINKS)) {
            jsonFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .map(fileName -> fileName.substring(0, fileName.length() - 5))
                    .toList();
        } catch (IOException e) {
            return new ArrayList<>();
        }

        String lowercaseQuery = query.toLowerCase();

        return jsonFiles.stream()
                .filter(item -> item.toLowerCase().contains(lowercaseQuery))
                .limit(25)
                .map(item -> new Command.Choice(item, item.toLowerCase()))
                .collect(Collectors.toList());
    }

    public static String getPathFromDate(String fileName) {
        String finalFileName = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
        try (Stream<Path> paths = Files.walk(Paths.get("./data/"), FileVisitOption.FOLLOW_LINKS)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> p.getFileName().toString().equals(finalFileName + ".json"))
                    .map(Path::toString)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            return "no date";
        }
    }
}