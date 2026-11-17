package com.fs.starfarer.api.impl.campaign.rulecmd;

import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.campaign.rules.MemKeys;
import com.fs.starfarer.api.characters.CharacterCreationData;
import com.fs.starfarer.api.util.Misc.Token;
import java.util.*;
import org.json.JSONObject;
import survivaldial.*;

/** Called after selecting Extended mode, before the vanilla occupation choices. */
public class SDStart extends BaseCommandPlugin {
    public boolean execute(String ruleId,final InteractionDialogAPI dialog,List<Token> params,final Map<String,MemoryAPI> memoryMap) {
        try {
            CharacterCreationData character=(CharacterCreationData)memoryMap.get(MemKeys.LOCAL).get("$characterData");
            if(character==null || !character.isExtMode())return false;
            SettingsModel.pending=null;
            SettingsModel.restore();
            final InteractionDialogPlugin previous=dialog.getPlugin();
            SettingsDialog editor=new SettingsDialog(SettingsModel.baseline(),true,new SettingsDialog.OnClose(){
                public void close(JSONObject profile) throws Exception {
                    SettingsModel.applyForNewExtendedGame(profile);
                    SettingsModel.pending=SettingsModel.copy(profile);
                    dialog.setPlugin(previous);
                    dialog.getOptionPanel().clearOptions();
                    dialog.getTextPanel().clear();
                    FireBest.fire(null,dialog,memoryMap,"SDContinueExt");
                }
            });
            dialog.setPlugin(editor); editor.init(dialog);
            return true;
        } catch(Exception e){throw new RuntimeException("Survival Dial could not start character setup",e);}
    }
}
