package survivaldial;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.*;
import javax.imageio.ImageIO;
import org.json.*;
import org.lwjgl.util.vector.Vector2f;

/** A one-time edit of vanilla terrain tiles; no custom terrain or script is saved. */
public final class CloudCoverage {
    public static final String KEY="sd_hyperspaceCloudCoverage";
    public static final String APPLIED_KEY="survival_dial.cloudsApplied.v1";

    static SettingsModel.Entry entry() throws JSONException {
        return new SettingsModel.Entry(new JSONObject().put("key",KEY)
            .put("label","Hyperspace cloud coverage (%)").put("category","Fleet and travel")
            .put("type","number").put("status","creation").put("default",100)
            .put("min",0).put("max",125).put("integer",true)
            .put("description","Target cloud-covered area relative to the generated map: 50 means half, 100 leaves it unchanged, and 125 targets a quarter more. Changes cloud coverage, not lightning frequency. Increases are capped by available space; original clear regions, system entrances, the abyss, and connected clear routes are protected. New Survival campaigns only. Terrain changes remain if the mod is removed."));
    }

    public static void applyNewCampaign(JSONObject profile) throws Exception {
        SectorAPI sector=Global.getSector();
        if(!sector.isExtMode() || Boolean.TRUE.equals(sector.getPersistentData().get(APPLIED_KEY)))return;
        int percent=profile.getInt(KEY);
        if(percent==100){sector.getPersistentData().put(APPLIED_KEY,Boolean.TRUE);return;}
        HyperspaceTerrainPlugin terrain=null;
        for(CampaignTerrainAPI candidate:sector.getHyperspace().getTerrainCopy())
            if(candidate.getPlugin() instanceof HyperspaceTerrainPlugin){terrain=(HyperspaceTerrainPlugin)candidate.getPlugin();break;}
        if(terrain==null)throw new IllegalStateException("No hyperspace terrain available for cloud coverage");
        int[][] tiles=terrain.getTiles();
        boolean[][] allowed=percent>100?additionMask(terrain):null;
        int before=count(tiles);
        int[][] changed=adjust(tiles,percent,allowed);
        for(int x=0;x<tiles.length;x++)System.arraycopy(changed[x],0,tiles[x],0,tiles[x].length);
        // Removed cloud cells must not retain storm effects or cached map samples.
        HyperspaceTerrainPlugin.CellStateTracker[][] active=terrain.getActiveCells();
        if(active!=null)for(int x=0;x<tiles.length;x++)for(int y=0;y<tiles[x].length;y++)
            if(tiles[x][y]<0)active[x][y]=null;
        terrain.forceClearSampleCache();
        sector.getPersistentData().put(APPLIED_KEY,Boolean.TRUE);
        int after=count(tiles);
        Global.getLogger(CloudCoverage.class).info("Cloud coverage target "+percent+"%: "+before+" -> "+after+
            " tiles ("+(before==0?100:Math.round(after*10000d/before)/100d)+"%); clear-area protections may limit increases.");
    }

    private static boolean[][] additionMask(HyperspaceTerrainPlugin terrain) throws Exception {
        int[][] tiles=terrain.getTiles();int w=tiles.length,h=tiles[0].length;
        BufferedImage image;
        try(InputStream stream=Global.getSettings().openStream("data/campaign/terrain/hyperspace_map.png")){
            image=ImageIO.read(stream);
        }
        if(image==null || image.getWidth()!=w || image.getHeight()!=h)
            throw new IllegalStateException("Cloud coverage increase requires the matching hyperspace terrain map");
        boolean[][] allowed=new boolean[w][h];
        float halfWidth=Global.getSettings().getFloat("sectorWidth")/2f;
        float halfHeight=Global.getSettings().getFloat("sectorHeight")/2f;
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            // Match Misc.addNebulaFromPNG's bottom-up tile coordinates.
            if((image.getRGB(x,h-y-1)&0xffffff)==0 || tiles[x][y]>=0)continue;
            float[] center=terrain.getTileCenter(x,y);
            allowed[x][y]=Math.abs(center[0])<halfWidth && Math.abs(center[1])<halfHeight
                && !terrain.getAbyssPlugin().isInAbyss(new Vector2f(center[0],center[1]));
        }
        for(StarSystemAPI system:Global.getSector().getStarSystems())
            protectCircle(allowed,terrain,system.getLocation(),system.getMaxRadiusInHyperspace()*0.5f+terrain.getTileSize()*2f);
        for(SectorEntityToken entity:Global.getSector().getHyperspace().getAllEntities()){
            if(entity instanceof CampaignTerrainAPI)continue;
            protectCircle(allowed,terrain,entity.getLocation(),Math.max(0,entity.getRadius())+terrain.getTileSize()*2f);
        }
        return allowed;
    }

    private static void protectCircle(boolean[][] allowed,HyperspaceTerrainPlugin terrain,Vector2f position,float radius){
        float[] origin=terrain.getTileCenter(0,0);float step=terrain.getTileSize();
        int x0=Math.max(0,(int)Math.floor((position.x-radius-origin[0])/step));
        int x1=Math.min(allowed.length-1,(int)Math.ceil((position.x+radius-origin[0])/step));
        int y0=Math.max(0,(int)Math.floor((position.y-radius-origin[1])/step));
        int y1=Math.min(allowed[0].length-1,(int)Math.ceil((position.y+radius-origin[1])/step));
        for(int x=x0;x<=x1;x++)for(int y=y0;y<=y1;y++){
            double dx=origin[0]+x*step-position.x,dy=origin[1]+y*step-position.y;
            if(dx*dx+dy*dy<=radius*radius)allowed[x][y]=false;
        }
    }

    /** Returns a detached grid. Percentages use occupied tile counts, not storm activation. */
    static int[][] adjust(int[][] source,int percent,boolean[][] allowed){
        if(percent<0 || percent>125)throw new IllegalArgumentException("Cloud coverage must be 0-125 percent");
        int w=source.length,h=source[0].length;
        int[][] result=new int[w][];
        long seed=0x534449414cL;
        for(int x=0;x<w;x++){
            if(source[x].length!=h)throw new IllegalArgumentException("Cloud grid must be rectangular");
            result[x]=source[x].clone();
            for(int y=0;y<h;y++)seed=seed*31+(source[x][y]>=0?1:0);
        }
        int before=count(source),target=(int)Math.round(before*percent/100d);
        if(target==before)return result;
        if(percent>100 && (allowed==null || allowed.length!=w))throw new IllegalArgumentException("Missing expansion mask");
        List<Cell> candidates=new ArrayList<Cell>();
        for(int x=0;x<w;x++)for(int y=0;y<h;y++){
            if(percent<100 ? source[x][y]>=0 : source[x][y]<0 && allowed[x][y] && neighbors(source,x,y)>0)
                candidates.add(new Cell(x,y,smoothNoise(x,y,seed)));
        }
        candidates.sort(Comparator.comparingDouble((Cell c)->c.score).thenComparingInt(c->c.x).thenComparingInt(c->c.y));
        int actual=before;
        for(Cell cell:candidates){
            if(actual==target)break;
            if(percent<100){result[cell.x][cell.y]=-1;actual--;}
            else if(keepsClearRoutes(result,cell.x,cell.y)){
                result[cell.x][cell.y]=neighborTexture(source,cell.x,cell.y);actual++;
            }
        }
        return result;
    }

    private static final class Cell {
        final int x,y;final double score;
        Cell(int x,int y,double score){this.x=x;this.y=y;this.score=score;}
    }
    static int count(int[][] tiles){int n=0;for(int[] column:tiles)for(int value:column)if(value>=0)n++;return n;}
    private static int neighbors(int[][] tiles,int x,int y){
        int n=0;for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)
            if((dx!=0 || dy!=0) && cloud(tiles,x+dx,y+dy))n++;
        return n;
    }
    private static boolean cloud(int[][] tiles,int x,int y){
        return x>=0 && y>=0 && x<tiles.length && y<tiles[0].length && tiles[x][y]>=0;
    }
    private static int neighborTexture(int[][] tiles,int x,int y){
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(cloud(tiles,x+dx,y+dy))return tiles[x+dx][y+dy];
        throw new IllegalStateException("Expansion has no neighboring cloud");
    }
    // Adding a tile must not disconnect any clear cardinal neighbors. Check a local
    // bypass around it before filling; induction preserves the original clear routes.
    private static boolean keepsClearRoutes(int[][] tiles,int x,int y){
        if(x==0 || y==0 || x==tiles.length-1 || y==tiles[0].length-1)return false;
        boolean[] empty=new boolean[9],seen=new boolean[9];
        for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)
            empty[(dx+1)*3+dy+1]=(dx!=0 || dy!=0) && tiles[x+dx][y+dy]<0;
        int[] cardinal={1,3,5,7},queue=new int[9];int start=-1;
        for(int i:cardinal)if(empty[i]){start=i;break;}
        if(start<0)return true; // an isolated hole, not a through-route
        int head=0,tail=0;queue[tail++]=start;seen[start]=true;
        while(head<tail){
            int i=queue[head++],cx=i/3,cy=i%3;
            for(int j:cardinal){int nx=cx+j/3-1,ny=cy+j%3-1;
                if(nx<0 || ny<0 || nx>2 || ny>2)continue;
                int k=nx*3+ny;if(empty[k] && !seen[k]){seen[k]=true;queue[tail++]=k;}
            }
        }
        for(int i:cardinal)if(empty[i] && !seen[i])return false;
        return true;
    }
    private static double smoothNoise(int x,int y,long seed){
        return noise(x/8d,y/8d,seed)*0.8+noise(x/3d,y/3d,seed+1)*0.2;
    }
    private static double noise(double x,double y,long seed){
        int ix=(int)Math.floor(x),iy=(int)Math.floor(y);double fx=x-ix,fy=y-iy;
        fx=fx*fx*(3-2*fx);fy=fy*fy*(3-2*fy);
        double a=hash(ix,iy,seed)*(1-fx)+hash(ix+1,iy,seed)*fx;
        double b=hash(ix,iy+1,seed)*(1-fx)+hash(ix+1,iy+1,seed)*fx;
        return a*(1-fy)+b*fy;
    }
    private static double hash(int x,int y,long seed){
        long n=seed+x*0x9e3779b97f4a7c15L+y*0xc2b2ae3d27d4eb4fL;
        n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;n^=n>>>31;
        return (n>>>11)*0x1.0p-53;
    }
    private CloudCoverage(){}
}