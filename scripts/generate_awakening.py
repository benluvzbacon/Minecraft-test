#!/usr/bin/env python3
"""Add the 2.0 datapack without changing any legacy Rift/Overworld definitions."""
import json
from resource_tools import RES,write_json
from generate_data import entry,pool,uniform
from awakening_content import *
D='data/riftborn/'; A='assets/riftborn/'
def merge_tag(path,values):
    file=RES/path
    old=json.loads(file.read_text()) if file.exists() else {'replace':False,'values':[]}
    for value in values:
        if value not in old['values']:old['values'].append(value)
    write_json(path,old)
def shaped(name,pattern,keys):
    write_json(D+'recipe/awakening/'+name+'.json',{'type':'minecraft:crafting_shaped','category':'equipment','pattern':pattern,'key':{key:{'item':value if ':' in value else 'riftborn:'+value} for key,value in keys.items()},'result':{'id':'riftborn:'+name,'count':1}})
def shapeless(name,result,ingredients,count=1):
    write_json(D+'recipe/awakening/'+name+'.json',{'type':'minecraft:crafting_shapeless','category':'misc','ingredients':[{'item':i if ':' in i else 'riftborn:'+i} for i in ingredients],'result':{'id':'riftborn:'+result,'count':count}})
def recipes():
    shaped('abyssal_key',['CHC','FSF','CHC'],{'C':'rift_core','H':'rift_heart','F':'void_fragment','S':'resonant_sigil'})
    shapeless('abyssal_crystal','abyssal_crystal',['abyssal_shard']*4+['rift_dust'])
    shaped('abyssal_core',['CEC','ERE','CEC'],{'C':'abyssal_crystal','E':'abyssal_essence','R':'rift_core'})
    shaped('abyssal_crystal_block',['CCC','CCC','CCC'],{'C':'abyssal_crystal'})
    shapeless('crystal_from_block','abyssal_crystal',['abyssal_crystal_block'],9)
    shaped('abyssal_greatblade',[' C ','CTC',' R '],{'C':'abyssal_crystal','T':'titan_core','R':'abyssal_core'})
    shaped('voidbow',['CEC','CBC','CEC'],{'C':'abyssal_crystal','E':'abyssal_essence','B':'minecraft:bow'})
    shaped('rift_staff',['CSC',' R ',' B '],{'C':'abyssal_crystal','S':'reality_spindle','R':'abyssal_core','B':'minecraft:blaze_rod'})
    shaped('abyssal_eye',['CEC','ERE','CEC'],{'C':'abyssal_crystal','E':'abyssal_essence','R':'rift_compass'})
    shaped('void_core',['CGC','GRG','CGC'],{'C':'abyssal_crystal','G':'stormglass','R':'abyssal_core'})
    shaped('warding_anchor',[' G ','RSC',' G '],{'G':'stormglass','R':'rift_core','S':'minecraft:shield','C':'abyssal_core'})
    shaped('heart_of_the_rift',['CTC','HSH','CRC'],{'C':'abyssal_crystal','T':'titan_core','H':'rift_heart','S':'sovereign_heart','R':'reality_spindle'})
    shaped('collapse_catalyst',['CEC','TRS','CEC'],{'C':'abyssal_crystal','E':'abyssal_essence','T':'titan_core','R':'abyssal_core','S':'reality_spindle'})
    shaped('sovereign_standard',['CCC','CHC','CCC'],{'C':'abyssal_crystal','H':'sovereign_heart'})
    shaped('abyssal_schematic',['CSC','CRC','CCC'],{'C':'abyssal_crystal','S':'abyssal_schematic','R':'rift_stone'})
    file=RES/(D+'recipe/awakening/abyssal_schematic.json');data=json.loads(file.read_text());data['result']['count']=2;write_json(D+'recipe/awakening/abyssal_schematic.json',data)
    for armor in ARMOR:
        write_json(D+'recipe/awakening/'+armor+'.json',{'type':'minecraft:smithing_transform','template':{'item':'riftborn:abyssal_schematic'},'base':{'item':'riftborn:'+armor.replace('abyssal_','rift_')},'addition':{'item':'riftborn:abyssal_core'},'result':{'id':'riftborn:'+armor,'count':1}})
    for path in (RES/(D+'recipe/awakening')).glob('*.json'):
        name=path.stem;trigger='resonant_sigil' if name=='abyssal_key' else 'abyssal_shard'
        write_json(D+'advancement/awakening/recipes/'+name+'.json',{'parent':'minecraft:recipes/root','criteria':{'material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'riftborn:'+trigger}]}},'recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':'riftborn:awakening/'+name}}},'requirements':[['material','recipe']],'rewards':{'recipes':['riftborn:awakening/'+name]}})
    for tag in ['armor','enchantable/armor','enchantable/equippable','enchantable/durability','trimmable_armor']:
        merge_tag('data/minecraft/tags/item/'+tag+'.json',['riftborn:'+name for name in ARMOR])
    for slot,name in zip(['head','chest','leg','foot'],ARMOR):
        for tag in [slot+'_armor','enchantable/'+slot+'_armor']:merge_tag('data/minecraft/tags/item/'+tag+'.json',['riftborn:'+name])
    for tag in ['swords','enchantable/sword','enchantable/weapon','enchantable/sharp_weapon','enchantable/durability']:
        merge_tag('data/minecraft/tags/item/'+tag+'.json',['riftborn:abyssal_greatblade'])
    merge_tag('data/minecraft/tags/item/enchantable/bow.json',['riftborn:voidbow'])
    merge_tag('data/minecraft/tags/item/enchantable/durability.json',['riftborn:voidbow','riftborn:rift_staff'])
    merge_tag('data/minecraft/tags/block/mineable/pickaxe.json',['riftborn:'+b for b in BLOCKS if b not in PLANTS])
    merge_tag('data/minecraft/tags/block/needs_diamond_tool.json',['riftborn:abyssal_crystal_ore'])
    merge_tag('data/minecraft/tags/block/needs_iron_tool.json',['riftborn:abyssal_stone','riftborn:luminous_shale'])

def loot():
    rituals={'abyss_gate','resonator','resonance_lock','ancient_stele','colossus_altar','architect_altar','sovereign_altar','phase_barrier'}
    for name in BLOCKS:
        entries=[] if name in rituals else [pool([entry('riftborn:abyssal_shard',2,4)])] if name=='abyssal_crystal_ore' else [pool([entry('riftborn:'+name)])]
        if entries:entries[0]['conditions']=[{'condition':'minecraft:survives_explosion'}]
        write_json(D+'loot_table/blocks/'+name+'.json',{'type':'minecraft:block','pools':entries})
    drops={'abyss_stalker':[('abyssal_shard',1,3),('abyssal_essence',0,1)],'void_reaver':[('abyssal_essence',1,2)],'abyssal_brute':[('abyssal_shard',3,5),('abyssal_essence',1,2)],'rift_echo':[('abyssal_essence',1,2),('abyssal_shard',1,2)],'abyssal_warden':[('abyssal_crystal',1,2),('abyssal_essence',2,4)],'abyssal_colossus':[('titan_core',2,3),('abyssal_crystal',6,10),('abyssal_essence',8,12)],'rift_architect':[('reality_spindle',2,3),('abyssal_core',1,2),('abyssal_essence',8,12)],'abyss_sovereign':[('sovereign_heart',1,1),('abyssal_core',3,4),('abyssal_crystal',12,18)],'collapse_herald':[('sovereign_sigil',1,1),('stormglass',4,7),('abyssal_essence',4,6)]}
    for mob,items in drops.items():
        pools=[pool([entry('riftborn:'+name,a,b,looting=mob in list(MOBS)[:5])]) for name,a,b in items]
        if mob in list(MOBS)[:5]:
            for p in pools:p['conditions']=[{'condition':'minecraft:inverted','term':{'condition':'minecraft:entity_properties','entity':'this','predicate':{'nbt':'{AbyssSummoned:1b}'}}}]
        write_json(D+'loot_table/entities/'+mob+'.json',{'type':'minecraft:entity','pools':pools})
    for name,spec in STRUCTURES.items():
        abyss=spec[2]=='abyss'
        entries=[entry('minecraft:bread',3,8,6),entry('minecraft:golden_carrot',2,5,2),entry('riftborn:rift_core',1,1,2),entry('riftborn:rift_shard',3,8,5)]
        if abyss:entries += [entry('riftborn:abyssal_shard',3,8,8),entry('riftborn:abyssal_essence',2,5,5),entry('riftborn:abyssal_crystal',1,3,2),entry('riftborn:stormglass',1,2,1)]
        else:entries += [entry('riftborn:void_fragment',2,4,3),entry('riftborn:rift_dust',3,6,3)]
        pools=[pool(entries,uniform(3,6))]
        if name=='forgotten_laboratory':pools.insert(0,pool([entry('riftborn:abyssal_schematic',2,3)]))
        if name=='storm_observatory':pools.insert(0,pool([entry('riftborn:stormglass',4,6)]))
        if name=='abyssal_fortress':pools.append({'rolls':1,'entries':[entry('riftborn:abyssal_eye')],'conditions':[{'condition':'minecraft:random_chance','chance':0.04}]})
        write_json(D+'loot_table/chests/awakening/'+name+'.json',{'type':'minecraft:chest','pools':pools})

def worldgen():
    def op(kind,a,b):return {'type':'minecraft:'+kind,'argument1':a,'argument2':b}
    def gradient(a,b,c,d):return {'type':'minecraft:y_clamped_gradient','from_y':a,'to_y':b,'from_value':c,'to_value':d}
    def noise(name,xz=1,y=1):return {'type':'minecraft:noise','noise':'riftborn:awakening/'+name,'xz_scale':xz,'y_scale':y}
    for name,octave,amps in [('abyss_landshape',-8,[1.0,.65,.25]),('abyss_fold',-6,[1.0,.5,.3]),('abyss_caves',-5,[1.0,.4])]:
        write_json(D+'worldgen/noise/awakening/'+name+'.json',{'firstOctave':octave,'amplitudes':amps})
    lower=op('min',gradient(0,76,-3,.3),gradient(76,158,.3,-3))
    upper=op('min',gradient(148,236,-3,.45),gradient(236,384,.45,-4.5))
    continental=op('mul',1.7,{'type':'minecraft:cache_2d','argument':noise('abyss_landshape',1,0)})
    solid=op('add',op('max',lower,upper),op('add',op('add',continental,-.28),op('mul',.28,noise('abyss_fold',1,.65))))
    cavern=op('mul',8,op('add',{'type':'minecraft:abs','argument':noise('abyss_caves',1,1)},-.07))
    cave_bands={'type':'minecraft:range_choice','input':gradient(0,384,0,384),'min_inclusive':85,'max_exclusive':235,'when_in_range':cavern,'when_out_of_range':64}
    final=op('min',solid,cave_bands)
    write_json(D+'worldgen/density_function/awakening/abyss_mass.json',solid)
    write_json(D+'worldgen/density_function/awakening/abyss_hollows.json',final)
    router={name:0.0 for name in ['barrier','fluid_level_floodedness','fluid_level_spread','lava','temperature','vegetation','continents','erosion','depth','ridges','vein_toggle','vein_ridged','vein_gap']}
    router['initial_density_without_jaggedness']='riftborn:awakening/abyss_mass';router['final_density']={'type':'minecraft:squeeze','argument':{'type':'minecraft:interpolated','argument':'riftborn:awakening/abyss_hollows'}}
    write_json(D+'worldgen/noise_settings/the_abyss.json',{'sea_level':0,'disable_mob_generation':False,'aquifers_enabled':False,'ore_veins_enabled':False,'legacy_random_source':False,'default_block':{'Name':'riftborn:abyssal_stone'},'default_fluid':{'Name':'minecraft:air'},'noise':{'min_y':0,'height':384,'size_horizontal':2,'size_vertical':2},'noise_router':router,'spawn_target':[],'surface_rule':{'type':'minecraft:condition','if_true':{'type':'minecraft:stone_depth','offset':0,'add_surface_depth':False,'secondary_depth_range':0,'surface_type':'floor'},'then_run':{'type':'minecraft:block','result_state':{'Name':'riftborn:luminous_shale'}}}})
    write_json(D+'dimension_type/the_abyss.json',{'ultrawarm':False,'natural':False,'coordinate_scale':1.0,'has_skylight':False,'has_ceiling':False,'ambient_light':.18,'fixed_time':18000,'piglin_safe':False,'bed_works':False,'respawn_anchor_works':False,'has_raids':False,'logical_height':384,'min_y':0,'height':384,'infiniburn':'#minecraft:infiniburn_end','effects':'riftborn:the_abyss','monster_spawn_block_light_limit':7,'monster_spawn_light_level':{'type':'minecraft:uniform','min_inclusive':0,'max_inclusive':7}})
    write_json(D+'dimension/the_abyss.json',{'type':'riftborn:the_abyss','generator':{'type':'minecraft:noise','settings':'riftborn:the_abyss','biome_source':{'type':'minecraft:fixed','biome':'riftborn:abyssal_expanse'}}})
    features=[[] for _ in range(11)];features[6]=['riftborn:awakening/abyss_ore'];features[2]=['riftborn:awakening/crystal_spires'];features[9]=['riftborn:awakening/echo_ferns','riftborn:awakening/ashen_reeds','riftborn:awakening/brambles']
    spawns=[{'type':'riftborn:'+name,'weight':weight,'minCount':1,'maxCount':count} for name,weight,count in [('abyss_stalker',40,2),('void_reaver',26,2),('abyssal_brute',9,1),('rift_echo',18,2),('abyssal_warden',6,1)]]
    write_json(D+'worldgen/biome/abyssal_expanse.json',{'has_precipitation':False,'temperature':.1,'downfall':0,'effects':{'fog_color':0x071c28,'sky_color':0x05151f,'water_color':0x36bbc2,'water_fog_color':0x103b48,'foliage_color':0x6bddc6,'grass_color':0x174a50,'particle':{'options':{'type':'riftborn:abyss_mote'},'probability':.002},'mood_sound':{'sound':'minecraft:ambient.basalt_deltas.mood','tick_delay':8000,'block_search_extent':8,'offset':2}},'spawners':{'monster':spawns,**{key:[] for key in ['creature','ambient','axolotls','underground_water_creature','water_creature','water_ambient','misc']}},'spawn_costs':{},'carvers':{},'features':features})
    write_json(D+'worldgen/configured_feature/awakening/abyss_ore.json',{'type':'minecraft:ore','config':{'size':7,'discard_chance_on_air_exposure':.2,'targets':[{'target':{'predicate_type':'minecraft:block_match','block':'riftborn:abyssal_stone'},'state':{'Name':'riftborn:abyssal_crystal_ore'}}]}})
    write_json(D+'worldgen/placed_feature/awakening/abyss_ore.json',{'feature':'riftborn:awakening/abyss_ore','placement':[{'type':'minecraft:count','count':8},{'type':'minecraft:in_square'},{'type':'minecraft:height_range','height':{'type':'minecraft:uniform','min_inclusive':{'absolute':40},'max_inclusive':{'absolute':310}}},{'type':'minecraft:biome'}]})
    surface=[{'type':'minecraft:count','count':3},{'type':'minecraft:in_square'},{'type':'minecraft:heightmap','heightmap':'WORLD_SURFACE_WG'},{'type':'minecraft:biome'}]
    write_json(D+'worldgen/configured_feature/awakening/crystal_spires.json',{'type':'minecraft:block_column','config':{'direction':'up','allowed_placement':{'type':'minecraft:matching_blocks','blocks':['minecraft:air']},'prioritize_tip':True,'layers':[{'height':{'type':'minecraft:uniform','min_inclusive':3,'max_inclusive':12},'provider':{'type':'minecraft:simple_state_provider','state':{'Name':'riftborn:luminous_shale'}}},{'height':1,'provider':{'type':'minecraft:simple_state_provider','state':{'Name':'riftborn:abyssal_crystal_ore'}}}]}})
    write_json(D+'worldgen/placed_feature/awakening/crystal_spires.json',{'feature':'riftborn:awakening/crystal_spires','placement':surface[:3]+[{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:matching_blocks','offset':[0,-1,0],'blocks':['riftborn:abyssal_stone','riftborn:luminous_shale']}}]+surface[3:]})
    for feature,block in [('echo_ferns','echo_fern'),('ashen_reeds','ashen_reeds'),('brambles','abyssal_bramble')]:
        placed={'feature':{'type':'minecraft:simple_block','config':{'to_place':{'type':'minecraft:simple_state_provider','state':{'Name':'riftborn:'+block}}}},'placement':[{'type':'minecraft:block_predicate_filter','predicate':{'type':'minecraft:all_of','predicates':[{'type':'minecraft:matching_blocks','blocks':['minecraft:air']},{'type':'minecraft:would_survive','state':{'Name':'riftborn:'+block}}]}}]}
        write_json(D+'worldgen/configured_feature/awakening/'+feature+'.json',{'type':'minecraft:random_patch','config':{'tries':24,'xz_spread':6,'y_spread':2,'feature':placed}})
        write_json(D+'worldgen/placed_feature/awakening/'+feature+'.json',{'feature':'riftborn:awakening/'+feature,'placement':surface})
    for name,(width,height,realm,spacing,separation,salt,circular,verse) in STRUCTURES.items():
        path='awakening/'+name
        write_json(D+'worldgen/structure/'+path+'.json',{'type':'riftborn:rift_surface','biomes':'#riftborn:has_structure/'+path,'step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin','start_pool':'riftborn:'+path,'size':1,'surface_search_radius':32 if width>=65 else 64,'circular_footprint':circular})
        placement={'type':'minecraft:random_spread','salt':salt,'spacing':spacing,'separation':separation,'spread_type':'linear'}
        if realm=='rift':placement['exclusion_zone']={'other_set':'riftborn:rift_ruin','chunk_count':3}
        write_json(D+'worldgen/structure_set/'+path+'.json',{'structures':[{'structure':'riftborn:'+path,'weight':1}],'placement':placement})
        write_json(D+'worldgen/template_pool/'+path+'.json',{'name':'riftborn:'+path,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':'riftborn:'+path,'processors':'minecraft:empty','projection':'rigid'}}]})
        write_json(D+'tags/worldgen/biome/has_structure/'+path+'.json',{'replace':False,'values':['riftborn:void_reaches' if realm=='rift' else 'riftborn:abyssal_expanse']})
    targets={'gateway':['rift_citadel','broken_temple'],'colossus':['colossus_arena'],'architect':['architect_spire'],'sovereign':['sovereign_arena'],'fortress':['abyssal_fortress'],'laboratory':['forgotten_laboratory'],'observatory':['storm_observatory']}
    for tag,names in targets.items():write_json(D+'tags/worldgen/structure/awakening/'+tag+'.json',{'replace':False,'values':['riftborn:awakening/'+name for name in names]})

def advancements():
    nodes=[('guardian_fallen','rift_heart','Guardian Fallen','The Guardian was a lock. Something below has heard it fall.','riftborn:guardian'),('three_voices','resonant_sigil','Three Voices','Resolve the Broken Temple\'s resonance lock.','guardian_fallen'),('rift_ascension','rift_chestplate','Rift Ascension','Obtain a complete set of Rift Armor.','guardian_fallen'),('the_abyss','abyssal_key','The Abyss','Open a path to the origin of the Rift.','three_voices'),('deeper_than_darkness','ancient_stele','Deeper Than Darkness','Read an ancient inscription and discover what it guarded.','the_abyss'),('abyssal_ascension','abyssal_chestplate','Abyssal Ascension','Upgrade the complete armor set at a smithing table.','the_abyss'),('colossal','titan_core','Colossal','Defeat the Abyssal Colossus.','abyssal_ascension'),('reality_breaker','reality_spindle','Reality Breaker','Defeat the Rift Architect.','colossal'),('the_collapse','sovereign_sigil','The Collapse','Survive the Herald and claim its sovereign sigil.','reality_breaker'),('sovereign_slayer','sovereign_heart','Sovereign Slayer','Defeat all three phases of the Abyss Sovereign.','the_collapse'),('rift_master','heart_of_the_rift','Rift Master','Complete the major Riftborn progression.','sovereign_slayer')]
    for name,icon,title,description,parent in nodes:
        condition={'awakened':{'trigger':'minecraft:impossible'}}
        if name in ['rift_ascension','abyssal_ascension']:
            pieces=['rift_'+slot for slot in ['helmet','chestplate','leggings','boots']] if name=='rift_ascension' else ARMOR
            condition={'awakened':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'riftborn:'+p} for p in pieces]}}}
        write_json(D+'advancement/awakening/'+name+'.json',{'parent':parent if ':' in parent else 'riftborn:awakening/'+parent,'display':{'icon':{'id':'riftborn:'+icon},'title':title,'description':description,'frame':'challenge' if name in ['colossal','reality_breaker','sovereign_slayer','rift_master'] else 'task','show_toast':True,'announce_to_chat':True,'hidden':False},'criteria':condition,'requirements':[['awakened']]})

def language():
    path=RES/(A+'lang/en_us.json');v=json.loads(path.read_text())
    for name in ITEMS+BLOCKS:v[('block.' if name in BLOCKS else 'item.')+'riftborn.'+name]=name.replace('_',' ').title()
    for name in MOBS:v['entity.riftborn.'+name]=name.replace('_',' ').title();v['item.riftborn.'+name+'_spawn_egg']=name.replace('_',' ').title()+' Spawn Egg'
    v['entity.riftborn.abyssal_colossus']='The Abyssal Colossus';v['entity.riftborn.rift_architect']='The Rift Architect';v['entity.riftborn.abyss_sovereign']='The Abyss Sovereign';v['entity.riftborn.abyss_bolt']='Abyss Bolt';v['item.riftborn.warding_anchor']='Warding Rift Anchor'
    tips={
        'abyssal_shard':'Harvested from Abyssal crystal ore. Voidbow ammunition; refine four with Rift Dust.',
        'abyssal_crystal':'The crystal remembers light. Used in Abyssal gear and repaired with its own kind.',
        'abyssal_essence':'Life distilled from Abyss creatures. Fuels every Rift Staff spell.',
        'abyssal_core':'Four Crystals, four Essences, one Rift Core. The foundation of Abyssal equipment.',
        'resonant_sigil':'Solve the three-voice lock in a Broken Temple. Its glyph completes an Abyssal Key.',
        'abyssal_key':'Use a Rift gateway to enter The Abyss. Use in the air to locate a gateway. First attunement consumes one key.',
        'abyssal_schematic':'Found in Forgotten Laboratories. Smith Rift Armor with this schematic and an Abyssal Core; enchantments are retained.',
        'stormglass':'Defeat storm-touched enemies or explore an observatory. Forms movement and warding artifacts.',
        'titan_core':'The Colossus\'s exclusive core. Opens the Architect\'s altar and forges the Greatblade.',
        'reality_spindle':'The Architect\'s exclusive spindle. Shapes a Rift Staff or a Collapse Catalyst.',
        'collapse_catalyst':'Offer at a Sovereign altar after defeating the Colossus and Architect to call The Collapse.',
        'sovereign_sigil':'The Collapse Herald\'s exclusive seal. Offer it at a Sovereign altar.',
        'sovereign_heart':'The final Sovereign\'s heart. Forge the Heart of the Rift or a Sovereign Standard.',
        'abyssal_greatblade':'15 melee damage, 0.9 attack speed. Hold Use for one second, then release a devastating frontal cleave.',
        'voidbow':'Charge to fire an Abyss bolt. Consumes one Abyssal Shard. Power, Punch and Flame are supported.',
        'rift_staff':'Sneak-use to cycle spells. Use consumes 1 / 2 / 3 Abyssal Essence, with separate spell power and shared cooldown.',
        'heart_of_the_rift':'Offhand: reduces new weapon and Rift Dash cooldowns by 20%. Does not change the original Riftblade.',
        'abyssal_eye':'Use to locate Abyss landmarks; sneak-use cycles targets. Offhand: Night Vision in the realms. Storms reveal observatories.',
        'void_core':'Offhand: increases armor flight speed by 12%. Trade your shield for mobility.',
        'warding_anchor':'Offhand: while grounded and still, reduces a hit by 25% (up to 3 damage), once per ten seconds.'}
    for name,tip in tips.items():v['item.riftborn.'+name+'.tooltip']=tip
    v.update({
      'itemGroup.riftborn.awakening':'Riftborn — The Rift Awakening','key.riftborn.dash':'Rift Dash','category.riftborn':'Riftborn',
      'awakening.riftborn.guardian_secret':'The Guardian falls. Far below, a second heartbeat answers. Seek the Broken Temple.',
      'awakening.riftborn.lock_hint':'The inscription reads: Dusk, Crown, Tide. Tune the left, rear, and right resonators to one, three, and two.',
      'awakening.riftborn.lock_open':'The lock remembers the song. Offer a Rift Core to create another Resonant Sigil.',
      'awakening.riftborn.artifact_slot':'Only the artifact in your OFFHAND is active. Choose one, or keep your shield.',
      'awakening.riftborn.ascension':'Full set: Abyssal Ascension — faster flight, limited Aegis defense, and Rift Dash.',
      'awakening.riftborn.dash_hint':'Press the bound Rift Dash key (default R). Aerial dashes need safe terrain within 24 blocks below.',
      'awakening.riftborn.dash_blocked':'No safe dash landing. Move closer to supported terrain; the dash will not carry you into open void.',
      'awakening.riftborn.gate_wrong_world':'Abyss gateways connect only The Rift and The Abyss.',
      'awakening.riftborn.gate_settling':'Dismount and let the gateway settle before crossing again.',
      'awakening.riftborn.gate_key':'An Abyssal Key is required for your first crossing. Solve the temple and forge the key.',
      'awakening.riftborn.gate_missing':'The Abyss is unavailable. Ask the server owner to check its datapack.',
      'awakening.riftborn.gate_blocked':'The destination is obstructed. Clear room beside the gateway.',
      'awakening.riftborn.gate_arrive':'The Abyss. This is where the fracture began. Use a gateway to return to your own Rift entrance.',
      'awakening.riftborn.gate_return':'The deeper heartbeat recedes. You have returned from The Abyss.',
      'awakening.riftborn.locator_world':'This locator answers within The Rift or The Abyss.',
      'awakening.riftborn.observatory_veiled':'The observatory signal is veiled. A Rift Storm or Collapse may reveal it.',
      'awakening.riftborn.locator_none':'No %s signal in range. Explore farther.',
      'awakening.riftborn.locator_found':'%s signal: X %s, Z %s — approximately %s blocks away.',
      'awakening.riftborn.altar_hint_0':'Offer an Abyssal Core. The Colossus\'s ground waves can be jumped; keep moving under its barrage.',
      'awakening.riftborn.altar_hint_1':'Offer a Titan Core after defeating the Colossus. The Architect\'s temporary walls can be broken.',
      'awakening.riftborn.altar_hint_2':'After the Architect, offer a Collapse Catalyst here. Defeat its Herald, then offer the Sovereign Sigil.',
      'awakening.riftborn.altar_world':'These ancient altars answer only in The Abyss.',
      'awakening.riftborn.altar_peaceful':'The ancient presence sleeps in Peaceful difficulty.',
      'awakening.riftborn.altar_progress':'Your journey has not reached this seal. Defeat the preceding guardian of The Abyss first.',
      'awakening.riftborn.altar_busy':'An Abyss encounter is already awake nearby.',
      'awakening.riftborn.altar_space':'Clear the marked summoning space in front of the altar.',
      'awakening.riftborn.bow_ammo':'The Voidbow needs an Abyssal Shard.',
      'awakening.riftborn.staff_ammo':'This spell needs %s Abyssal Essence.',
      'spell.riftborn.0':'Rift Staff: Lance — 1 Essence, a focused energy bolt.',
      'spell.riftborn.1':'Rift Staff: Repulse — 2 Essence, a close-range shockwave.',
      'spell.riftborn.2':'Rift Staff: Mend — 3 Essence, brief regeneration for you and nearby teammates.',
      'awakening.riftborn.storm_begins':'A Rift Storm rises. Storm-touched enemies carry Stormglass; veiled signals become visible.',
      'awakening.riftborn.collapse_begins':'THE COLLAPSE — Reality thins. Defeat the Herald and claim its Sovereign Sigil.',
      'awakening.riftborn.collapse_requirements':'Defeat the Colossus and Architect, and wait until no realm event is active.',
      'awakening.riftborn.collapse_won':'The Collapse recedes. The Herald\'s sigil can open the final seal.',
      'boss.riftborn.abyss_warning':'%s — Fracture incoming!',
      'boss.riftborn.last_silence':'THE ABYSS SOVEREIGN — LAST SILENCE',
      'boss.riftborn.last_silence_hint':'Last Silence: reach the small inner ring of light. You have five seconds.',
      'hud.riftborn.dash_ready':'Rift Dash — ready','hud.riftborn.dash_cooling':'Rift Dash — recharging',
      'hud.riftborn.storm':'RIFT STORM · %ss','hud.riftborn.collapse':'THE COLLAPSE · %ss'})
    for key,title in {'gateway':'Abyss Gateway','colossus':'Colossus Arena','architect':'Architect Spire','sovereign':'Sovereign Arena','fortress':'Abyssal Fortress','laboratory':'Forgotten Laboratory','observatory':'Storm Observatory'}.items():v['location.riftborn.'+key]=title
    verses=[
      'The Guardian was no conqueror. Its heartbeat held the first gate shut. Now the dark below remembers us.',
      'Three voices open the memory: Dusk, Crown, Tide. One mark, three marks, two marks. Face the inscription and listen.',
      'We built our citadels above the wound. Their gateways lead not to another world, but to the beginning of this one.',
      'Laboratory note: an Abyssal Core binds to Rift plate. Keep the old enchantments; replace the material. Duplicate the schematic carefully.',
      'The Colossus bears the foundation. Claim its Titan Core to turn the Architect\'s lock. Ground waves spare those who leave the ground.',
      'The Architect borrowed walls from tomorrow. Break them, or wait for them to fade. Titan and Spindle together can call The Collapse.',
      'The Sovereign has three faces. When the Last Silence gathers, flee inward, not away. The smallest ring is the eye of the storm.',
      'Stormglass catches the veiled signals. During a storm, the Eye sees observatories. The Collapse Herald carries the final seal.']
    for i,line in enumerate(verses):v['lore.riftborn.verse_'+str(i)]=line
    write_json(A+'lang/en_us.json',v)
if __name__=='__main__':
    recipes();loot();worldgen();advancements();language();print('Generated additive Awakening data, progression and translations.')
