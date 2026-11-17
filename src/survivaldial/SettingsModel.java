package survivaldial;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import java.util.*;
import org.json.*;

/** Pure profile operations plus a small, reversible adapter to the game's settings. */
public final class SettingsModel {
    public static final String SAVE_KEY = "survival_dial.profile.v1";
    public static final String[] PRESETS = {"Easy", "Medium", "Hard", "Insane"};
    public static final List<Entry> entries = new ArrayList<Entry>();
    private static JSONObject baseline;
    private static boolean overridesActive;
    public static JSONObject pending;

    public static final class Entry {
        public final String key, label, category, type, status, description;
        public final Object vanilla;
        public final double min, max;
        public final boolean integer;
        public final Double reference;
        public Entry(JSONObject row) throws JSONException {
            key=row.getString("key"); label=row.getString("label"); category=row.getString("category");
            if(!key.startsWith("ext_")) throw new IllegalArgumentException("Only Extended-mode settings are allowed: "+key);
            type=row.getString("type"); status=row.getString("status"); description=row.getString("description");
            vanilla=row.get("default"); min=row.getDouble("min"); max=row.getDouble("max");
            integer=row.getBoolean("integer"); reference=row.has("presetReference") ? row.getDouble("presetReference") : null;
        }
        public boolean supported() { return status.equals("live") || status.equals("future") || status.equals("creation"); }
        public boolean editable(boolean creation) { return supported() && (creation || !status.equals("creation")); }
        public String tooltip() {
            String timing=status.equals("live") ? "Applies to subsequent calculations after Apply. Reopen affected screens to refresh them."
                : status.equals("future") ? "Affects future stock, offers, or rolls; existing results are unchanged."
                : status.equals("creation") ? "Available during new-game setup only. Existing campaign state is not rebuilt."
                : status.equals("unverified") ? "Effect or timing is not verified for this build. Control disabled."
                : "Not offered as a difficulty control.";
            return description + "\n\nHard / vanilla Extended: " + vanilla + ".\n" + timing;
        }
    }

    public static JSONObject copy(JSONObject source) throws JSONException { return new JSONObject(source.toString()); }

    public static void initialize() throws Exception {
        JSONArray rows = new JSONArray(Global.getSettings().loadText("data/config/survival_dial_catalog.json", "survival_dial"));
        entries.clear();
        for(int i=0;i<rows.length();i++) entries.add(new Entry(rows.getJSONObject(i)));
        baseline = new JSONObject();
        for(Entry e:entries) baseline.put(e.key, Global.getSettings().getSettingsJSON().get(e.key));
        baseline=copy(baseline);
        overridesActive=false;
    }

    public static JSONObject baseline() throws JSONException { return copy(baseline); }

    public static JSONObject current() throws Exception {
        Object saved=Global.getSector().getPersistentData().get(SAVE_KEY);
        JSONObject result=baseline();
        if(saved instanceof String) {
            JSONObject stored=new JSONObject((String)saved);
            for(Entry e:entries) if(e.supported() && stored.has(e.key)) result.put(e.key,stored.get(e.key));
        }
        validate(result);
        return result;
    }

    public static JSONObject preset(JSONObject current, String name, boolean creation) throws Exception {
        if(!Arrays.asList(PRESETS).contains(name)) throw new IllegalArgumentException("Unknown preset");
        JSONObject result=copy(current);
        double blend=name.equals("Easy") ? .25 : name.equals("Medium") ? .60 : 1;
        for(Entry e:entries) {
            if(!e.editable(creation) || (SettingsPresentation.neutral(e) && !name.equals("Hard"))) continue;
            Object value=e.vanilla;
            if(e.reference!=null) {
                if(value instanceof JSONArray) {
                    JSONArray arr=new JSONArray();
                    for(int i=0;i<((JSONArray)value).length();i++) arr.put(presetNumber(e,((JSONArray)value).getDouble(i),blend,name));
                    value=arr;
                } else value=presetNumber(e,((Number)value).doubleValue(),blend,name);
            }
            // Retirement dates are rolled at campaign creation; live presets leave them alone.
            if(e.key.equals("ext_retirementMinCycles") || e.key.equals("ext_retirementMaxCycles")) {
                int offset=name.equals("Easy") ? 20 : name.equals("Medium") ? 10 : name.equals("Insane") ? -5 : 0;
                value=((Number)e.vanilla).intValue()+offset;
            }
            result.put(e.key,value);
        }
        validate(result);
        return copy(result);
    }

    private static double presetNumber(Entry e,double hard,double blend,String name) {
        // Neutral components (e.g. frigate fuel use) stay at their Survival defaults.
        if(Math.abs(hard-e.reference)<0.00001)return hard;
        double value=e.reference+(hard-e.reference)*blend;
        if(name.equals("Insane")) value=hard < e.reference ? hard/1.5 : hard*1.5;
        value=Math.max(e.min,Math.min(e.max,value));
        return e.integer ? Math.rint(value) : Math.round(value*10000d)/10000d;
    }

    public static void validate(JSONObject profile) throws Exception {
        for(Entry e:entries) {
            if(!e.supported()) continue;
            Object value=profile.get(e.key);
            if(e.type.equals("boolean")) { if(!(value instanceof Boolean)) throw new IllegalArgumentException(e.label+" must be on or off"); }
            else if(e.type.equals("array")) {
                if(!(value instanceof JSONArray) || ((JSONArray)value).length()!=4) throw new IllegalArgumentException(e.label+" needs four hull sizes");
                for(int i=0;i<4;i++) validateNumber(e,((JSONArray)value).get(i));
            } else validateNumber(e,value);
        }
        ordered(profile,"ext_retirementMinCycles","ext_retirementMaxCycles");
        ordered(profile,"ext_missionRewardMinDist","ext_missionRewardMaxDist");
        ordered(profile,"ext_retirementDelayCostBase","ext_retirementDelayCostMax");
    }

    private static void validateNumber(Entry e,Object value) {
        if(!(value instanceof Number)) throw new IllegalArgumentException(e.label+" must be numeric");
        double n=((Number)value).doubleValue();
        if(!Double.isFinite(n) || n<e.min || n>e.max || (e.integer && n!=Math.rint(n)))
            throw new IllegalArgumentException(e.label+" must be "+(e.integer?"a whole number ":"")+"between "+e.min+" and "+e.max);
    }
    private static void ordered(JSONObject p,String low,String high) throws JSONException {
        if(p.getDouble(low)>p.getDouble(high)) throw new IllegalArgumentException(low+" must not exceed "+high);
    }

    /** No disk settings edits, reflection, or permanent global overrides. */
    public static void apply(JSONObject profile) throws Exception {
        if(Global.getSector()==null || !Global.getSector().isExtMode())
            throw new IllegalStateException("Survival Dial only applies to Survival / Extended campaigns");
        write(profile);
        overridesActive=true;
    }
    public static void applyForNewExtendedGame(JSONObject profile) throws Exception {
        write(profile);
        overridesActive=true;
    }
    private static void write(JSONObject profile) throws Exception {
        validate(profile);
        SettingsAPI settings=Global.getSettings();
        JSONObject target=settings.getSettingsJSON();
        JSONObject before=new JSONObject();
        JSONObject detached=copy(profile);
        for(Entry e:entries) if(e.supported()) before.put(e.key,target.get(e.key));
        try {
            for(Entry e:entries) if(e.supported()) target.put(e.key,detached.get(e.key));
            settings.resetCached();
        } catch(Exception failure) {
            for(Entry e:entries) if(e.supported()) target.put(e.key,before.get(e.key));
            settings.resetCached();
            throw failure;
        }
    }
    public static void restore() throws Exception {
        // Nothing to undo in a Normal-only session. Never write ordinary game settings.
        if(!overridesActive)return;
        write(baseline());
        overridesActive=false;
    }
    private SettingsModel() {}
}
