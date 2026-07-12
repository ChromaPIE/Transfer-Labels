package com.buuz135.transfer_labels;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static int amountUpgrades = 63;
    public static int speedUpgrades = 38;
    public static int baseItemTransferAmount = 1;
    public static int fluidTransferMultiplier = 100;
    public static int filterSlots = 20;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

        amountUpgrades = configuration.getInt(
            "amountUpgrades",
            Configuration.CATEGORY_GENERAL,
            amountUpgrades,
            0,
            63,
            "How many amount upgrades a transfer label can accept.");
        speedUpgrades = configuration.getInt(
            "speedUpgrades",
            Configuration.CATEGORY_GENERAL,
            speedUpgrades,
            0,
            38,
            "How many speed upgrades a transfer label can accept.");
        baseItemTransferAmount = configuration.getInt(
            "baseItemTransferAmount",
            Configuration.CATEGORY_GENERAL,
            baseItemTransferAmount,
            0,
            Integer.MAX_VALUE,
            "Base item amount a transfer label moves before amount upgrades are applied.");
        fluidTransferMultiplier = configuration.getInt(
            "fluidTransferMultiplier",
            Configuration.CATEGORY_GENERAL,
            fluidTransferMultiplier,
            0,
            Integer.MAX_VALUE,
            "How many millibuckets a fluid transfer label moves per item transfer amount.");
        filterSlots = configuration.getInt(
            "filterSlots",
            Configuration.CATEGORY_GENERAL,
            filterSlots,
            1,
            20,
            "How many filter slots transfer labels have.");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
