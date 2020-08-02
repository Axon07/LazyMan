
package lazyman;

import GameObj.Game;
import Objects.Web;
import Util.MessageBox;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;


public class GetMLBInfo {
    public static Game[] getGames(String date) {
        try {
            JsonObject t = new JsonParser().parse(Web.getContent("https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=" + date + "&hydrate=team,linescore,game(content(summary,media(epg)))&language=en"))
                    .getAsJsonObject();

            if (t.get("totalItems").getAsInt() < 1) {
                return null;
            }

            int i = 0;

            Game[] g = new Game[t.getAsJsonArray("dates").get(0).getAsJsonObject().get("totalItems").getAsInt()];
            Game g1;

            for (JsonElement jo : t.getAsJsonArray("dates").get(0).getAsJsonObject().getAsJsonArray("games")) {
                g1 = new Game();
               
                g1.setGameID(jo.getAsJsonObject().get("gamePk").getAsInt());
                g1.setTime(jo.getAsJsonObject().get("gameDate").getAsString().substring(11).replace("Z", ""));
                if (g1.getTime().equals("04:00:00")) {
                    g1.setTime("TBD");
                }
                g1.setDate(jo.getAsJsonObject().get("gameDate").getAsString().substring(0, 10));

                g1.setAwayTeamFull(jo.getAsJsonObject().getAsJsonObject("teams").getAsJsonObject("away").getAsJsonObject("team").get("name").getAsString());
                g1.setAwayTeam(jo.getAsJsonObject().getAsJsonObject("teams").getAsJsonObject("away").getAsJsonObject("team").get("abbreviation").getAsString());
                
                g1.setHomeTeamFull(jo.getAsJsonObject().getAsJsonObject("teams").getAsJsonObject("home").getAsJsonObject("team").get("name").getAsString());
                g1.setHomeTeam(jo.getAsJsonObject().getAsJsonObject("teams").getAsJsonObject("home").getAsJsonObject("team").get("abbreviation").getAsString());

                g1.setGameState(jo.getAsJsonObject().getAsJsonObject("status").get("detailedState").getAsString());
                
                if (g1.getGameState().contains("In Progress")) {
                    String period = jo.getAsJsonObject().getAsJsonObject("linescore").get("currentInningOrdinal").getAsString();
                    String time = jo.getAsJsonObject().getAsJsonObject("linescore").get("inningHalf").getAsString();
                    g1.setTimeRemaining(period + " " + time);
                } else if (g1.getGameState().equals("Final") || g1.getGameState().equals("Game Over")) {
                    g1.setTimeRemaining("Final");
                } else if (g1.getGameState().equals("Postponed")) {
                    g1.setTimeRemaining("PPD");
                } else if (g1.getGameState().equals("Delayed")) {
                    g1.setTimeRemaining("Delayed");
                } else if (jo.getAsJsonObject().get("gameNumber").getAsInt() > 1) {
                    g1.setTimeRemaining("TBD");
                }
                else {
                    g1.setTimeRemaining("n/a");
                }
                
                try {
                    g1.setActualStart(jo.getAsJsonObject().get("gameDate").getAsString().replace("T", " ").replace("Z", ""));
                } catch (Exception e) {
                    g1.setActualStart("");
                }
                
                boolean cl = false;
                if (jo.getAsJsonObject().getAsJsonObject("content").getAsJsonObject("media") != null) {
                    MAIN_LOOP:
                    for (JsonElement stream : jo.getAsJsonObject().getAsJsonObject("content").getAsJsonObject("media").getAsJsonArray("epg")) {
                        if (stream.getAsJsonObject().get("title").getAsString().equals("MLBTV")) {
                            for (JsonElement innerStr : stream.getAsJsonObject().getAsJsonArray("items")) {
                                if (innerStr.getAsJsonObject().get("mediaFeedType").getAsString().contains("IN_"))
                                    continue;
                                if (innerStr.getAsJsonObject().get("mediaState").getAsString().equals("MEDIA_OFF")) {
                                    cl = true;
                                    break MAIN_LOOP;
                                }
                               g1.addFeed(innerStr.getAsJsonObject().get("mediaFeedType").getAsString(), innerStr.getAsJsonObject().get("id").getAsString(), innerStr.getAsJsonObject().get("callLetters").getAsString());
                            }
                        }
                    }
                } else {
                    cl = true;
                }

                if (cl) {
                    g1.addFeed("Info available later", null, "");
                }
                g[i] = g1;
                i++;
            }
            Arrays.sort(g, (Game o1, Game o2) -> {
                String index = "nearcoi";
                if (o1.getGameState().equals(o2.getGameState()))
                    return 0;
                char s1 = o1.getGameState().charAt(1), s2 = o2.getGameState().charAt(1);
                
                return index.indexOf(s1) - index.indexOf(s2);
            });
            return g;
        } catch (JsonSyntaxException ex) {
            ex.printStackTrace();
            Game[] e = new Game[1];
            e[0].setAwayTeam("err");
            return e;
        } catch (UnknownHostException ex) {
            MessageBox.show("Schedule site is down for MLB.", "Error", 2);
            return null;
        }
    }
}
