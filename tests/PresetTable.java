package survivaldial;
import org.json.*;

/** Generates the public comparison from the same preset functions the UI uses. */
public final class PresetTable {
    public static String render(JSONObject base) throws Exception {
        JSONObject[] presets=new JSONObject[4];
        for(int i=0;i<4;i++)presets[i]=SettingsModel.preset(base,SettingsModel.PRESETS[i],true);
        String[][] rows={
            {"Ship prices (frigate / destroyer / cruiser / capital)","shipBuyPriceMult","x"},
            {"Weapon prices","weaponPriceMult","x"},{"Fighter LPC prices","fighterPriceMult","x"},
            {"Crew hiring cost","crewPriceMult","x"},{"Crew wages","crewSalaryMult","x"},
            {"Fuel price","fuelPriceMult","x"},{"Ship fuel capacity","fuelCapacityMult","%"},
            {"Tanker fuel capacity","fuelCapacityMultTanker","%"},
            {"Fuel use (frigate / destroyer / cruiser / capital)","fuelUseMult","x"},
            {"Commission income","commissionMonthlyIncomeMult","%"},
            {"Post-battle ship recovery factor","shipRecoverableFraction","%"},
            {"Story points per level","storyPointsPerLevel",""},{"Retirement window","retirementMinCycles","cycles"}
        };
        StringBuilder table=new StringBuilder("| Setting | Easy | Medium | **Hard (default)** | Insane |\n| --- | --- | --- | --- | --- |\n");
        for(String[] row:rows){
            table.append("| ").append(row[0]);
            for(JSONObject p:presets){
                Object value=p.get("ext_"+row[1]);String text;
                if(row[2].equals("cycles"))text=SettingsPresentation.value(value)+"-"+SettingsPresentation.value(p.get("ext_retirementMaxCycles"))+" cycles";
                else if(row[2].equals("%"))text=SettingsPresentation.number(Math.round(((Number)value).doubleValue()*10000d)/100d)+"%";
                else text=SettingsPresentation.value(value)+row[2];
                table.append(" | ").append(text);
            }
            table.append(" |\n");
        }
        return table.toString();
    }
}
