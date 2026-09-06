#!/usr/bin/env python3
"""Original, coherent obsidian/cyan/antique-gold Awakening art; no external assets."""
import math,random
from resource_tools import Image,write_json
from awakening_content import *
A='assets/riftborn/'
DARK=(12,24,33); METAL=(32,55,66); CYAN=(94,236,224); GOLD=(223,177,102); LIGHT=(199,255,243); VIOLET=(126,85,173)
def noise_tile(seed,glow=False):
    r=random.Random(seed);im=Image(32,32)
    for y in range(32):
        for x in range(32):
            n=r.randrange(-6,7);seam=8 if x%16==0 or y%16==0 else 0
            im.pixel(x,y,tuple(max(0,c+n-seam) for c in DARK))
    for i in range(5):
        x=r.randrange(2,28);y=r.randrange(2,28);im.line(x,y,min(31,x+5),max(0,y-3),tuple(int(c*.35) for c in CYAN),2)
        if glow:im.line(x,y,min(31,x+5),max(0,y-3),CYAN)
    return im
def rune(im,cx,cy,radius,color):
    im.line(cx-radius,cy,cx,cy-radius,color);im.line(cx,cy-radius,cx+radius,cy,color);im.line(cx+radius,cy,cx,cy+radius,color);im.line(cx,cy+radius,cx-radius,cy,color)
    im.line(cx,cy-radius+2,cx,cy+radius-2,LIGHT)
def blocks():
    for i,name in enumerate(BLOCKS):
        if name in PLANTS:continue
        im=noise_tile(2100+i,name in ['luminous_shale','abyssal_crystal_block','phase_barrier'])
        if name=='abyssal_crystal_ore':
            for x,y in [(6,5),(20,9),(12,23),(27,25)]:rune(im,x,y,3,CYAN)
        elif name not in ['abyssal_stone','luminous_shale']:
            im.rect(0,0,31,1,GOLD);im.rect(0,30,31,31,tuple(c//2 for c in GOLD));rune(im,16,16,10,GOLD if 'altar' in name or name=='sovereign_standard' else CYAN)
            im.rect(13,13,18,18,LIGHT if name=='sovereign_standard' else METAL)
        if name=='phase_barrier':
            for y in range(32):
                for x in range(32):
                    im.pixel(x,y,(*CYAN,190 if x in [0,1,30,31] or y in [0,1,30,31] or (x+y)%12==0 else 45))
        im.save(A+'textures/block/'+name+'.png')
        write_json(A+'models/block/'+name+'.json',{'parent':'minecraft:block/cube_all','textures':{'all':'riftborn:block/'+name}})
        if name=='resonator':
            variants={}
            for note in range(4):
                image=noise_tile(2201,True);image.rect(2,2,29,29,METAL)
                for j in range(note):image.rect(7+j*7,8,10+j*7,24,GOLD)
                image.save(A+f'textures/block/resonator_{note}.png');write_json(A+f'models/block/resonator_{note}.json',{'parent':'minecraft:block/cube_all','textures':{'all':f'riftborn:block/resonator_{note}'}});variants[f'note={note}']={'model':f'riftborn:block/resonator_{note}'}
        elif name=='ancient_stele':variants={f'verse={n}':{'model':'riftborn:block/'+name} for n in range(8)}
        elif name=='abyss_gate':variants={f'active={v}':{'model':'riftborn:block/'+name} for v in ['true','false']}
        elif name=='resonance_lock':variants={f'open={v}':{'model':'riftborn:block/'+name} for v in ['true','false']}
        else:variants={'':{'model':'riftborn:block/'+name}}
        write_json(A+'blockstates/'+name+'.json',{'variants':variants});write_json(A+'models/item/'+name+'.json',{'parent':'riftborn:block/'+name})
    for i,name in enumerate(PLANTS):
        im=Image(32,32);im.line(16,7,16,31,METAL,2)
        for j in range(5):
            y=10+j*4;span=9-j
            im.line(16,y,16-span,y-5,CYAN if i<2 else GOLD,2);im.line(17,y,17+span,y-4,CYAN if i<2 else VIOLET,2)
        rune(im,16,7,4,LIGHT if i==0 else GOLD);im.save(A+'textures/block/'+name+'.png')
        write_json(A+'models/block/'+name+'.json',{'parent':'minecraft:block/cross','textures':{'cross':'riftborn:block/'+name}});write_json(A+'blockstates/'+name+'.json',{'variants':{'':{'model':'riftborn:block/'+name}}});write_json(A+'models/item/'+name+'.json',{'parent':'minecraft:item/generated','textures':{'layer0':'riftborn:block/'+name}})

def items():
    for i,name in enumerate(ITEMS):
        im=Image(32,32)
        if name in ARMOR:
            if 'helmet' in name:
                im.rect(5,5,26,25,METAL);im.rect(7,7,24,22,DARK);im.rect(9,14,22,24,(0,0,0,0));im.line(6,11,25,11,GOLD,2);rune(im,16,7,3,CYAN)
                im.line(4,3,6,10,GOLD,2);im.line(27,3,25,10,GOLD,2)
            elif 'chestplate' in name:
                im.rect(7,6,24,27,METAL);im.rect(2,5,8,14,GOLD);im.rect(23,5,29,14,GOLD);im.rect(12,4,19,9,(0,0,0,0));rune(im,16,18,7,CYAN);im.line(8,26,23,26,GOLD,2)
            elif 'leggings' in name:
                im.rect(6,4,25,12,METAL);im.rect(6,12,13,28,DARK);im.rect(18,12,25,28,DARK);im.line(6,6,25,6,GOLD,2);im.line(8,12,8,26,CYAN,2);im.line(22,12,22,26,CYAN,2);rune(im,16,8,3,LIGHT)
            else:
                for x in [5,19]:im.rect(x,5,x+7,24,METAL);im.rect(x-2,22,x+8,28,DARK);im.line(x,7,x+6,7,GOLD,2);rune(im,x+3,20,3,CYAN)
        elif name=='abyssal_greatblade':
            im.line(5,26,10,21,GOLD,4);im.line(11,20,27,4,DARK,6);im.line(13,17,28,2,GOLD,4);im.line(14,16,28,2,CYAN,2);im.line(15,16,28,3,LIGHT);im.line(6,18,16,28,METAL,3);rune(im,11,23,3,CYAN)
        elif name=='rift_staff':
            im.line(6,27,22,10,METAL,3);im.line(7,26,22,11,GOLD);rune(im,23,8,6,GOLD);rune(im,23,8,3,CYAN);im.line(17,5,20,1,VIOLET,2)
        elif name=='voidbow':
            for y in range(3,29):
                x=8+int(math.sin((y-3)*math.pi/25)*13);im.rect(x,y,x+2,y+1,GOLD if y in range(13,20) else METAL)
            im.line(8,3,8,28,CYAN);rune(im,22,16,4,LIGHT)
        elif name=='abyssal_schematic':
            im.rect(4,3,27,28,GOLD);im.rect(6,5,25,26,DARK)
            for y in [8,13,18,23]:im.line(8,y,22,y,CYAN)
            rune(im,16,15,7,LIGHT)
        elif name=='abyssal_key':
            rune(im,11,10,8,GOLD);rune(im,11,10,4,CYAN);im.line(15,15,27,28,GOLD,3);im.rect(22,20,27,22,LIGHT);im.rect(26,25,29,27,CYAN)
        elif name in ['abyssal_shard','abyssal_crystal','abyssal_essence','stormglass']:
            for y in range(3,29):
                half=min((y-2)*.7,(29-y)*.65,9)
                for x in range(3,29):
                    if abs(x-16)<=half:im.pixel(x,y,CYAN if x<15 else METAL if x>19 else LIGHT)
            im.line(13,8,10,18,LIGHT,2)
            if name in ['abyssal_essence','stormglass']:rune(im,16,17,9,GOLD)
        else:
            for y in range(32):
                for x in range(32):
                    radius=math.hypot(x-15.5,y-15.5)
                    if radius<12:im.pixel(x,y,GOLD if radius>10 else DARK if radius>7 else CYAN if radius>5 else METAL)
            rune(im,16,16,8,GOLD if 'sovereign' in name else CYAN)
            if name in ['abyssal_eye','heart_of_the_rift']:im.rect(13,10,18,22,LIGHT);im.rect(15,12,16,20,DARK)
            if name=='reality_spindle':im.line(5,5,27,27,LIGHT,2)
        im.save(A+'textures/item/'+name+'.png')
        parent='minecraft:item/handheld' if name in ['abyssal_greatblade','rift_staff'] else 'minecraft:item/generated'
        model={'parent':parent,'textures':{'layer0':'riftborn:item/'+name}}
        if name=='voidbow':
            model['parent']='minecraft:item/bow';model['overrides']=[{'predicate':{'riftborn:pulling':1},'model':'riftborn:item/voidbow_pulling_0'},{'predicate':{'riftborn:pulling':1,'riftborn:pull':.65},'model':'riftborn:item/voidbow_pulling_1'},{'predicate':{'riftborn:pulling':1,'riftborn:pull':.9},'model':'riftborn:item/voidbow_pulling_2'}]
            for frame in range(3):
                arrow=Image(32,32)
                for y,row in enumerate(im.pixels):
                    for x,p in enumerate(row):arrow.pixel(x,y,p)
                arrow.line(3,16,28,16,CYAN,1+frame);rune(arrow,28,16,2+frame,GOLD)
                arrow.save(A+f'textures/item/voidbow_pulling_{frame}.png');write_json(A+f'models/item/voidbow_pulling_{frame}.json',{'parent':'minecraft:item/bow','textures':{'layer0':f'riftborn:item/voidbow_pulling_{frame}'}})
        write_json(A+'models/item/'+name+'.json',model)
    for name in MOBS:write_json(A+'models/item/'+name+'_spawn_egg.json',{'parent':'minecraft:item/template_spawn_egg'})

def armor():
    for layer in [1,2]:
        im=Image(64,32);r=random.Random(2300+layer)
        regions=[(0,0,31,15),(16,16,39,31),(40,16,55,31),(0,25,15,31)] if layer==1 else [(0,16,15,31),(16,28,39,31)]
        for x0,y0,x1,y1 in regions:
            for y in range(y0,y1+1):
                for x in range(x0,x1+1):n=r.randrange(-4,5);im.pixel(x,y,tuple(c+n for c in DARK))
        if layer==1:
            im.rect(9,11,14,14,(0,0,0,0));im.line(8,10,15,10,GOLD);im.rect(11,8,12,9,CYAN)
            rune(im,24,26,5,GOLD);im.rect(23,23,24,28,LIGHT);im.line(40,20,55,20,GOLD);im.line(40,30,55,30,CYAN);im.line(0,26,15,26,GOLD);im.rect(5,28,6,30,CYAN)
        else:im.line(16,29,39,29,GOLD);im.rect(23,29,24,30,CYAN);im.line(4,20,4,30,GOLD);im.line(7,20,7,30,GOLD);im.rect(5,23,6,27,CYAN)
        im.save(A+f'textures/models/armor/abyssal_layer_{layer}.png')

def entities():
    for i,name in enumerate(MOBS):
        im=Image(128,128);r=random.Random(2400+i)
        base=(18,41,49) if name not in ['rift_echo','rift_architect'] else (39,32,64)
        for y in range(128):
            for x in range(128):
                n=r.randrange(-5,6);color=tuple(c+n for c in base)
                if (x+2*y)%37==0:color=tuple(c//3 for c in CYAN)
                im.pixel(x,y,color)
        # Dedicated UV glyph regions, eyes, articulated joints, and antique-metal trim.
        for y in [4,31,63,89,119]:im.line(0,y,127,y,GOLD)
        for x,y in [(15,16),(35,50),(78,44),(101,104)]:rune(im,x,y,7,CYAN if i%2==0 else GOLD)
        im.rect(11,12,13,15,LIGHT);im.rect(17,12,19,15,LIGHT)
        im.rect(96,0,115,27,CYAN);im.rect(112,16,123,30,GOLD);im.rect(100,100,115,119,LIGHT)
        im.save(A+'textures/entity/awakening/'+name+'.png')
    eyes=Image(128,128);eyes.rect(11,12,13,15,LIGHT);eyes.rect(17,12,19,15,LIGHT);eyes.save(A+'textures/entity/awakening/abyss_stalker_eyes.png')
    im=Image(128,128)
    for y in range(128):
        for x in range(128):im.pixel(x,y,(218,222,207) if (x+y)%11<2 else (37,42,53))
    for x,y in [(15,16),(35,50),(78,44),(101,104)]:rune(im,x,y,8,GOLD)
    im.rect(96,0,123,30,LIGHT);im.save(A+'textures/entity/awakening/abyss_sovereign_awakened.png')

def skies():
    im=Image(256,256);r=random.Random(2600)
    for y in range(256):
        for x in range(256):
            wave=math.sin(x*math.tau/256+math.cos(y*math.tau/256))*.5+math.cos((x-y)*math.tau/128)*.2
            im.pixel(x,y,(int(7+wave*4),int(20+wave*7),int(31+wave*9)))
    for i in range(180):x,y=r.randrange(256),r.randrange(256);im.pixel(x,y,r.choice([CYAN,GOLD,LIGHT]));
    im.save(A+'textures/environment/abyss_sky.png')
    planet=Image(512,512)
    for y in range(512):
        for x in range(512):
            dx=(x-256)/150;dy=(y-250)/150;r2=dx*dx+dy*dy;ring=math.sqrt(((x-256)/225)**2+((y-275+(x-256)*.22)/52)**2)
            if .86<ring<1.0:planet.pixel(x,y,(*GOLD,int(230*(1-abs(.93-ring)/.08))))
            if r2<1:
                light=max(.08,(-dx*.7-dy*.45+math.sqrt(1-r2)*.35));cloud=math.sin(dy*25+math.cos(dx*11))*math.cos(dx*17)*.2
                planet.pixel(x,y,(int(20+35*light),int(45+100*light+cloud*10),int(64+100*light+cloud*10)))
                if abs(dx*.7+dy*.3+.2)<.018:planet.pixel(x,y,LIGHT)
    planet.save(A+'textures/environment/abyss_planet.png')
    storm=Image(128,128)
    for y in range(128):
        for x in range(128):
            wave=math.sin(x*.09+math.sin(y*.11))*math.cos(y*.08);storm.pixel(x,y,(int(20+12*wave),int(39+22*wave),int(60+35*wave)))
    for coords in [(5,3,40,50),(40,50,32,78),(32,78,70,123),(100,0,83,55)]:storm.line(*coords,GOLD,1)
    storm.save(A+'textures/environment/abyss_storm.png')
    for name,rune_shape in [('abyss_mote',False),('abyss_rune',True)]:
        image=Image(16,16)
        if rune_shape:rune(image,8,8,6,(255,255,255))
        else:
            for y in range(16):
                for x in range(16):
                    d=math.hypot(x-7.5,y-7.5)
                    if d<6:image.pixel(x,y,(255,255,255,int(220*(1-d/6))))
        image.save(A+'textures/particle/'+name+'.png');write_json(A+'particles/'+name+'.json',{'textures':['riftborn:'+name]})
if __name__=='__main__':
    blocks();items();armor();entities();skies();print('Generated coherent Awakening item, block, armor, entity, particle and celestial art.')
