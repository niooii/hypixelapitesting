package org.example;


import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.IOUtils;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class apitesting {

    static ArrayList<String> doneFlips = new ArrayList<>();
    static NumberFormat myFormat = NumberFormat.getInstance();

    public static void main(String[] args) throws IOException, InterruptedException {
        //getAvgBin();
        HashMap<String, JsonObject> avgPrices = getAvgBin();
        while(true) {
            Thread.sleep(500);
            avgPrices = getAvgBin();
            ArrayList<JsonObject> underBinList = getUnderBin();
            for(JsonObject x : underBinList){
                boolean isCake = x.get("name").getAsString().contains("New Year Cake");
                boolean isYear1Cake = x.get("name").getAsString().contains("Cake (Year 1)");
                if(!doneFlips.contains(x.get("uuid").getAsString())) {
                    System.out.println("ITEM_NAME: " + x.get("name").getAsString());
                    doneFlips.add(x.get("uuid").getAsString());
                    boolean isManipulated = calcManipulated(x, avgPrices);
                    if(!isCake && !isManipulated){
                        createAndSendEmbedWithInfo(x, avgPrices, false);
                    } else {
                        if(isYear1Cake){
                            createAndSendEmbedWithInfo(x, avgPrices, isManipulated);
                            System.out.println("is yr1 cake.");
                        }
                        else if(isManipulated){
                            createAndSendEmbedWithInfo(x, avgPrices, true);
                            System.out.println("likely manipulated.");
                        }
                        else if (isCake){
                            System.out.println("is cake.");
                        } else {
                            System.out.println("wtf happened.");
                        }
                    }
                }
            }
        }
    }

    public static double calcPercentChange(JsonObject x, HashMap<String, JsonObject> avgMap){
        try {
            String avgPriceStr = avgMap.get(x.get("id").getAsString()).get("price").getAsString();
            double avgPrice = Double.parseDouble(avgPriceStr);
            System.out.println("avg price calc result: " + avgPriceStr);
            double percentChange = 100 * (Double.parseDouble(x.get("past_bin_price").getAsString()) - avgPrice) / avgPrice;
            System.out.println("percent change parameters: ");
            System.out.println("past bin price: " + Double.parseDouble(x.get("past_bin_price").getAsString()));
            System.out.println("avg price: " + avgPrice);
            System.out.println("percent change: " + percentChange);
            return percentChange;
        } catch(NullPointerException e){
            System.out.println(e);
            return 0;
        }
    }

    public static boolean calcManipulated(JsonObject x, HashMap<String, JsonObject> avgMap){
        try{
            double percentChange = calcPercentChange(x, avgMap);
//            System.out.println("percent change for item " + ": "+ percentChange);
            if(percentChange <= 25){
                return false;
            } else{
                return true;
            }
        } catch(Exception e){
            return false;
        }
    }

    public static void createAndSendEmbedWithInfo(JsonObject x, HashMap<String, JsonObject> avgPrices, boolean isManipulated){
        System.out.println();
        String avgPriceStr;
        String avgSalesStr;
        try{
            avgPriceStr = avgPrices.get(x.get("id").getAsString()).get("price").getAsString();
            avgPriceStr = myFormat.format(Integer.parseInt(avgPriceStr.substring(0, avgPriceStr.indexOf("."))));
            avgSalesStr = avgPrices.get(x.get("id").getAsString()).get("sales").getAsString().substring(0, avgPrices.get(x.get("id").getAsString()).get("sales").getAsString().indexOf("."));
        } catch(Exception err){
            avgPriceStr = "N/A";
            avgSalesStr = "N/A";
        }
        double percentChange = calcPercentChange(x, avgPrices);
        sendWebhook(x.get("name").getAsString(),
                avgPriceStr,
                myFormat.format(Integer.parseInt(String.valueOf(x.get("past_bin_price")).substring(0, String.valueOf(x.get("past_bin_price")).indexOf(".")))),
                myFormat.format(Integer.parseInt(String.valueOf(x.get("starting_bid")))),
                x.get("viewah").getAsString(),
                avgSalesStr,
                isManipulated,
                percentChange);
    }

    public static void sendWebhook(String name, String avg, String nextLowestBin, String price, String viewCommand, String dailySales, boolean isManipulated, double percentChange){
        try {
            System.out.println(name);
            System.out.println(avg);
            System.out.println(nextLowestBin);
            System.out.println(price);
            String url;
            if(isManipulated){
                url = "https://discord.com/api/webhooks/1092249338182373488/wNQDhWszILGC0ls6zYSp3RiHHocjgqyDBDtPEKUeE8IQgqLa0V7SSWt3-6nHGZMEVlVU";
            } else {
                url = "https://discord.com/api/webhooks/1092215203480993833/X0e0SIVUjNC0P4WIDUAplYa6kG53DJiu8MrXFc9kgpwapYYNGW7gLm0CLRXQ_4jpvsrQ";
            }
            DiscordWebhook webhook = new DiscordWebhook(url);
            webhook.setContent("");
            webhook.setAvatarUrl("https://cdn.discordapp.com/attachments/975541046329114654/1092215452756881508/image.png");
            webhook.setUsername(":o");
            webhook.setTts(false);
            Color color;
            String desc = "";
            if(isManipulated){
                color = Color.red;
                name += " (likely manipulated)";
                desc = "`percent change: " + percentChange + "%`";
            } else {
                color = Color.cyan;
            }
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setTitle(name)
                    .setDescription(desc)
                    .setColor(color)
                    .addField("Next lowest bin", nextLowestBin, false)
                    .addField("Average", avg, false)
                    .addField("Price", price, true)
                    .addField("Daily sales", dailySales, true)
                    .addField("View auction command", "`" + viewCommand + "`", false)
                    .setThumbnail("https://cdn.discordapp.com/attachments/975541046329114654/1092215452756881508/image.png")
                    .setFooter("", "")
                    .setImage("")
                    .setAuthor("", "", "")
                    .setUrl(""));
            /*
            webhook.addEmbed(new DiscordWebhook.EmbedObject()
                    .setDescription("Just another added embed object!"));
             */
            webhook.execute(); //Handle exception
        } catch(Exception e){
            System.out.println(e);
        }
    }

    public static HashMap<String, JsonObject> getAvgBin() throws IOException {
        long epoch = System.currentTimeMillis() - 259200000;
        HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:8002/average?time=" + epoch + "&step=4320").openConnection();
        c.setRequestProperty("Content-type", "application/json");
        c.setDoOutput(true);
        String res = IOUtils.toString(c.getInputStream());
        return parseAvgBin(res);
    }

    public static HashMap<String, JsonObject> parseAvgBin(String str){
        if(str.length() == 2){
            System.out.println("length 0");
            return new HashMap<>();
        }
        HashMap<String, JsonObject> map = new HashMap<>();
        JsonParser parser = new JsonParser();
        ArrayList<JsonObject> jsonlist = new ArrayList<>();
        //cut off ending {}
        str = str.substring(1);
        str = str.substring(0, str.length() - 2);
        //split into array of stuff
        String[] strArray = str.split("},");
        for(int i = 0; i < strArray.length; i++){
            strArray[i] += "}";
            String tempjson = strArray[i].substring(strArray[i].indexOf("price") - 2);
//            System.out.println(strArray[i].substring(1, strArray[i].indexOf("\"", 1)));
//            System.out.println(tempjson);
            map.put(strArray[i].substring(1, strArray[i].indexOf("\"", 1)), parser.parse(tempjson).getAsJsonObject());
        }
//        map.forEach((k, v)
//                -> System.out.println(k + " : "
//                + (v.toString())));
        return map;
    }

    public static ArrayList<JsonObject> getUnderBin() throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL("http://localhost:8002/underbin").openConnection();
        c.setRequestProperty("Content-type", "application/json");
        c.setDoOutput(true);
        String res = IOUtils.toString(c.getInputStream());
        return parseUnderBin(res);
    }

    public static ArrayList<JsonObject> parseUnderBin(String str){
        if(str.length() == 2){
            return new ArrayList<>();
        }
        JsonParser parser = new JsonParser();
        //cut off ending {}
        str = str.substring(1);
        str = str.substring(0, str.length() - 2);
        //split into array of stuff
        String[] strArray = str.split("},");
        for(int i = 0; i < strArray.length; i++){
            strArray[i] = strArray[i].substring(strArray[i].indexOf(":") + 1);
            strArray[i] = strArray[i] + ",\"viewah\":\"/viewauction " + strArray[i].substring(strArray[i].indexOf("uuid") + 7, strArray[i].lastIndexOf("\"")) + "\"}";
        }
        ArrayList<JsonObject> jsonlist = new ArrayList<>();
        for(String x : strArray){
            jsonlist.add(parser.parse(x).getAsJsonObject());
        }
        return jsonlist;
    }
}
