"""2.0 scenarios added after the complete 1.5 multiplayer regression sequence."""
REQUIRED={
'RIFTBORN_20_ENTRY_LOCK_OK','RIFTBORN_20_PUZZLE_OK','RIFTBORN_20_ABYSS_ENTRY_OK','RIFTBORN_20_ABYSS_RENDER_OK','RIFTBORN_20_DASH_OK',
'RIFTBORN_20_GREATBLADE_OK','RIFTBORN_20_VOIDBOW_OK','RIFTBORN_20_STAFF_LANCE_OK','RIFTBORN_20_STAFF_PULSE_OK','RIFTBORN_20_STAFF_MEND_OK',
'RIFTBORN_20_ARTIFACT_OK','RIFTBORN_20_STORM_OK','RIFTBORN_20_FINAL_BOSS_OK','RIFTBORN_20_ABYSS_RETURN_OK','RIFTBORN_20_ATTUNEMENT_OK','RIFTBORN_20_CLIENT_OK'}
class AwakeningScenario:
    def __init__(self,command):self.command=command;self.boss=0;self.structures=set();self.bosses=set();self.terrain=False;self.collapse=False;self.progress=False
    def cmd(self,*commands):
        for command in commands:self.command(command)
    def hand(self,item):self.cmd('item replace entity RiftbornTester weapon.mainhand with '+item)
    def armor(self,tier):
        for slot,name in [('head','helmet'),('chest','chestplate'),('legs','leggings'),('feet','boots')]:self.cmd(f'item replace entity RiftbornTester armor.{slot} with riftborn:{tier}_{name}')
    def target(self,x):self.cmd(f'execute in riftborn:the_abyss run tp @e[tag=awakening_weapon,limit=1] {x} 325 0.5')
    def handle(self,source,line):
        if source=='server':
            if 'RIFTBORN_20_STRUCTURE_OK ' in line:self.structures.add(line.split('RIFTBORN_20_STRUCTURE_OK ')[1].split()[0])
            if 'RIFTBORN_20_TERRAIN_OK' in line:self.terrain=True
            if 'RIFTBORN_20_BOSS_OK kind=' in line:self.bosses.add(int(line.split('kind=')[1].split()[0]))
            if 'RIFTBORN_20_COLLAPSE_OK' in line:self.collapse=True
            if 'RIFTBORN_20_SERVER_PROGRESSION_OK' in line:self.progress=True
            if 'RIFTBORN_20_WORLD_AUDIT_OK' in line:
                self.cmd('execute in riftborn:the_rift run setblock 16 141 4 riftborn:abyss_gate', 'execute in riftborn:the_rift run tp RiftbornTester 16.5 141 6.5 180 0','clear RiftbornTester')
                self.armor('rift')
                self.cmd('execute in riftborn:the_rift run damage @e[type=riftborn:rift_guardian,limit=1] 10000 minecraft:generic_kill by RiftbornTester')
            return
        if source!='client':return
        if 'RIFTBORN_20_BEGIN' in line:self.cmd('riftborn_awake audit')
        if 'RIFTBORN_20_ENTRY_LOCK_OK' in line:
            self.cmd('execute in riftborn:the_rift run setblock 16 141 12 riftborn:resonance_lock[open=false]', 'execute in riftborn:the_rift run setblock 13 141 12 riftborn:resonator[note=1]', 'execute in riftborn:the_rift run setblock 16 141 9 riftborn:resonator[note=3]', 'execute in riftborn:the_rift run setblock 19 141 12 riftborn:resonator[note=2]', 'execute in riftborn:the_rift run tp RiftbornTester 16.5 141 15.5 180 0')
        if 'RIFTBORN_20_PUZZLE_OK' in line:self.cmd('riftborn_awake key','execute in riftborn:the_rift run tp RiftbornTester 16.5 141 6.5 180 0')
        if 'RIFTBORN_20_ABYSS_ENTRY_OK' in line:
            self.cmd('riftborn_awake entry','execute in riftborn:the_abyss run forceload add -32 -32 48 48','execute in riftborn:the_abyss run fill -24 324 -24 24 324 24 riftborn:abyssal_stone','execute in riftborn:the_abyss run setblock 0 325 -5 riftborn:abyss_gate[active=true]','execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 -5.5 0 -6','effect give RiftbornTester minecraft:resistance 999 4 true')
            for x,mob,y in [(-14,'abyss_stalker',325),(-7,'void_reaver',328),(0,'abyssal_brute',325),(7,'rift_echo',325),(14,'abyssal_warden',325)]:self.cmd(f'execute in riftborn:the_abyss run summon riftborn:{mob} {x} {y} 12 {{NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["awakening_display"]}}')
            self.armor('abyssal')
        if 'RIFTBORN_20_DASH_VERIFIED' in line:self.cmd('riftborn_awake dash')
        if 'RIFTBORN_20_VISTA_REQUEST' in line:self.cmd('riftborn_awake vista')
        if 'RIFTBORN_20_DASH_OK' in line:
            self.cmd('execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 0.5 -90 0','execute in riftborn:the_abyss run summon riftborn:abyssal_brute 4.5 325 0.5 {NoAI:1b,Silent:1b,PersistenceRequired:1b,Tags:["awakening_weapon"]}')
            self.hand('riftborn:abyssal_greatblade')
        if 'RIFTBORN_20_GREATBLADE_OK' in line:self.cmd('riftborn_awake weapon','give RiftbornTester riftborn:abyssal_shard 12');self.target(8.5);self.hand('riftborn:voidbow')
        if 'RIFTBORN_20_VOIDBOW_OK' in line:self.cmd('riftborn_awake weapon','give RiftbornTester riftborn:abyssal_essence 24');self.hand('riftborn:rift_staff')
        if 'RIFTBORN_20_STAFF_LANCE_OK' in line:self.cmd('riftborn_awake weapon');self.target(4.5)
        if 'RIFTBORN_20_STAFF_PULSE_OK' in line:self.cmd('riftborn_awake weapon','riftborn_awake heal_setup')
        if 'RIFTBORN_20_STAFF_MEND_OK' in line:self.cmd('riftborn_awake heal','item replace entity RiftbornTester weapon.offhand with riftborn:void_core')
        if 'RIFTBORN_20_ARTIFACT_OK' in line:
            self.cmd('riftborn_awake artifact','item replace entity RiftbornTester weapon.offhand with minecraft:air','execute in riftborn:the_abyss run kill @e[tag=awakening_display]','execute in riftborn:the_abyss run kill @e[tag=awakening_weapon]','execute in riftborn:the_abyss run setblock 0 325 8 riftborn:colossus_altar','execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 5.5 0 0')
            self.hand('riftborn:abyssal_core')
        if 'RIFTBORN_20_BOSS_SEEN' in line:self.cmd('riftborn_awake freeze')
        if 'RIFTBORN_20_SOVEREIGN_TRANSFORM' in line:self.cmd('riftborn_awake phase3')
        if 'RIFTBORN_20_BOSS_COMBAT' in line:self.cmd('execute in riftborn:the_abyss run tp RiftbornTester 0.5 334 5.5 0 25','riftborn_awake combat')
        if 'RIFTBORN_20_BOSS_DEFEAT_REQUEST' in line:
            self.cmd('riftborn_awake defeat')
            if self.boss==0:
                self.cmd('execute in riftborn:the_abyss run setblock 0 325 8 riftborn:architect_altar','execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 5.5 0 0');self.hand('riftborn:titan_core')
            elif self.boss==1:
                self.cmd('execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 5.5 0 -8','riftborn_awake storm')
            self.boss+=1
        if 'RIFTBORN_20_STORM_OK' in line:
            self.cmd('riftborn_awake storm_check','execute in riftborn:the_abyss run setblock 0 325 8 riftborn:sovereign_altar','execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 5.5 0 0');self.hand('riftborn:collapse_catalyst')
        if 'RIFTBORN_20_HERALD_DEFEAT_REQUEST' in line:self.cmd('riftborn_awake herald','execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 5.5 0 0');self.hand('riftborn:sovereign_sigil')
        if 'RIFTBORN_20_FINAL_BOSS_OK' in line:self.cmd('riftborn_awake finish','execute in riftborn:the_abyss run tp RiftbornTester 0.5 325 -2.5 180 0');self.hand('minecraft:air')
    def verify(self):
        if len(self.structures)!=10 or not self.terrain or self.bosses!={0,1,2} or not self.collapse or not self.progress:raise RuntimeError(f'Awakening server assertions incomplete: structures={self.structures},bosses={self.bosses},terrain={self.terrain},collapse={self.collapse},progress={self.progress}')
