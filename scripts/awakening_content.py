"""Shared, explicit 2.0 content inventory for generators and integrity tests."""
MATERIALS = ['abyssal_shard','abyssal_crystal','abyssal_essence','abyssal_core','resonant_sigil','stormglass','titan_core','reality_spindle','sovereign_sigil','sovereign_heart','abyssal_schematic','collapse_catalyst']
ARMOR=['abyssal_helmet','abyssal_chestplate','abyssal_leggings','abyssal_boots']
WEAPONS=['abyssal_greatblade','voidbow','rift_staff']
ARTIFACTS=['heart_of_the_rift','abyssal_eye','void_core','warding_anchor']
ITEMS=MATERIALS+ARMOR+WEAPONS+ARTIFACTS+['abyssal_key']
BLOCKS=['abyssal_stone','luminous_shale','abyssal_crystal_ore','abyssal_crystal_block','abyss_gate','resonator','resonance_lock','ancient_stele','colossus_altar','architect_altar','sovereign_altar','phase_barrier','echo_fern','ashen_reeds','abyssal_bramble','sovereign_standard']
PLANTS=['echo_fern','ashen_reeds','abyssal_bramble']
MOBS={'abyss_stalker':42,'void_reaver':36,'abyssal_brute':150,'rift_echo':46,'abyssal_warden':90,'abyssal_colossus':520,'rift_architect':650,'abyss_sovereign':1000,'collapse_herald':260}
# name: width, height, dimension, spacing, separation, salt, circular, lore verse
STRUCTURES={
 'rift_citadel':(49,36,'rift',34,12,5200101,False,2),
 'broken_temple':(33,20,'rift',22,8,5200103,False,1),
 'sky_ruins':(25,18,'rift',24,8,5200107,False,0),
 'abyssal_fortress':(65,44,'abyss',28,10,5200111,False,4),
 'forgotten_laboratory':(41,30,'abyss',24,8,5200117,False,3),
 'abyss_sky_ruins':(33,24,'abyss',20,8,5200121,True,2),
 'colossus_arena':(65,22,'abyss',32,12,5200131,True,4),
 'architect_spire':(49,64,'abyss',36,14,5200137,False,5),
 'sovereign_arena':(81,36,'abyss',40,16,5200141,True,6),
 'storm_observatory':(41,48,'abyss',48,18,5200149,True,7),
}
