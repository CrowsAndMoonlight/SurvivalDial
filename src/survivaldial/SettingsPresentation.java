package survivaldial;

import java.math.BigDecimal;
import java.util.*;
import org.json.*;

/** Player-facing names and a deliberately small starting selection. */
public final class SettingsPresentation {
    public static final String[] COMMON={"shipBuyPriceMult","weaponPriceMult","fighterPriceMult",
        "crewSalaryMult","fuelPriceMult","fuelCapacityMult","fuelCapacityMultTanker","fuelUseMult",
        "commissionMonthlyIncomeMult","shipRecoverableFraction","storyPointsPerLevel","retirementMinCycles"};
    private static final Set<String> NEUTRAL=new HashSet<String>(Arrays.asList(
        "jumpPayMult","pristineShipSellPriceMult","fuelUseMultCiv","commissionBountyMult",
        "suppliesPriceMult","officerSalaryMult","surveySuppliesMult","militaryMarketTariff",
        "blackMarketTariffSneak","probOfForcingBadDerelictCondition","establishColonyCrew",
        "missionRewardMinDist","missionRewardMaxDist","missionRewardMaxBonusFraction",
        "missionRewardBonusCap","bonusXP_convinceOfficer"));
    private static final Map<String,String> NAMES=new HashMap<String,String>();
    static {
        String[][] names={
            {"shipBuyPriceMult","Ship prices"},{"weaponPriceMult","Weapon prices"},{"fighterPriceMult","Fighter LPC prices"},
            {"crewSalaryMult","Crew wages"},{"marineSalaryMult","Marine wages"},{"officerSalaryMult","Officer wages"},
            {"fuelPriceMult","Fuel price"},{"fuelCapacityMult","Ship fuel tanks"},{"fuelCapacityMultTanker","Tanker fuel tanks"},
            {"fuelUseMult","Fuel consumption"},{"fuelUseMultCiv","Civilian fuel consumption"},
            {"commissionMonthlyIncomeMult","Monthly commission income"},{"commissionBountyMult","Commission bounties"},
            {"shipRecoverableFraction","Ships recoverable after battle"},{"derelictShipRecoverableFraction","Recoverable wrecks"},
            {"storyPointsPerLevel","Story points per level"},{"storyPointsPerLevelAtMaxLevel","Story points after the level cap"},
            {"retirementMinCycles","Retirement timing"},{"retirementMaxCycles","Latest retirement date"},
            {"retirementDelayCostBase","Initial rejuvenation bill"},{"retirementDelayCostPerMonth","Monthly increase in rejuvenation bills"},
            {"retirementDelayCostMax","Maximum rejuvenation bill"},{"crewPriceMult","Crew hiring cost"},
            {"marinesPriceMult","Marine hiring cost"},{"suppliesPriceMult","Supply price"},{"officerCombatPay","Officer combat pay"},
            {"normalTariff","Open-market tariff"},{"militaryMarketTariff","Military-market tariff"},
            {"blackMarketTariff","Black-market bribes"},{"blackMarketTariffSneak","Bribes when sneaking into port"},
            {"militaryMarketRequiresCommission","Military market requires a commission"},
            {"removeGalatiaStipend","Remove the Academy stipend"},{"shipForSaleFraction","Ships available to buy"},
            {"weaponForSaleFraction","Weapons available to buy"},{"fighterForSaleFraction","Fighter LPCs available to buy"},
            {"weaponDropFraction","Weapons found after battle"},{"fighterDropFraction","Fighter LPCs found after battle"},
            {"hullmodDropFraction","Hullmods found after battle"},{"commoditySalvageDropFraction","Commodity salvage"},
            {"aiCoreDropFraction","AI-core drops"},{"industryCostMult","Colony construction cost"},
            {"industryBuiltTimeMult","Colony construction time"},{"colonyGrowthRequiredMult","Colony growth requirements"},
            {"corruptionThreshold","Income before corruption starts"},{"corruptionExponent","Corruption strength"},
            {"corruptionUsesGrossIncome","Corruption uses income before expenses"},{"spacerSharePerMonth","Spacer lifestyle spending"},
            {"spacerShareThresholdSalaryMonths","Savings protected from spacer spending"},
            {"transverseJumpDamageMult","Readiness lost to Transverse Jump"},{"environmentalDamageMult","Damage from space hazards"},
            {"partsPriceMult","Domain-grade Parts price"},{"partsPerUnitOfSupplies","Parts recovered alongside supplies"},
            {"shipBuyPriceMultPerDMod","Price remaining per D-mod"},{"shipStoragePriceMult","Ship storage cost factor"},
            {"shipRecoveryWithStoryPointsFraction","Wrecks allowing story-point recovery"},
            {"recoveryNonOwnSPCost","Story-point recovery cost"},{"recoveryNonOwnSPCostCiv","Civilian story-point recovery cost"},
            {"bonusXP_cleanDisengage","Bonus XP for escaping combat"},{"bonusXP_avoidCargoScan","Bonus XP for avoiding a scan"},
            {"bonusXP_convinceOfficer","Bonus XP for convincing an officer"},
            {"bonusXP_recoverNoBonusXPDeploymentPoints","Ship size where recovery stops giving bonus XP"}
        };
        for(String[] pair:names)NAMES.put(pair[0],pair[1]);
    }
    public static String shortKey(SettingsModel.Entry e){return e.key.substring(4);}
    public static String name(SettingsModel.Entry e){
        String result=NAMES.get(shortKey(e));
        if(result!=null)return result;
        return e.label.replace("For Sale Fraction Player Colony","available at your colonies")
            .replace("For Sale Fraction Free Port","available at free ports").replace("For Sale Military Mult","availability at military markets")
            .replace("For Sale Fraction","availability").replace("Salvage Drop Fraction","salvage amount")
            .replace("Price Mult","price factor").replace("Fraction","amount").replace("Mult","factor")
            .replace("Civ","(civilian)").replace("SP Cost","story-point cost");
    }
    public static boolean neutral(SettingsModel.Entry e){return NEUTRAL.contains(shortKey(e));}
    public static boolean same(Object a,Object b) throws JSONException {
        if(a instanceof Number && b instanceof Number)return Math.abs(((Number)a).doubleValue()-((Number)b).doubleValue())<.00005;
        if(a instanceof JSONArray && b instanceof JSONArray){
            JSONArray aa=(JSONArray)a,bb=(JSONArray)b;if(aa.length()!=bb.length())return false;
            for(int i=0;i<aa.length();i++)if(!same(aa.get(i),bb.get(i)))return false;
            return true;
        }
        return a.equals(b);
    }
    public static boolean visible(SettingsModel.Entry e,JSONObject profile) throws JSONException {
        return e.supported() && (!neutral(e) || !same(profile.get(e.key),e.vanilla));
    }
    public static SettingsModel.Entry find(String key){
        for(SettingsModel.Entry e:SettingsModel.entries)if(e.key.equals(key) || shortKey(e).equals(key))return e;
        throw new IllegalArgumentException("Unknown setting: "+key);
    }
    public static String number(double n){return BigDecimal.valueOf(n).stripTrailingZeros().toPlainString();}
    public static String value(Object v) throws JSONException {
        if(v instanceof Number)return number(((Number)v).doubleValue());
        if(v instanceof Boolean)return (Boolean)v?"On":"Off";
        if(v instanceof JSONArray){
            JSONArray a=(JSONArray)v;StringBuilder text=new StringBuilder();
            for(int i=0;i<a.length();i++){if(i>0)text.append(" / ");text.append(value(a.get(i)));}return text.toString();
        }
        return v.toString();
    }
    public static String normal(SettingsModel.Entry e){
        String k=shortKey(e);
        if(k.equals("retirementMinCycles") || k.equals("retirementMaxCycles"))return "No forced retirement";
        if(k.equals("storyPointsPerLevel"))return "4";
        if(k.equals("normalTariff"))return "0.3 (30%, standard market)";
        if(k.equals("blackMarketTariff"))return "0 (no bribe)";
        if(k.equals("officerCombatPay"))return "0 / 0 / 0 / 0 (no extra combat pay)";
        if(k.equals("partsPerUnitOfSupplies"))return "0.05";
        if(e.reference!=null)return e.type.equals("array")?"1 / 1 / 1 / 1":number(e.reference);
        if(neutral(e))try{return value(e.vanilla);}catch(JSONException ignored){}
        return null;
    }
    public static String tooltip(SettingsModel.Entry e,Object current) throws JSONException {
        String normal=normal(e);
        String comparison=normal==null?"Normal: no verified single-value comparison for this control."
            :"Normal: "+normal;
        comparison+="\nSurvival default: "+value(e.vanilla)+"\nYour value: "+value(current);
        String timing=e.status.equals("live")?"Applies after you save these settings. Reopen affected screens if needed."
            :e.status.equals("future")?"Affects future stock or rewards. Existing items are unchanged."
            :"New campaigns only. An existing campaign keeps the results already generated.";
        return e.description+"\n\n"+comparison+(e.type.equals("array")?"\nShip order: frigate / destroyer / cruiser / capital.":"")+"\n\n"+timing;
    }
    public static String presetDescription(String name){
        if(name.equals("Easy"))return "Much lower costs and more available ships and equipment.";
        if(name.equals("Medium"))return "Some breathing room, while keeping much of Survival's scarcity.";
        if(name.equals("Hard"))return "The game's original Survival settings. This is the default.";
        if(name.equals("Insane"))return "Higher costs and even fewer ships and equipment available.";
        return "Your own choices. Nothing changes until you apply them.";
    }
    private SettingsPresentation(){}
}
