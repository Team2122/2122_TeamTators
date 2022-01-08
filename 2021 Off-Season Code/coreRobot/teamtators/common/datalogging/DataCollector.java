package org.teamtators.common.datalogging;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.control.Timer;
import org.teamtators.common.control.Updatable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.AccessControlException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects quantitative data from various sources on the robot and logs it to a file
 */

public class DataCollector implements Updatable {
    public static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
    private static final Logger logger = LoggerFactory.getLogger(DataCollector.class);
    private String outputDir;
    private static DataCollector collector = null;
    private Set<ProviderUsage> providers = ConcurrentHashMap.newKeySet();
    private boolean cannotLog = false;
    private DataCollector() {
        try {
            outputDir = System.getProperty("tator.logdir") + "/datalogs";
            File outputDirFile = new File(outputDir);
            if (outputDirFile.mkdirs()) {
                logger.debug("Created data log directory {}", outputDirFile);
            } else {
                logger.debug("Did not create data log directory {}", outputDirFile);
                // is it a security (permissions) issue?
                var sm = System.getSecurityManager();

                if ( org.teamtators.common.Robot.isReal() ){
                    try {
                        sm.checkWrite(outputDir);
                    } catch (AccessControlException e2) {
                        logger.error("No permission to write files on RoboRio!!! ;("); // todo make sure this works
                    }
                cannotLog = true;
                }
            }
        } catch (Throwable e) {
            logger.error("Could not create data collector output directory", e);
            cannotLog = true;
        }
    }

    public static synchronized DataCollector getDataCollector() {
        if (collector == null) collector = new DataCollector();
        return collector;
    }

    public String getOutputDir() {
        return outputDir;
    }

    /**
     * Register a new data provider to be used periodically
     *
     * @param provider New provider to add
     */
    public void startProvider(LogDataProvider provider) {
        startProvider(null, provider);
    }

    public String getFilename(String timestamp, String name) {
        return String.format("%s/%s %s.csv", outputDir, timestamp, name);
    }

    public String getFilename(String subdir, String timestamp, String name) {
        return String.format("%s/%s/%s %s.csv", outputDir, subdir, timestamp, name);
    }

    public String getFormattedNow() {
        return DATE_FORMAT.format(new Date());
    }

    public void startProvider(String subdir, LogDataProvider provider) {
        if (cannotLog) return;
        Preconditions.checkNotNull(provider);
        if (providers.stream().anyMatch(u -> u.provider == provider))
            return;
        ProviderUsage usage = new ProviderUsage(provider, null, null);
        String timestamp = getFormattedNow();
        String fileName;
        if(subdir == null) {
            fileName = getFilename(timestamp, provider.getName());
        } else {
            fileName = getFilename(subdir, timestamp, provider.getName());
        }
        providers.add(usage);
        logger.debug("Starting data logging for {} to {}", provider.getName(), fileName);
        new Thread(() -> {
            FileWriter writer;
            CSVPrinter printer;
            try {
                writer = new FileWriter(fileName);
                printer = new CSVPrinter(writer, CSVFormat.EXCEL);
            } catch (IOException e) {
                logger.error("Failed to create outputs for new data provider " + provider.getName(), e);
                return;
            }
            Iterable<Object> keys = Iterables.concat(Collections.singletonList("timestamp"), provider.getKeys());
            try {
                printer.printRecord(keys);
                synchronized (usage.savedRows) {
                    for (Iterable<Object> row : usage.savedRows) {
                        printer.printRecord(row);
                    }
                }
            } catch (IOException e) {
                logger.error("Error writing to CSVWriter", e);
            }
            usage.writer = writer;
            usage.csvPrinter = printer;
        }, "startProvider-" + provider.getName()).start();
    }

    public void createOrEmptySubdirectory(String subdir) {
        try {
            File subdirFile = new File(String.format("%s/%s", outputDir, subdir));
            if(subdirFile.exists() && subdirFile.isDirectory()) {
                File[] files = subdirFile.listFiles();
                if(files != null) {
                    for(File f : files) {
                        f.delete();
                    }
                }
                subdirFile.delete();
                subdirFile.mkdirs();
            } else {
                subdirFile.mkdirs();
            }
        } catch (Exception e) {
            logger.error("Error clearing subdirectory", e);
        }
    }

    /**
     * Retire a data provider
     *
     * @param provider Provider to remove
     */
    public void stopProvider(LogDataProvider provider) {
        if (cannotLog) return;
        Preconditions.checkNotNull(provider);
        Optional<ProviderUsage> providerUsage = providers.stream()
                .filter(current -> current.provider.getName().equals(provider.getName()))
                .findFirst();
        if (providerUsage.isPresent()) {
            ProviderUsage usage = providerUsage.get();
            providers.remove(usage);
            new Thread(() -> {
                logger.debug("Stopping datalogging and flushing file for provider {}", provider.getName());
                try {
                    if (usage.csvPrinter != null) {
                        usage.csvPrinter.flush();
                        usage.csvPrinter.close();
                    }
                } catch (IOException e) {
                    logger.error("Error flushing csv file", e);
                }
            }, "stopProvider-" + provider.getName()).start();
        }
    }

    private void addRow(ProviderUsage usage) {
        try {
            Iterable<Object> row = Iterables.concat(Collections.singletonList(Timer.getTimestamp()),
                    usage.provider.getValues());
            if (usage.csvPrinter != null) {
                usage.csvPrinter.printRecord(row);
            } else {
                synchronized (usage.savedRows) {
                    usage.savedRows.add(row);
                }
            }
        } catch (IOException e) {
            logger.error("Exception while printing data log record", e);
        }
    }

    @Override
    public void update(double delta) {
        for (ProviderUsage providerUsage : providers) {
            addRow(providerUsage);
        }
    }

    @Override
    public String getName() {
        return "DataCollector";
    }

    public String getSubdirectory(String subdir) {
        return String.format("%s/%s", outputDir, subdir);
    }

    private class ProviderUsage {
        LogDataProvider provider;
        FileWriter writer;
        CSVPrinter csvPrinter;
        public final ArrayList<Iterable<Object>> savedRows = new ArrayList<>();

        ProviderUsage(LogDataProvider provider, FileWriter writer, CSVPrinter csvPrinter) {
            this.provider = provider;
            this.writer = writer;
            this.csvPrinter = csvPrinter;
        }
    }
}
