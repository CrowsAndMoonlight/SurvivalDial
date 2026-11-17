param(
    [Parameter(Mandatory=$true)][string]$StarsectorPath,
    [Parameter(Mandatory=$true)][string]$ApiSourcePath
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$core = Join-Path $StarsectorPath 'starsector-core'
$sourceFiles = Get-ChildItem -LiteralPath $ApiSourcePath -Recurse -Filter *.java
$sources = @{}
foreach ($file in $sourceFiles) { $sources[$file.FullName.Substring($ApiSourcePath.TrimEnd('\').Length + 1).Replace('\','/')] = [IO.File]::ReadAllText($file.FullName) }
foreach ($file in Get-ChildItem -LiteralPath "$core\data\scripts" -Recurse -Filter *.java) { $sources[$file.FullName.Substring($core.Length+1).Replace('\','/')] = [IO.File]::ReadAllText($file.FullName) }
# Descriptions explain the actual quantity, not a generic "difficulty" rating.
$descriptions = @{}
@'
jumpPayMult|Extra crew pay when jumping out of a populated system, as a multiple of crew salary. Zero disables this charge.
spacerShareThresholdSalaryMonths|Savings protected from the spacer lifestyle charge, measured in months of crew salary.
spacerSharePerMonth|Monthly fraction of eligible savings spent by the spacer lifestyle. 0.10 means 10%; zero disables it.
retirementMinCycles|Earliest retirement cycle offset. Zero disables ordinary retirement checks. The date is rolled at campaign creation; changing this later does not reroll it.
retirementMaxCycles|Latest retirement cycle offset. Must be at least the minimum. Used when the retirement date is first rolled.
retirementDelayCostBase|Starting monthly rejuvenation cost in credits. Stored when the retirement script is created.
retirementDelayCostPerMonth|Credits added to rejuvenation costs each month after postponing retirement.
retirementDelayCostMax|Maximum monthly rejuvenation charge in credits.
shipBuyPriceMult|Ship purchase price multiplier by hull size. 1 means the normal-mode reference price; 5 means five times that price before other modifiers.
pristineShipSellPriceMult|Resale multiplier for pristine ships by hull size, combined with the game's ordinary ship resale factor. It is not a percentage of the purchase price.
shipSellValueIncreasePerPartUsed|Credits added to resale value per Domain-grade Part spent repairing the ship, with the game's repair-count cap.
shipSellValueMultPerSMod|Resale bonus per built-in S-mod. The multiplier is 1 + this value times the number of S-mods.
shipStoragePriceMult|Multiplier used for the ship value underlying storage charges. Lower values reduce that component of storage fees.
shipBuyPriceMultPerDMod|Remaining purchase-price fraction per D-mod. 0.85 leaves 85% per D-mod; smaller values make damaged ships cheaper.
weaponPriceMult|Weapon base-price multiplier. 1 restores the normal-mode base price; 10 means ten times that price.
fighterPriceMult|Fighter LPC base-price multiplier, before market and resale modifiers.
hullmodPriceMult|Hullmod package base-price multiplier, before resale modifiers.
hullmodSellPriceMult|Multiplier applied when selling hullmod packages. Higher values increase sale proceeds.
weaponAndFighterSellPriceMult|Resale multiplier for weapons and fighter LPCs, applied after their Extended price multiplier.
blueprintSellPriceMult|Blueprint resale multiplier outside the black market.
blueprintSellPriceMultBlackMarket|Blueprint resale multiplier on the black market.
partsForSaleFraction|Domain-grade Parts stock factor based on available supplies. Affects future stock generation, not an immediate inventory refill.
partsPriceMult|Domain-grade Parts price multiplier relative to their base price of 10,000 credits.
partsSpecialSalvageDropFraction|Domain-grade Parts factor for special salvage drops, such as the Domain mothership. Existing rolled loot is unchanged.
partsPerUnitOfSupplies|Domain-grade Parts generated per eligible unit of supplies in salvage calculations.
shipForSaleFraction|Ship availability factor for newly generated market stock. Lower values mean fewer ships.
shipRecoverableFraction|Fraction used for ordinary post-battle ship recovery availability. Existing recovery lists are unchanged.
derelictShipRecoverableFraction|Recovery availability factor for derelict ships when their result is generated.
shipRecoveryWithStoryPointsFraction|Availability factor for additional recoveries requiring story points.
weaponForSaleFraction|Weapon availability factor for future market stock refreshes.
weaponDropFraction|Weapon drop factor after battles. Existing loot is unchanged.
weaponSalvageDropFraction|Weapon drop factor when generating salvage-site loot.
fighterForSaleFraction|Fighter LPC availability factor for future market stock refreshes.
fighterDropFraction|Fighter LPC drop factor after battles.
fighterSalvageDropFraction|Fighter LPC drop factor for salvage sites.
hullmodForSaleFraction|Hullmod package availability factor for future market stock refreshes.
hullmodDropFraction|Hullmod package drop factor after battles.
hullmodSalvageDropFraction|Hullmod package drop factor for salvage sites.
specialItemSalvageDropFraction|Special-item salvage factor. Does not recreate loot already generated.
commoditySalvageDropFraction|Ordinary commodity salvage factor. Does not recreate loot already generated.
sleeperPodsSpecialCrewFraction|Crew reward factor for special sleeper-pod salvage.
salvageCargoFractionPostCombat|Fraction of cargo made available through post-combat salvage calculations.
threatResourceSalvageMult|Resource salvage multiplier for Threat encounters.
aiCoreDropFraction|AI-core drop factor for future loot rolls.
substrateDropFraction|Substrate drop factor for future loot rolls.
barCustomProductionMaxValueMult|Maximum custom-production budget multiplier for bar offers. Existing offers keep their generated terms.
playerIndustryCustomProductionMaxValueMult|Maximum production-budget multiplier for player industry custom orders.
fuelCapacityMult|Fuel capacity multiplier for non-tankers. 0.5 gives half normal capacity; 1 restores it. Lowering capacity can leave your fleet over capacity.
fuelCapacityMultTanker|Fuel capacity multiplier for ships tagged as tankers. 0.35 gives 35% of normal capacity.
fuelUseMult|Fuel use per light-year multiplier for non-civilian ships, by hull size. 2 doubles consumption.
fuelUseMultCiv|Fuel use per light-year multiplier for civilian ships, by hull size.
personnelForSaleMinMultBadRep|Personnel availability multiplier at poor faction reputation.
personnelForSaleMaxMultGoodRep|Personnel availability multiplier at good faction reputation.
localResourcesStockpileAndRateMult|Player-colony local-resource stockpile and accumulation-rate multiplier.
commissionBountyMult|Commission bounty payment multiplier for future eligible kills.
commissionMonthlyIncomeMult|Monthly commission income multiplier. 0.1 pays one tenth of normal.
removeGalatiaStipend|Removes the Galatia Academy stipend in Extended mode. Disabling this does not retroactively pay missed income.
taskForceCostMult|Credit cost multiplier when requesting a task force.
resupplyFleetCostMult|Credit cost multiplier when requesting a resupply fleet.
resupplyCrewBroughtMult|Crew quantity multiplier for newly requested resupply fleets.
crewPriceMult|Crew purchase price multiplier relative to a base of 50 credits per crew member.
marinesPriceMult|Marine purchase price multiplier relative to a base of 200 credits per marine.
suppliesPriceMult|Supply base-price multiplier relative to 100 credits per unit.
fuelPriceMult|Fuel base-price multiplier relative to 25 credits per unit.
personnelSellPriceMult|Personnel resale multiplier. Higher values increase credits received when selling personnel.
crewSalaryMult|Monthly crew salary multiplier relative to the ordinary salary calculation. 3 triples it.
marineSalaryMult|Monthly marine salary multiplier relative to the ordinary salary calculation.
officerSalaryMult|Officer salary multiplier. Higher values increase officer pay.
officerCombatPay|Officer combat pay multiplier by commanded ship size, based on officer salary.
colonyGrowthRequiredMult|Population growth required to reach the next colony size. Higher values slow growth.
corruptionThreshold|Colony income threshold in credits above which corruption applies. Higher values protect more income.
corruptionExponent|Exponent controlling income compression above the corruption threshold. 1 removes the compression; larger values strengthen it.
corruptionUsesGrossIncome|Whether corruption uses gross colony income rather than income after expenses.
officersAvailableFraction|Officer availability factor for future recruitment generation.
cryopodOfficersFraction|Officer availability factor for future cryopod results.
cryopodAdminsFraction|Administrator availability factor for future cryopod results.
adminAvailableFraction|Administrator availability factor for future recruitment generation.
normalTariff|Open-market tariff fraction. 0.4 means a 40% tariff.
militaryMarketTariff|Military-market tariff fraction. 0.3 means a 30% tariff.
blackMarketTariff|Black-market tariff fraction when trading openly. 0.25 means 25%.
blackMarketTariffSneak|Black-market tariff fraction when sneaking in with the transponder off.
militaryMarketRequiresCommission|Requires a commission to use military markets when enabled.
certainWeaponCooldownsBlockPhaseCloak|The shipped settings explicitly mark this setting as unused. No control is offered.
strongerAlphaSiteDefenders|Uses the stronger Alpha Site defenders when that encounter is generated. Existing defenders are unchanged.
analyzeMissionRequiresSensorStrength|Requires extra sensor strength for applicable analysis missions.
analyzeMissionSensorStrengthAboveBaseRequired|Extra sensor strength above the base required by applicable analysis missions.
ZGRTurnInValueMult|Reward value multiplier for turning in the related unique ship. Existing mission rewards may already be fixed.
moreDangerousMkIThreat|Enables the more dangerous Mk I Threat variant when its encounter is generated.
extraSpacerDebtPerLevel|Extra spacer debt in credits per character level.
respawnFleetId|Fleet definition used on respawn. This is an identifier rather than a difficulty number; no slider is offered.
deliveryMissionHighQuantityChanceMult|Chance multiplier for high-quantity delivery offers. Existing missions are unchanged.
worseHandMeDownFreighter|Uses the worse freighter variant for newly generated hand-me-down offers.
worseHandMeDownFreighterPriceMult|Price multiplier for the worse hand-me-down freighter offer.
hijackedShipCostMult|Price multiplier for newly generated hijacked-ship offers.
surplusHullPriceMult|Price multiplier for newly generated surplus-hull offers.
reduceLegionXIVSpawns|Reduces special XIV Legion wreck spawns during sector generation. Existing wrecks are unchanged.
reduceDerelictQuality|Reduces the quality of generated derelicts. Existing wrecks are unchanged.
shipForSaleQualityMod|Additive quality modifier for newly generated sale ships. More negative values generally mean poorer condition.
probOfForcingBadDerelictCondition|Probability of forcing bad condition on a generated derelict. 0 disables the extra forced downgrade; 1 always applies it.
cargoScanFindContrabandMult|Multiplier on contraband detection during cargo scans.
cargoScanNumShipsMult|Multiplier on the number of ships affected by cargo scans.
cargoScanShipDamageMult|Ship damage multiplier from cargo scans.
environmentalDamageMult|Damage multiplier from hazards such as hyperstorm strikes and asteroid impacts.
transverseJumpDamageMult|Combat-readiness damage multiplier for Transverse Jump. 10 means ten times the normal CR damage.
timeBasedThingsDelayFactor|Delays selected time-based escalation, including pirate raids and fleet-strength growth. Higher values postpone escalation.
surveyCrewMult|Crew requirement multiplier for planetary surveys. Required crew are not necessarily consumed.
surveyMachineryMult|Heavy-machinery requirement multiplier for surveys. Required machinery is not necessarily consumed.
surveySuppliesMult|Supply cost multiplier for planetary surveys.
establishColonyCrew|Crew required to establish a colony.
establishColonyMachinery|Heavy machinery required to establish a colony.
establishColonySupplies|Supplies required to establish a colony.
industryCostMult|Industry construction cost multiplier. Existing queued construction may retain its original cost.
industryBuiltTimeMult|Industry construction time multiplier. Existing build progress is not rewound.
missionRewardMinDist|Travel distance in light-years where the distance reward bonus starts. Must not exceed the maximum distance.
missionRewardMaxDist|Travel distance in light-years where the distance reward bonus reaches its maximum.
missionRewardMaxBonusFraction|Maximum distance-based reward bonus fraction. 0.65 means up to 65% extra.
missionRewardBonusCap|Maximum distance-based mission reward bonus in credits.
storyPointsPerLevel|Story points granted per future level gained below the level cap. Does not change previously awarded points.
storyPointsPerLevelAtMaxLevel|Story points awarded for future XP level intervals at maximum level.
bonusXP_cleanDisengage|Bonus-XP fraction attached to the clean-disengagement story option.
bonusXP_avoidCargoScan|Bonus-XP fraction attached to the avoid-cargo-scan story option.
bonusXP_convinceOfficer|Bonus-XP fraction attached to the convince-officer story option.
bonusXP_recoverNoBonusXPDeploymentPoints|Deployment-point threshold used for ship-recovery bonus-XP calculations.
recoveryNonOwnSPCost|Story-point recovery cost for ships that were not yours, by hull size.
recoveryNonOwnSPCostCiv|Story-point recovery cost for civilian ships that were not yours, by hull size.
'@ -split "`n" | ForEach-Object { $pair=$_.Trim().Split('|',2); if($pair.Count -eq 2){$descriptions[$pair[0]]=$pair[1]} }
$live = 'jumpPayMult spacerShareThresholdSalaryMonths spacerSharePerMonth retirementDelayCostPerMonth retirementDelayCostMax shipStoragePriceMult shipSellValueIncreasePerPartUsed shipSellValueMultPerSMod weaponPriceMult fuelCapacityMult fuelCapacityMultTanker fuelUseMult fuelUseMultCiv crewSalaryMult marineSalaryMult officerSalaryMult officerCombatPay corruptionThreshold corruptionExponent corruptionUsesGrossIncome cargoScanFindContrabandMult cargoScanNumShipsMult cargoScanShipDamageMult environmentalDamageMult transverseJumpDamageMult surveyCrewMult surveyMachineryMult surveySuppliesMult'.Split(' ')
$live += 'normalTariff militaryMarketTariff blackMarketTariff blackMarketTariffSneak militaryMarketRequiresCommission'.Split(' ')
$engine = @{}
foreach($name in 'shipBuyPriceMult pristineShipSellPriceMult shipBuyPriceMultPerDMod weaponAndFighterSellPriceMult'.Split(' ')){$engine[$name]='campaign/fleet/FleetMember.getBaseBuyValue/getBaseSellValue'}
foreach($name in 'hullmodSellPriceMult blueprintSellPriceMult blueprintSellPriceMultBlackMarket personnelSellPriceMult'.Split(' ')){$engine[$name]='campaign/ui/trade/E.computeCurrentSingleItemSellCost/computeSellCost'}
$engine.fighterPriceMult='loading/specs/FighterWingSpec.getBaseValue'
$engine.hullmodPriceMult='loading/specs/O.getBaseValue'
$engine.playerIndustryCustomProductionMaxValueMult='loading/specs/FactionProduction.getProductionCapacityForMarket'
$engine.industryCostMult='loading/specs/H.getCost'
$engine.industryBuiltTimeMult='loading/specs/H.getBuildTime'
$engine.recoveryNonOwnSPCost='ui/newui/FleetMemberRecoveryDialog.getSPToRecoverMember'
$engine.recoveryNonOwnSPCostCiv=$engine.recoveryNonOwnSPCost
foreach($name in 'crewPriceMult marinesPriceMult suppliesPriceMult fuelPriceMult partsPriceMult'.Split(' ')){$engine[$name]='loading/N.getBasePrice (ext_ + commodity id + PriceMult)'}
$presets = @{}
# Reference endpoints for curated presets: costs rise with difficulty; availability falls.
foreach($name in 'shipBuyPriceMult weaponPriceMult fighterPriceMult hullmodPriceMult partsPriceMult fuelUseMult fuelUseMultCiv crewPriceMult marinesPriceMult fuelPriceMult crewSalaryMult marineSalaryMult environmentalDamageMult transverseJumpDamageMult industryCostMult industryBuiltTimeMult colonyGrowthRequiredMult surveyCrewMult surveyMachineryMult'.Split(' ')){ $presets[$name]=1 }
foreach($name in 'fuelCapacityMult fuelCapacityMultTanker shipForSaleFraction shipRecoverableFraction derelictShipRecoverableFraction weaponForSaleFraction weaponDropFraction fighterForSaleFraction fighterDropFraction hullmodForSaleFraction hullmodDropFraction commoditySalvageDropFraction aiCoreDropFraction commissionMonthlyIncomeMult'.Split(' ')){ $presets[$name]=1 }
$rows=@()
foreach($line in Get-Content -LiteralPath "$core\data\config\settings.json") {
    if($line -notmatch '^\s*"(ext_([^" ]+))"\s*:\s*(.+?)\s*,\s*(?:#.*)?$'){continue}
    $key=$Matches[1]; $name=$Matches[2]; $value=ConvertFrom-Json $Matches[3]
    # Rebuild arrays: Windows PowerShell's pipeline can attach Count/value metadata
    # to ConvertFrom-Json results, which otherwise serializes as an object.
    if($value -is [array]){$value=@($value | ForEach-Object {[double]$_})}
    $type=if($value -is [bool]){'boolean'}elseif($value -is [array]){'array'}elseif($value -is [string]){'string'}else{'number'}
    $label=([regex]::Replace($name,'([a-z0-9])([A-Z])','$1 $2')).Replace('_',' ')
    $label=$label.Substring(0,1).ToUpper()+$label.Substring(1)
    $description=$descriptions[$name]
    if(!$description -and $name -match '^(crew|supplies|fuel|marines|drugs|organs|hand_weapons)ForSale(Fraction|MilitaryMult)(.*)$') {
        $item=$Matches[1].Replace('_',' '); $context=switch($Matches[3]) {'FreePort'{' at free ports'} 'PlayerColony'{' at player colonies'} default{''}}
        if($Matches[2] -eq 'MilitaryMult'){$context=' at military markets'}
        $description="Availability multiplier for $item$context. 0.5 means half the reference stock. Applies to future stock generation; it does not refill existing inventories."
    }
    if(!$description){throw "Missing description: $key"}
    $evidence=@($sources.Keys | Where-Object {$sources[$_].Contains('"'+$key+'"')} | Sort-Object)
    $status=if($name -eq 'certainWeaponCooldownsBlockPhaseCloak'){'unused'}elseif($type -eq 'string'){'identifier'}elseif($live -contains $name){'live'}elseif($evidence.Count){'creation'}else{'unverified'}
    if($engine.ContainsKey($name)){$status='live'; $evidence+=('starfarer_obf.jar: '+$engine[$name]+' (dynamic getter inspected with javap)')}
    if($name -match '^(crew|supplies|fuel|marines|drugs|organs|hand_weapons)ForSale'){$status='future'; $evidence+='com/fs/starfarer/api/impl/campaign/submarkets/BaseSubmarketPlugin.java:getCommodityAvailabilityMult (dynamic key)'}
    if($name -match '^missionReward'){$status='future'; $evidence+='com/fs/starfarer/api/util/Misc.java:getMissionDistanceRewardMult (prefix + key)'}
    if($name -match '^bonusXP_'){$status='future'; $evidence+='starfarer_obf.jar: StarfarerSettings bonus-XP getter (ext_bonusXP_ + action, inspected with javap)'}
    if($name -in @('weaponForSaleFraction','fighterForSaleFraction','hullmodForSaleFraction','shipForSaleFraction','weaponDropFraction','fighterDropFraction','hullmodDropFraction','salvageCargoFractionPostCombat','aiCoreDropFraction','shipRecoverableFraction','threatResourceSalvageMult')){$status='future'}
    if($name -in @('fuelCapacityMult','fuelCapacityMultTanker','fuelUseMult','fuelUseMultCiv')){$evidence+= 'starfarer_obf.jar: loading/specs/O00o$Oo (dynamic getters, inspected with javap)'}
    if($name -eq 'weaponPriceMult'){$evidence+= 'starfarer_obf.jar: BaseWeaponSpec.getBaseValue (dynamic getter, inspected with javap)'}
    $category=if($name -match 'retirement|spacer|jumpPay'){'Career'}elseif($name -match 'fuelCapacity|fuelUse|Salary|CombatPay|survey|environmental|transverse|cargoScan'){'Fleet and travel'}elseif($name -match 'colony|Colony|corruption|industry|Industry|establish|localResources'){'Colonies'}elseif($name -match 'Drop|Salvage|salvage|Recover|recover|derelict|Derelict|cryopod|sleeper'){'Salvage and recovery'}elseif($name -match 'Mission|mission|Threat|Alpha|Legion|HandMe|handMe|hijacked|surplus|ZGRT|timeBased|storyPoints|bonusXP'){'Missions and progression'}else{'Markets and income'}
    $maxDefault=if($type -in @('number','array')){[double](($value | Measure-Object -Maximum).Maximum)}else{1}
    $min=0.0; $max=[Math]::Max(2, $maxDefault*4)
    $integer=$name -match 'CostBase|CostPerMonth|CostMax|Cycles|ThresholdSalaryMonths|PerPartUsed|Threshold$|AboveBaseRequired|DebtPerLevel|establishColony|BonusCap|^storyPoints|NonOwnSPCost|recoverNoBonusXP'
    if($name -match 'Tariff|spacerSharePerMonth|probOfForcing'){$max=1}
    if($name -eq 'shipBuyPriceMultPerDMod'){$min=0.05; $max=1}
    if($name -match 'fuelCapacity'){$min=0.05; $max=3}
    if($name -eq 'corruptionExponent'){$min=1; $max=5}
    if($name -eq 'corruptionThreshold'){$min=1}
    if($name -eq 'shipForSaleQualityMod'){$min=-1; $max=1}
    if($name -match '^storyPoints|NonOwnSPCost'){$min=1; $max=20}
    $row=[ordered]@{key=$key; label=$label; category=$category; type=$type; default=$value; min=$min; max=$max; integer=[bool]$integer; status=$status; description=$description; evidence=$evidence}
    if($presets.ContainsKey($name)){$row.presetReference=$presets[$name]}
    $rows += [pscustomobject]$row
}
$expected=(Select-String -LiteralPath "$core\data\config\settings.json" -Pattern '^\s*"ext_').Count
if($rows.Count -ne $expected){throw "Parsed $($rows.Count) of $expected keys"}
$utf8=New-Object Text.UTF8Encoding($false)
[IO.Directory]::CreateDirectory("$root\data\config") | Out-Null
[IO.Directory]::CreateDirectory("$root\docs") | Out-Null
[IO.File]::WriteAllText("$root\data\config\survival_dial_catalog.json",(ConvertTo-Json -InputObject @($rows) -Depth 8),$utf8)
$doc=@('# Extended / Survival setting inventory','','Audited against the installed Starsector 0.98.5a-RC7 on 2026-11-16. This inventory includes every `ext_` key in the shipped settings file. Defaults are Extended defaults, not inferred normal-mode values.','','Status: **live** = dynamic consumer inspected; **future** = subsequent stock, offers or rolls only; **creation** = source consumer found but limited to creation to avoid claiming retroactive changes; **unverified** = setting exists but its effect/timing has not been established from the supplied source; **unused** = explicitly marked unused; **identifier** = not numeric. Unsupported controls are omitted from the menu; unchanged defaults are hidden unless previously customized. Source inspection is not an in-game playtest.','','Array order: frigate, destroyer, cruiser, capital.','','| Setting | Extended default | Availability | Effect and evidence |','| --- | --- | --- | --- |')
foreach($row in $rows){$default=ConvertTo-Json -InputObject $row.default -Compress; $ev=($row.evidence | ForEach-Object {'`'+$_+'`'}) -join '; '; $doc+='| `'+$row.key+'` | `'+$default+'` | '+$row.status+' | '+$row.description+' '+$ev+' |'}
[IO.File]::WriteAllLines("$root\docs\setting-inventory.md",$doc,$utf8)
$rows | Group-Object status | Select-Object Name,Count
