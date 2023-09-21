package net.xsapi.panat.xscasinoredis.xscore;

import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.models.XSLottery;
import org.bukkit.Bukkit;
import java.time.LocalTime;

public class XSHandlers {

    private static String timer;
    private static XSLottery xsLottery;

    //MySQL Connection
    private static boolean usingSQL = false;
    private static String JDBC_URL;
    private static String USER;
    private static String PASS;
    private static String DB_TABLE;
    private static String TABLE_LOTTERY = "xscasino_lottery";

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

    public static void setUpConfig() {
        timer = config.customConfig.getString("config.prizeTime");
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

    public static XSLottery getXsLottery() {
        return xsLottery;
    }

    public static void saveData() {
       // config.customConfig.set("config.prizeMillis", timerMillis);
        config.customConfig.set("config.lockPrize", xsLottery.getLockPrize());
        config.customConfig.set("config.lockPrizeSetter", xsLottery.getSetterLockPrize());
        config.save();
        config.reload();

        getXsLottery().saveTOSQL(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
        Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCasinoRedis] Data : §x§6§0§F§F§0§0Saved");
    }

    public static void createTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(core.getPlugin(), new Runnable() {
            @Override
            public void run() {

                LocalTime currentTime = LocalTime.now();

                LocalTime timerLocalTime = LocalTime.parse(timer);

                if (currentTime.isAfter(timerLocalTime) && System.currentTimeMillis() - getXsLottery().getNextPrizeTime() >= 0L) {
                    //Bukkit.broadcastMessage("SEND NOW! " + timer + " " + currentTime + "<<");
                    getXsLottery().setNextPrizeTime(System.currentTimeMillis() + (86400L * 1000L));
                    getXsLottery().xsLotteryEndEvent();
                }
            }
        }, 0L, 20L);
    }

}
