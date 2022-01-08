package org.teamtators.common.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teamtators.common.config.Configurable;

import java.lang.reflect.Field;

/**
 * DO NOT USE IN COMPETITION
 * Provides runtime configuration of embedded Config class in a Configurable
 * Provide a getConfig() that returns a singleton instance of the embedded Config class
 * (usually used to tune Commands during runtime)
 * @see org.teamtators.bbt8r.commands.CommandRegistrar
 * Uses reflection
 */
// TODO: make sure this works
public interface Tunable<Config> extends Configurable<Config> {
    Logger logger = LoggerFactory.getLogger(Tunable.class);
    Config getConfig ();

    /**
     * attempts to change a value in the Config class
     * @param fieldName
     * @param value
     */
    default void changeValue (String fieldName, double value) {
        Config config = getConfig();
        Class<?> configClass = config.getClass();
        Field field;
        try {
            field = configClass.getDeclaredField(fieldName);
        } catch (NoSuchFieldException error) {
            logger.error(
                    "NO VALUE SET: Was not able to find field '{}' in the config of '{}'. {}",
                    fieldName,
                    configClass.getName(),
                    error.toString()
            );
            return;
        }
        try {
            field.set(config, value);
        } catch (IllegalAccessException error) {
            logger.error(
                    "NO VALUE SET: The value '{}' in '{}' may not be public. {}",
                    fieldName,
                    configClass.getName(),
                    error.toString()
            );
            return;
        }
        configure(config);
    }
}
