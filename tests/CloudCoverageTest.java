package survivaldial;

import com.fs.starfarer.api.*;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.terrain.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** Geometry, persistence, and lifecycle checks against the actual terrain API. */
public final class CloudCoverageTest {
    private static int checks;
    private static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    @SuppressWarnings("unchecked") private static <T>T proxy(Class<T> type,InvocationHandler h){
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},h);
    }
    private static int[][] grid(int size){
        int[][] tiles=new int[size][size];
        for(int x=0;x<size;x++)for(int y=0;y<size;y++)tiles[x][y]=x>0 && y>0 && x<size-1 && y<size-1 && (x+y)%2==0?7:-1;
        return tiles;
    }
    private static boolean[][] allowed(int size){boolean[][] a=new boolean[size][size];for(boolean[] col:a)Arrays.fill(col,true);return a;}
    public static int run() throws Exception {
        checks=0;
        int[][] original=grid(30);int before=CloudCoverage.count(original);
        int[][] hard=CloudCoverage.adjust(original,100,null);
        check(Arrays.deepEquals(original,hard),"100% leaves every original tile unchanged");
        hard[1][1]=-1;check(original[1][1]==7,"adjustment returns a detached grid");
        int[][] easy=CloudCoverage.adjust(original,50,null),medium=CloudCoverage.adjust(original,75,null);
        check(CloudCoverage.count(easy)==Math.round(before*.5),"Easy keeps half the occupied tiles");
        check(CloudCoverage.count(medium)==Math.round(before*.75),"Medium keeps three quarters of the occupied tiles");
        check(CloudCoverage.count(CloudCoverage.adjust(original,0,null))==0,"custom zero removes clouds");
        check(Arrays.deepEquals(easy,CloudCoverage.adjust(original,50,null)),"same generated map gives repeatable cloud thinning");
        boolean nested=true,onlyRemoves=true;
        for(int x=0;x<30;x++)for(int y=0;y<30;y++){
            if(easy[x][y]>=0 && medium[x][y]<0)nested=false;
            if(medium[x][y]>=0 && medium[x][y]!=original[x][y])onlyRemoves=false;
        }
        check(nested && onlyRemoves,"easier coverage is nested and never invents new clouds");
        boolean[][] mask=allowed(30);mask[10][11]=false;
        int[][] insane=CloudCoverage.adjust(original,125,mask);
        check(CloudCoverage.count(insane)==Math.round(before*1.25),"Insane reaches 125% when enough eligible space exists");
        check(insane[10][11]<0,"protected empty tile never gets a cloud");
        boolean retains=true;for(int x=0;x<30;x++)for(int y=0;y<30;y++)if(original[x][y]>=0 && insane[x][y]!=original[x][y])retains=false;
        check(retains,"expansion preserves all original clouds and their texture indices");
        boolean[][] noSpace=new boolean[30][30];
        check(Arrays.deepEquals(original,CloudCoverage.adjust(original,125,noSpace)),"limited space caps expansion without altering protected cells");
        int[][] corridor=new int[20][20];for(int[] column:corridor)Arrays.fill(column,4);
        for(int x=0;x<20;x++)corridor[x][10]=-1;
        int[][] route=CloudCoverage.adjust(corridor,125,allowed(20));
        boolean clear=true;for(int x=0;x<20;x++)if(route[x][10]>=0)clear=false;
        check(clear,"expansion cannot close an existing narrow clear route");
        int[][] empty=new int[10][10];for(int[] col:empty)Arrays.fill(col,-1);
        check(CloudCoverage.count(CloudCoverage.adjust(empty,125,allowed(10)))==0,"empty maps stay empty");
        int[][] decoded=BaseTiledTerrain.decodeTiles(BaseTiledTerrain.encodeTiles(insane),30,30);
        boolean sameOccupancy=true;for(int x=0;x<30;x++)for(int y=0;y<30;y++)if((decoded[x][y]>=0)!=(insane[x][y]>=0))sameOccupancy=false;
        check(sameOccupancy,"changed terrain survives the game's native tile serialization");
        boolean invalid=false;try{CloudCoverage.adjust(original,126,mask);}catch(IllegalArgumentException expected){invalid=true;}
        check(invalid,"out-of-range cloud coverage is rejected");

        int[][] rectangle=new int[13][7];for(int x=0;x<13;x++)for(int y=0;y<7;y++)rectangle[x][y]=(x+y)%2==0?3:-1;
        int[][] halfRectangle=CloudCoverage.adjust(rectangle,50,null);
        check(halfRectangle.length==13 && halfRectangle[0].length==7
            && CloudCoverage.count(halfRectangle)==Math.round(CloudCoverage.count(rectangle)*.5),"non-square terrain retains dimensions and correct coverage");
        SettingsAPI previousSettings=Global.getSettings();SectorAPI previousSector=Global.getSector();
        try{
            Map<String,Object> persistent=new HashMap<String,Object>();boolean[] extended={true};int[] terrainReads={0};
            FakeTerrain[] terrain={new FakeTerrain(grid(30))};
            BufferedImage image=new BufferedImage(30,30,BufferedImage.TYPE_INT_RGB);
            for(int x=0;x<30;x++)for(int y=0;y<30;y++)image.setRGB(x,y,0xffffff);
            image.setRGB(9,30-12-1,0); // an original clear area, with asymmetric Y to test orientation
            ByteArrayOutputStream encoded=new ByteArrayOutputStream();ImageIO.write(image,"png",encoded);
            StarSystemAPI system=proxy(StarSystemAPI.class,(o,m,a)->{
                if(m.getName().equals("getLocation"))return new Vector2f(600,600);
                if(m.getName().equals("getMaxRadiusInHyperspace"))return 200f;
                return null;
            });
            SectorEntityToken landmark=proxy(SectorEntityToken.class,(o,m,a)->{
                if(m.getName().equals("getLocation"))return new Vector2f(1400,1400);
                if(m.getName().equals("getRadius"))return 100f;
                return null;
            });
            CampaignTerrainAPI token=proxy(CampaignTerrainAPI.class,(o,m,a)->m.getName().equals("getPlugin")?terrain[0]:null);
            LocationAPI hyper=proxy(LocationAPI.class,(o,m,a)->{
                if(m.getName().equals("getTerrainCopy")){terrainReads[0]++;return Collections.singletonList(token);}
                if(m.getName().equals("getAllEntities"))return Collections.singletonList(landmark);
                return null;
            });
            Global.setSettings(proxy(SettingsAPI.class,(o,m,a)->{
                if(m.getName().equals("openStream"))return new ByteArrayInputStream(encoded.toByteArray());
                if(m.getName().equals("getFloat") && (a[0].equals("sectorWidth") || a[0].equals("sectorHeight")))return 10000f;
                return m.invoke(previousSettings,a);
            }));
            Global.setSector(proxy(SectorAPI.class,(o,m,a)->{
                if(m.getName().equals("isExtMode"))return extended[0];
                if(m.getName().equals("getPersistentData"))return persistent;
                if(m.getName().equals("getHyperspace"))return hyper;
                if(m.getName().equals("getStarSystems"))return Collections.singletonList(system);
                return null;
            }));
            JSONObject profile=SettingsModel.baseline();profile.put(CloudCoverage.KEY,125);
            persistent.put(SettingsModel.SAVE_KEY,profile.toString());
            SurvivalDialPlugin plugin=new SurvivalDialPlugin();
            extended[0]=false;plugin.onNewGameAfterProcGen();
            check(terrainReads[0]==0 && !persistent.containsKey(CloudCoverage.APPLIED_KEY),"Normal generation never touches cloud terrain");
            extended[0]=true;profile.put(CloudCoverage.KEY,100);persistent.put(SettingsModel.SAVE_KEY,profile.toString());plugin.onNewGameAfterProcGen();
            check(terrainReads[0]==0 && Boolean.TRUE.equals(persistent.get(CloudCoverage.APPLIED_KEY)),"Hard is a true terrain no-op");
            persistent.remove(CloudCoverage.APPLIED_KEY);profile.put(CloudCoverage.KEY,125);persistent.put(SettingsModel.SAVE_KEY,profile.toString());
            plugin.onNewGameAfterProcGen();
            int[][] changed=terrain[0].getTiles();
            check(CloudCoverage.count(changed)>before,"new-campaign hook applies increased cloud coverage");
            check(changed[9][12]<0,"original PNG clear area is protected with correct Y orientation");
            boolean protectedAreas=true;
            for(int x=0;x<30;x++)for(int y=0;y<30;y++){
                if(original[x][y]>=0)continue;
                int ds=(x-6)*(x-6)+(y-6)*(y-6),de=(x-14)*(x-14)+(y-14)*(y-14);
                if((x>=24 || ds<=9 || de<=9) && changed[x][y]>=0)protectedAreas=false;
            }
            check(protectedAreas,"abyss, system entrances, and landmark clearances remain empty");
            check(terrain[0].cacheClears==1,"terrain map cache refreshed after generation");
            int actual=CloudCoverage.count(changed);int reads=terrainReads[0];plugin.onNewGameAfterProcGen();
            check(CloudCoverage.count(changed)==actual && terrainReads[0]==reads,"repeated generation hook cannot compound coverage");
            persistent.remove(CloudCoverage.APPLIED_KEY);plugin.onGameLoad(false);
            check(CloudCoverage.count(changed)==actual && terrainReads[0]==reads,"loading an existing save never changes its clouds");
            profile.remove(CloudCoverage.KEY);persistent.put(SettingsModel.SAVE_KEY,profile.toString());
            check(SettingsModel.current().getInt(CloudCoverage.KEY)==100,"older profiles migrate to unchanged cloud coverage");
            check(!previousSettings.getSettingsJSON().has(CloudCoverage.KEY),"mod cloud setting never leaks into game global settings");
            terrain[0]=new FakeTerrain(grid(30));profile.put(CloudCoverage.KEY,50);persistent.put(SettingsModel.SAVE_KEY,profile.toString());
            plugin.onNewGameAfterProcGen();
            check(CloudCoverage.count(terrain[0].getTiles())==Math.round(before*.5),"new-campaign hook applies Easy coverage");
            boolean stale=false;for(int x=0;x<30;x++)for(int y=0;y<30;y++)if(terrain[0].getTiles()[x][y]<0 && terrain[0].getActiveCells()[x][y]!=null)stale=true;
            check(!stale,"removed clouds retain no active storm cells");
        }finally{Global.setSettings(previousSettings);Global.setSector(previousSector);}
        return checks;
    }
    private static final class FakeTerrain extends HyperspaceTerrainPlugin {
        int cacheClears;
        FakeTerrain(int[][] source){
            tiles=source;activeCells=new CellStateTracker[source.length][source[0].length];
            for(int x=0;x<source.length;x++)for(int y=0;y<source[0].length;y++)
                if(source[x][y]>=0)activeCells[x][y]=new CellStateTracker(x,y,1f,1f);
            setAbyssPlugin(proxy(HyperspaceAbyssPlugin.class,(o,m,a)->{
                if(m.getName().equals("isInAbyss"))return ((Vector2f)a[0]).x>=2400;
                return null;
            }));
        }
        @Override public float getTileSize(){return 100f;}
        @Override public float[] getTileCenter(int x,int y){return new float[]{x*100f,y*100f};}
        @Override public void forceClearSampleCache(){cacheClears++;super.forceClearSampleCache();}
    }
}