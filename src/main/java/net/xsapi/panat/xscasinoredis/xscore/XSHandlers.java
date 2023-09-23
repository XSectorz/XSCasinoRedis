package net.xsapi.panat.xscasinoredis.xscore;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.models.XSLottery;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.time.Duration;
import java.time.LocalTime;

public class XSHandlers {

    private static XSLottery xsLottery;

    //MySQL Connection
    private static boolean usingSQL = false;
    private static String JDBC_URL;
    private static String USER;
    private static String PASS;
    private static String DB_TABLE;
    private static String TABLE_LOTTERY = "xscasino_lottery";
    private static String TABLE_XSPLAYER = "xscasino_user";


    //Vault API
    private static Economy econ = null;
    private static Permission perms = null;

    public static boolean getUsingSQL() { return usingSQL; }
    public static String getJDBC_URL() {
        return JDBC_URL;
    }

    public static String getUSER() {
        return USER;
    }

    public static String getPASS() {
        return PASS;
    }

    public static String getTableLottery() {
        return TABLE_LOTTERY;
    }
    public static String getTableXSPlayer() {
        return TABLE_XSPLAYER;
    }

    public static void setUpConfig() {
        //timerMillis = config.customConfig.getLong("config.prizeMillis");
        usingSQL = config.customConfig.getBoolean("database.enable");

        if(getUsingSQL()) {
            String host = config.customConfig.getString("database.host");
            DB_TABLE = config.customConfig.getString("database.dbTable");
            JDBC_URL = "jdbc:mysql://" + host +  "/" + DB_TABLE;
            USER = config.customConfig.getString("database.user");
            PASS = config.customConfig.getString("database.password");
        }


        xsLottery = new XSLottery();
    }

    public static void setupAPI() {
        if (core.getPlugin().getServer().getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getConsoleSender().sendMessage("§x§f§f§a§c§2§f[XSCASINO REDIS] Vault : §x§D§F§1§C§6§3Not Found!");
            core.getPlugin().getServer().getPluginManager().disablePlugin(core.getPlugin());
            Bukkit.getConsoleSender().sendMessage("§x§f§f§a§c§2§f[XSCASINO REDIS] §x§D§F§1§C§6§3Plugin Disabled due not found vault!");
        } else {
            RegisteredServiceProvider<Economy> rsp = core.getPlugin().getServer().getServicesManager().getRegistration(Economy.class);
            econ = rsp.getProvider();
            RegisteredServiceProvider<Permission> rspPermission = core.getPlugin().getServer().getServicesManager().getRegistration(Permission.class);
            perms = rspPermission.getProvider();
            Bukkit.getConsoleSender().sendMessage("§x§f§f§a§c§2§f[XSCASINO REDIS] Vault : §x§2§F§C§0§2§0Found!");
        }
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static Permission getPerms() {
        return perms;
    }

    public static XSLottery getXsLottery() {
        return xsLottery;
    }

    public static void saveData() {
       // config.customConfig.set("config.prizeMillis", timerMillis);
        config.customConfig.set("config.lockPrize", xsLottery.getLockPrize());
        config.customConfig.set("config.lockPrizeSetter", xsLottery.getSetterLockPrize());
        //config.save();
        //config.reload();

        getXsLottery().saveTOSQL(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
        Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCasinoRedis] Data : §x§6§0§F§F§0§0Saved");
    }

    public static void createTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(core.getPlugin(), new Runnable() {
            @Override
            public void run() {

                LocalTime currentTime = LocalTime.now();
                String time = getXsLottery().getPrizeString();

                LocalTime targetTime = LocalTime.of(Integer.parseInt(time.split(":")[0]), Integer.parseInt(time.split(":")[1]));

                Bukkit.broadcastMessage("NextPrize:" + convertTime(Math.abs(System.currentTimeMillis() - getXsLottery().getNextPrizeTime())));

                if (currentTime.isAfter(targetTime) && System.currentTimeMillis() - getXsLottery().getNextPrizeTime() >= 0L) {
                    //Bukkit.broadcastMessage("SEND NOW! " + timer + " " + currentTime + "<<");
                    getXsLottery().setNextPrizeTime(System.currentTimeMillis() + calculateTimeRedis(getXsLottery().getPrizeString()));
                    Bukkit.broadcastMessage("[TEST] CurrentTime" + System.currentTimeMillis());
                    Bukkit.broadcastMessage("[TEST] NextPrizeTime" + getXsLottery().getNextPrizeTime());
                    getXsLottery().xsLotteryEndEvent();
                }
            }
        }, 0L, 20L);
    }

    public static long calculateTimeRedis(String time) {

        LocalTime currentTime = LocalTime.now();
        //LocalTime currentTime =LocalTime.of(0, 1);
        LocalTime targetTime = LocalTime.of(Integer.parseInt(time.split(":")[0]), Integer.parseInt(time.split(":")[1]));
        Duration duration;
        if (currentTime.isAfter(targetTime)) {
            duration = Duration.between(targetTime, currentTime);
            Bukkit.broadcastMessage("CURRENT : " +currentTime.getHour() + ":" +currentTime.getMinute());
            Bukkit.broadcastMessage("TARGET : " +targetTime.getHour()+":"+targetTime.getMinute());
            Bukkit.broadcastMessage("DIFF : " + duration.toMinutes()%60);
            Bukkit.broadcastMessage("------------------------------------");
            targetTime = LocalTime.of((int) (23-duration.toHours()), (int) (59-(duration.toMinutes()%60)),59);
            //Bukkit.broadcastMessage("TIME: " + (23-duration.toHours() + " : " + (60-(duration.toMinutes()%60))));
            currentTime =LocalTime.of(0, 0);
        }
        duration = Duration.between(currentTime, targetTime);
        Bukkit.broadcastMessage("CURRENT2 : " +currentTime.getHour() + ":" +currentTime.getMinute());
        Bukkit.broadcastMessage("TARGET2 : " +targetTime.getHour()+":"+targetTime.getMinute());
        Bukkit.broadcastMessage("DIFF2 : " + duration.toMinutes()%60);
        Bukkit.broadcastMessage("Duration: " + Math.abs(duration.toMillis()));
        Bukkit.broadcastMessage("------------------------------------");

        return Math.abs(duration.toMillis());

    }

    public static String convertTime(long millis) {
        int seconds = (int) (millis / 1000) % 60;
        int minutes = (int) ((millis / (1000 * 60)) % 60);
        int hours = (int) ((millis / (1000 * 60 * 60)) % 24);
        long days = millis / (1000 * 60 * 60 * 24);

        String timer = "";

        if(days >= 1) {
            timer += days + " ";
        }
        if(hours >= 1) {
            timer += hours + " ";
        }
        if(minutes >= 1) {
            timer += minutes + " ";
        }

        if(seconds >= 1) {
            timer += seconds + " ";
        }

        if(timer.isEmpty()) {
            timer += "SOON";
        }

        return timer;
    }

}
