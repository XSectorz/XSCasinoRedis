package net.xsapi.panat.xscasinoredis.models;

import com.google.gson.Gson;
import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.xscore.XSHandlers;
import net.xsapi.panat.xscasinoredis.xscore.core;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.*;

public class XSLottery {

    private int lockPrize = -1;
    private String setterLockPrize = "";
    private HashMap<String,Integer> dataLottery = new HashMap<>();
    private HashMap<Integer,Integer> lotteryList = new HashMap<>();
    private static long nextPrizeTime = 0L;

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

    public XSLottery() {
        setLockPrize(config.customConfig.getInt("config.lockPrize"));
        setSetterLockPrize(config.customConfig.getString("config.lockPrizeSetter"));

        loadDataSQL(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
    }

    public void loadDataSQL(String JDBC_URL, String USER, String PASS) {
        try {
            Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASS) ;

            Statement statement = connection.createStatement();

            String selectQuery = "SELECT * FROM " + XSHandlers.getTableLottery();

            ResultSet resultSet = statement.executeQuery(selectQuery);

            if (resultSet.next()) {
                String lotteryListData = resultSet.getString("lotteryList");
                long prizeTime = resultSet.getLong("NextPrizeTime");
                if(!lotteryListData.equalsIgnoreCase("[]")) {
                    lotteryListData = lotteryListData.replaceAll("\\[|\\]", "");
                    String[] dataArray = lotteryListData.split(",");

                    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(dataArray));
                    for (String lottery : arrayList) {
                        int key = Integer.parseInt(lottery.trim().split(":")[0]);
                        int amount = Integer.parseInt(lottery.trim().split(":")[1]);
                        lotteryList.put(key,amount);
                    }
                }
                setNextPrizeTime(prizeTime);

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

        core.sendMessageToRedisAsync("XSCasinoRedisData/XSLottery/EndPrizeNumber/"+core.getRedisCrossServerHostName(),"" + prizeNum);

        Bukkit.broadcastMessage("Waiting system calculate data...");
        Bukkit.getScheduler().scheduleSyncDelayedTask(core.getPlugin(), new Runnable() {
            @Override
            public void run() {
                calculateWinner();
            }
        }, 100L);
    }

    public void calculateWinner() {
        Bukkit.broadcastMessage("Send... Data Winner");

        Gson gson = new Gson();
        String json = gson.toJson(dataLottery);

        core.sendMessageToRedisAsync("XSCasinoRedisData/XSLottery/EndSendWinnerList/"+core.getRedisCrossServerHostName(),json);
        clearData();
    }

    public void clearData() {
        dataLottery.clear();
        lotteryList.clear();
        saveTOSQL(XSHandlers.getJDBC_URL(),XSHandlers.getUSER(),XSHandlers.getPASS());
    }

    public void saveTOSQL(String JDBC_URL,String USER,String PASS) {
        try {
            Connection connection = DriverManager.getConnection(JDBC_URL, USER, PASS);
            PreparedStatement preparedStatement = null;
            String updateQuery = "UPDATE " +XSHandlers.getTableLottery() + " SET lotteryList=?, NextPrizeTime=? LIMIT 1";

            preparedStatement = connection.prepareStatement(updateQuery);

            ArrayList<String> lotteryList = new ArrayList<>();
            for(Map.Entry<Integer,Integer> lottery : getLotteryList().entrySet()) {
                lotteryList.add(lottery.getKey()+":"+lottery.getValue());
            }

            preparedStatement.setString(1, String.valueOf(lotteryList));
            preparedStatement.setLong(2, getNextPrizeTime());

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
