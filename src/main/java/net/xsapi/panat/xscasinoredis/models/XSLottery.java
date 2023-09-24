package net.xsapi.panat.xscasinoredis.models;

import com.google.gson.Gson;
import dev.unnm3d.rediseconomy.currency.Currency;
import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.xscore.XSHandlers;
import net.xsapi.panat.xscasinoredis.xscore.core;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class XSLottery {

    private int lockPrize = -1;
    private String setterLockPrize = "";
    private long potPrize;
    private long potExtra;
    private String winnerName = "";
    private HashMap<String,Integer> dataLottery = new HashMap<>();
    private HashMap<Integer,Integer> lotteryList = new HashMap<>();
    private int amountTicket = 0;
    private static long nextPrizeTime = 0L;
    private String prizeString;

    public String getPrizeString() {
        return prizeString;
    }

    public void setPrizeString(String prizeString) {
        this.prizeString = prizeString;
    }

    public int getAmountTicket() {
        return amountTicket;
    }

    public void setAmountTicket(int amountTicket) {
        this.amountTicket = amountTicket;
    }

    public void addTicket(int amount) {
        this.amountTicket += amount;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public void setPotPrize(long potPrize) {
        this.potPrize = potPrize;
    }

    public long getPotPrize() {
        return potPrize;
    }

    public HashMap<Integer, Integer> getLotteryList() {
        return lotteryList;
    }

    public long getNextPrizeTime() {
        return nextPrizeTime;
    }

    public void setNextPrizeTime(long nextPrizeTime) {
        XSLottery.nextPrizeTime = nextPrizeTime;
    }

    public HashMap<String, Integer> getDataLottery() {
        return dataLottery;
    }

    public String getSetterLockPrize() {
        return setterLockPrize;
    }

    public void setSetterLockPrize(String setterLockPrize) {
        this.setterLockPrize = setterLockPrize;
    }

    public int getLockPrize() {
        return lockPrize;
    }

    public void setLockPrize(int lockPrize) {
        this.lockPrize = lockPrize;
    }

    public void setPotExtra(long potExtra) {
        this.potExtra = potExtra;
    }

    public long getPotExtra() {
        return potExtra;
    }

    public XSLottery() {
        createSQL(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
        setPotPrize(config.customConfig.getLong("config.potPrize"));
        setPotExtra(config.customConfig.getLong("config.potExtra"));
        setPrizeString(config.customConfig.getString("config.prizeTime"));
        loadDataSQL(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
    }

    public void createSQL(String JDBC_URL,String USER,String PASS) {
        try {
            Connection connection = DriverManager.getConnection(JDBC_URL,USER,PASS);

            DatabaseMetaData metaData = connection.getMetaData();
            ResultSet resultSet = metaData.getTables(null, null, XSHandlers.getTableLottery(), null);
            boolean tableExists = resultSet.next();

            if(!tableExists) {
                Statement statement = connection.createStatement();

                String createTableQuery = "CREATE TABLE " + XSHandlers.getTableLottery() + " ("
                        + "lotteryList TEXT DEFAULT '[]', "
                        + "NextPrizeTime BIGINT DEFAULT 0, "
                        + "winnerName VARCHAR(16) DEFAULT '', "
                        + "winnerNumber VARCHAR(2) DEFAULT '', "
                        + "winnerNumberTicket VARCHAR(10) DEFAULT '', "
                        + "winnerPrize VARCHAR(20) DEFAULT '', "
                        + "lockPrize VARCHAR(2) DEFAULT '', "
                        + "lockSetter VARCHAR(16) DEFAULT ''"
                        + ")";

                statement.executeUpdate(createTableQuery);


                Statement statementInsert = connection.createStatement();

                String insertQuery = "INSERT INTO " + XSHandlers.getTableLottery() + " (lotteryList) "
                        + "VALUES ('[]')";

                statementInsert.executeUpdate(insertQuery);
                statementInsert.close();
                statement.close();
            }
            connection.close();

            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCasino] Lottery Database : §x§6§0§F§F§0§0Connected");
        } catch (SQLException e) {
            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSasino] Lottery Database : §x§C§3§0§C§2§ANot Connected");
            e.printStackTrace();
        }
    }

    public void loadDataSQL(String JDBC_URL, String USER, String PASS) {
        try {
            Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASS) ;

            Statement statement = connection.createStatement();

            String selectQuery = "SELECT * FROM " + XSHandlers.getTableLottery();

            ResultSet resultSet = statement.executeQuery(selectQuery);

            if (resultSet.next()) {
                String lotteryListData = resultSet.getString("lotteryList");
                String lockSetter = resultSet.getString("lockSetter");
                int lockPrize = resultSet.getInt("lockPrize");
                //long prizeTime = resultSet.getLong("NextPrizeTime");
                int amtTicket = 0;
                if(!lotteryListData.equalsIgnoreCase("[]")) {
                    lotteryListData = lotteryListData.replaceAll("\\[|\\]", "");
                    String[] dataArray = lotteryListData.split(",");

                    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(dataArray));
                    for (String lottery : arrayList) {
                        int key = Integer.parseInt(lottery.trim().split(":")[0]);
                        int amount = Integer.parseInt(lottery.trim().split(":")[1]);
                        lotteryList.put(key,amount);
                        amtTicket += amount;
                    }
                }
                setLockPrize(lockPrize);
                setSetterLockPrize(lockSetter);
                setAmountTicket(amtTicket);
                //setNextPrizeTime(prizeTime);
                setNextPrizeTime(XSHandlers.calculateTimeRedis(getPrizeString()) + System.currentTimeMillis());
                //Bukkit.broadcastMessage("Next Time: " + XSHandlers.calculateTimeRedis(getPrizeString()) + System.currentTimeMillis());

                Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCasinoRedis] Lottery loaded data from database successfully");
            }
            resultSet.close();
            statement.close();
            connection.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void xsLotteryEndEvent() {
        int prizeNum;

        //Bukkit.broadcastMessage("LockPrize: " + getLockPrize());
        if(getLockPrize() == -1) {
            prizeNum = generatePrizeNumber();
        } else {
            prizeNum = getLockPrize();
        }
        setLockPrize(-1);
        setSetterLockPrize("");

        core.sendMessageToRedisAsync("XSCasinoRedisData/XSLottery/EndPrizeNumber/"+core.getRedisCrossServerHostName(),"" + prizeNum);

       // Bukkit.broadcastMessage("Waiting system calculate data...");
        Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
            @Override
            public void run() {
                calculateWinner();
            }
        }, 100L);
    }

    public void calculateWinner() {
        //Bukkit.broadcastMessage("Send... Data Winner");

        Gson gson = new Gson();
        String json = gson.toJson(dataLottery);

        int amountTicket = 0;
        for(Map.Entry<String,Integer> data : dataLottery.entrySet()) {
            amountTicket += data.getValue();
        }
        //Bukkit.broadcastMessage("Winticket: " + amountTicket);
        int maxWinTicket = 0;

        //Bukkit.broadcastMessage("Amt Ticket-> " + getAmountTicket());
        //Bukkit.broadcastMessage("Pot Extra-> " + getPotExtra());

        setPotPrize(getPotPrize() + (getAmountTicket()*getPotExtra()));
        //Bukkit.broadcastMessage("Pot Prize: " + getPotPrize());
        //Bukkit.broadcastMessage("Winner String " + dataLottery);

        boolean isUsingRedisEcon = false;
        Currency currency = null;
        if(XSHandlers.getRedisEconomyAPI() != null) {
            isUsingRedisEcon = true;
            currency = XSHandlers.getRedisEconomyAPI().getCurrencyByName("vault");
        }

        for (Map.Entry<String,Integer> winner : dataLottery.entrySet()) {
            double prizePool = (double) winner.getValue() / amountTicket;
            double reward = (getPotPrize() * prizePool);

            if(isUsingRedisEcon) {
                currency.depositPlayer(winner.getKey(),reward);

            } else {
                XSHandlers.getEconomy().depositPlayer(Bukkit.getPlayer(winner.getKey()),reward);
            }

            if(winner.getValue() > maxWinTicket) {
                maxWinTicket = winner.getValue();
                setWinnerName(winner.getKey());
            }
        }

        core.sendMessageToRedisAsync("XSCasinoRedisData/XSLottery/EndSendWinnerList/"+core.getRedisCrossServerHostName()
                ,json+"XSCASINO_ESCAPE_PREFIX"+getWinnerName()+"XSCASINO_ESCAPE_PREFIX"+amountTicket);
        clearData();
    }

    public void clearData() {
        dataLottery.clear();
        lotteryList.clear();
        setAmountTicket(0);
        setPotPrize(config.customConfig.getLong("config.potPrize"));
        saveTOSQL(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
        resetPlayerData(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
    }

    public void resetPlayerData(String JDBC_URL,String USER,String PASS) {
        try {
            Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASS);

            String updateQuery = "UPDATE " + XSHandlers.getTableXSPlayer() + " SET lotteryList = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(updateQuery);
            preparedStatement.setString(1, "[]");
            preparedStatement.executeUpdate();

            preparedStatement.close();
            connection.close();

            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSAPI Casino] Lottery Database : §x§6§0§F§F§0§0Reset!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveTOSQL(String JDBC_URL,String USER,String PASS) {
        try {
            Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASS);
            PreparedStatement preparedStatement = null;
            String updateQuery = "UPDATE " +XSHandlers.getTableLottery() + " SET lotteryList=?, NextPrizeTime=?, lockPrize=?, lockSetter=? LIMIT 1";

            preparedStatement = connection.prepareStatement(updateQuery);

            ArrayList<String> lotteryList = new ArrayList<>();
            for(Map.Entry<Integer,Integer> lottery : getLotteryList().entrySet()) {
                lotteryList.add(lottery.getKey()+":"+lottery.getValue());
            }

            preparedStatement.setString(1, String.valueOf(lotteryList));
            preparedStatement.setLong(2, getNextPrizeTime());
            preparedStatement.setInt(3, getLockPrize());
            preparedStatement.setString(4, getSetterLockPrize());

//            Bukkit.broadcastMessage("lockPrize: " + getLockPrize());
 //           Bukkit.broadcastMessage("lockPrizeSetter: " + getSetterLockPrize());

            preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();

            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCASINO REDIS] Lottery Database : §x§6§0§F§F§0§0Saved!");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int generatePrizeNumber() {
        Random r = new Random();
        int rand = r.nextInt(99);
        return rand;
    }
}
