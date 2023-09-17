package net.xsapi.panat.xscasinoredis.xscore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.xsapi.panat.xscasinoredis.api.HashMapDeserializer;
import net.xsapi.panat.xscasinoredis.configuration.config;
import net.xsapi.panat.xscasinoredis.configuration.configLoader;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class core extends JavaPlugin {

    public static core plugin;

    public static core getPlugin() {
        return plugin;
    }
    public static ArrayList<Thread> threads = new ArrayList<>();

    @Override
    public void onEnable() {

        plugin = this;
        new configLoader();

        if(redisConnection()) {
            for(String server : config.customConfig.getStringList("cross-server.servers")) {
                subscribeToChannelAsync("XSCasinoRedisData/XSLottery/" + config.customConfig.getString("redis.host-server") + "/" + server);
            }
        }
    }

    @Override
    public void onDisable() {
        for(Thread thread : threads) {
            thread.interrupt();
        }
    }

    private void subscribeToChannelAsync(String channelName) {
        String redisHost = config.customConfig.getString("redis.host");
        int redisPort = config.customConfig.getInt("redis.port");
        String password = config.customConfig.getString("redis.password");

        Thread thread = new Thread(() -> {
            try (Jedis jedis = new Jedis(redisHost, redisPort)) {
                if(!password.isEmpty()) {
                    jedis.auth(password);
                }
                JedisPubSub jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        if (Thread.currentThread().isInterrupted()) {
                            return;
                        }
                        Bukkit.getConsoleSender().sendMessage("XSCasinoRedis Received data from ---> " + channel);
                        for(String server : config.customConfig.getStringList("cross-server.servers")) {
                            if(channel.equalsIgnoreCase("XSCasinoRedisData/XSLottery/" + config.customConfig.getString("redis.host-server") + "/" + server)) {
                                Bukkit.getConsoleSender().sendMessage("XSCasino Received Data...");
                                convertToObject(message);
                            }
                        }
                    }
                };
                jedis.subscribe(jedisPubSub, channelName);
            } catch (Exception e) {
                // จัดการข้อผิดพลาดที่เกิดขึ้น
                e.printStackTrace();
            }
        });
        thread.start();
        threads.add(thread);
    }

    private void convertToObject(String json) {

        Bukkit.broadcastMessage("JSON: " + json);
        if (json.isEmpty()) {
            return;
        }


        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(new TypeToken<HashMap<Integer, Integer>>() {}.getType(), new HashMapDeserializer());
        Gson gson = gsonBuilder.create();

        // แปลง JSON เป็น HashMap<Integer, Integer>
        HashMap<Integer, Integer> hashMap = gson.fromJson(json, new TypeToken<HashMap<Integer, Integer>>() {}.getType());

        for(Map.Entry<Integer,Integer> map : hashMap.entrySet()) {
            Bukkit.getConsoleSender().sendMessage(map.getKey() + " : " + map.getValue());
        }

    }

    private boolean redisConnection() {
        String redisHost = config.customConfig.getString("redis.host");
        int redisPort = config.customConfig.getInt("redis.port");
        String password = config.customConfig.getString("redis.password");

        try {
            Jedis jedis = new Jedis(redisHost, redisPort);
            if(!password.isEmpty()) {
                jedis.auth(password);
            }
            jedis.close();
            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCASINO REDIS] Redis Server : §x§6§0§F§F§0§0Connected");
            return true;
        } catch (Exception e) {
            Bukkit.getConsoleSender().sendMessage("§x§E§7§F§F§0§0[XSCASINO REDIS] Redis Server : §x§C§3§0§C§2§ANot Connected");
            e.printStackTrace();
        }
        return false;
    }


}
