#!/usr/bin/env python3
"""Ten original, explorable structures. Uses the existing deterministic NBT writer."""
import math,random
from resource_tools import RES
from generate_structures import Template
from awakening_content import STRUCTURES,MOBS

def slab(t,y,x0,z0,x1,z1,block):
    for x in range(x0,x1+1):
        for z in range(z0,z1+1):t.set(x,y,z,block)
def pillar(t,x,z,h,stone,glow,width=2,start=1):
    for y in range(start,h+1):
        for dx in range(width):
            for dz in range(width):t.set(x+dx,y,z+dz,glow if y%7==0 else stone)
    for dx in range(width):
        for dz in range(width):t.set(x+dx,h,z+dz,glow)
def tower(t,x,z,width,height,stone,glow,loot):
    perimeter=[]
    for i in range(1,width-2):perimeter.append((i,1,'east'))
    for i in range(1,width-2):perimeter.append((width-2,i,'south'))
    for i in range(width-2,1,-1):perimeter.append((i,width-2,'west'))
    for i in range(width-2,1,-1):perimeter.append((1,i,'north'))
    well={(a,b) for a,b,_ in perimeter}
    for y in range(1,height+1):
        for dx in range(width):
            for dz in range(width):
                if dx in [0,width-1] or dz in [0,width-1]:
                    if y<4 and dz==width-1 and abs(dx-width//2)<=1:continue
                    if y%8 in [3,4] and (dx==width//2 or dz==width//2):continue
                    t.set(x+dx,y,z+dz,glow if y%8==0 else stone)
                elif y%8==0 and (dx,dz) not in well:t.set(x+dx,y,z+dz,stone)
        if y<height:
            dx,dz,facing=perimeter[(y-1)%len(perimeter)]
            t.set(x+dx,y,z+dz,'minecraft:polished_deepslate_stairs',{'facing':facing,'half':'bottom','shape':'straight','waterlogged':'false'})
    roof=height-1
    for dx in range(2,width-2):
        for dz in range(2,width-2):t.set(x+dx,roof,z+dz,stone)
    if width>=7:t.chest(x+width//2,roof+1,z+width//2,'awakening/'+loot)

def gateway(t,cx,z,stone,glow,active):
    for x in [cx-6,cx+5]:pillar(t,x,z,14,stone,glow,2)
    for x in range(cx-6,cx+7):
        t.set(x,14,z,glow);t.set(x,15,z,stone)
    for x in [cx-3,cx+3]:t.set(x,1,z,'riftborn:resonator',{'note':'3'})
    t.set(cx,1,z,'riftborn:abyss_gate',{'active':'true' if active else 'false'})

def generate(name,spec):
    width,height,realm,spacing,separation,salt,circular,verse=spec
    t=Template((width,height,width));t.clear();r=random.Random(salt);c=width//2
    stone='riftborn:abyssal_stone' if realm=='abyss' else 'riftborn:rift_stone'
    brick='minecraft:deepslate_bricks' if realm=='abyss' else 'minecraft:polished_blackstone_bricks'
    glow='riftborn:luminous_shale' if realm=='abyss' else 'minecraft:crying_obsidian'
    for x in range(width):
        for z in range(width):
            dist=math.hypot(x-c,z-c)
            if circular and dist>c:continue
            material=stone if abs(x-c)<=1 or abs(z-c)<=1 or int(dist)%11==0 else brick
            if r.random()<.06:material='minecraft:cracked_deepslate_bricks' if realm=='abyss' else 'minecraft:cracked_polished_blackstone_bricks'
            t.set(x,0,z,material)
            edge=(circular and c-1<dist<=c) or (not circular and (x in [0,width-1] or z in [0,width-1]))
            if edge and abs(x-c)>2 and abs(z-c)>2:t.set(x,1,z,stone)
    t.set(c,1,width-5,'riftborn:ancient_stele',{'verse':str(verse)})
    # All designs have distinct routes and elevations; no sealed box or void-only placement.
    if name=='rift_citadel':
        for x,z in [(3,3),(width-12,3),(3,width-12),(width-12,width-12)]:tower(t,x,z,9,height-4,brick,glow,name)
        for y in [9,17]:
            slab(t,y,8,7,width-9,9,brick);slab(t,y,8,width-10,width-9,width-8,brick)
        gateway(t,c,15,stone,'riftborn:luminous_shale',False)
        t.chest(14,1,30,'awakening/'+name);t.chest(width-15,1,30,'awakening/'+name)
    elif name=='broken_temple':
        for x,z,h in [(3,3,9),(width-6,3,12),(3,width-6,7),(width-6,width-6,10)]:pillar(t,x,z,h,stone,glow,2)
        t.set(c,1,c,'riftborn:resonance_lock',{'open':'false'})
        for x,z in [(c-3,c),(c,c-3),(c+3,c)]:t.set(x,1,z,'riftborn:resonator',{'note':'0'})
        t.set(c,1,c+7,'riftborn:ancient_stele',{'verse':'1'});gateway(t,c,6,stone,'riftborn:luminous_shale',False)
        t.chest(5,1,c,'awakening/'+name);t.chest(width-6,1,c,'awakening/'+name)
    elif name=='abyssal_fortress':
        for x,z in [(3,3),(width-14,3),(3,width-14),(width-14,width-14)]:tower(t,x,z,11,height-4,brick,glow,name)
        for y in [10,20,30]:
            for x in range(8,width-8):
                for dz in range(3):t.set(x,y,9+dz,stone);t.set(x,y,width-12+dz,stone)
            for z in range(8,width-8):
                for dx in range(3):t.set(9+dx,y,z,stone);t.set(width-12+dx,y,z,stone)
        gateway(t,c,12,stone,glow,True)
        for z in [c-5,c+5]:
            for x in [c-10,c+10]:pillar(t,x,z,18,brick,glow,2)
        slab(t,18,c-10,c-5,c+11,c+6,stone)
        for x,z in [(c-8,c),(c+8,c),(c,c+10)]:t.chest(x,1,z,'awakening/'+name)
    elif name=='forgotten_laboratory':
        for y in range(1,height-2):
            for x in range(5,width-5):
                for z in [5,width-6]:
                    if y<4 and abs(x-c)<2 and z==width-6:continue
                    t.set(x,y,z,glow if y%9==0 else 'minecraft:tinted_glass' if y%9 in range(2,7) else brick)
            for z in range(6,width-6):
                for x in [5,width-6]:t.set(x,y,z,glow if y%9==0 else 'minecraft:tinted_glass' if y%9 in range(2,7) else brick)
        for y in [0,9,18]:
            slab(t,y,6,6,width-7,width-7,stone)
            for x in [c-8,c+8]:
                for z in [c-6,c+6]:
                    pillar(t,x,z,y+4,brick,glow,1,y+1);t.set(x,y+5,z,'riftborn:abyssal_crystal_block');t.chest(x+2,y+1,z,'awakening/'+name)
            t.set(c,y+1,c,'riftborn:ancient_stele',{'verse':'3'})
        # A broad ascending stairwell links all laboratory floors.
        for y in range(1,21):
            x=8+(y-1)%20
            for z in range(8,11):
                for clear in range(1,4):t.set(x,min(height-1,y+clear),z,'minecraft:air')
                t.set(x,y,z,'minecraft:polished_deepslate_stairs',{'facing':'east','half':'bottom','shape':'straight','waterlogged':'false'})
        t.set(c,1,6,'riftborn:abyss_gate',{'active':'true'})
    elif name in ['colossus_arena','sovereign_arena']:
        radius=c-8
        for i in range(8):
            a=i*math.tau/8;x=c+round(math.cos(a)*radius);z=c+round(math.sin(a)*radius);pillar(t,x-1,z-1,10 if name=='colossus_arena' else 18,stone,glow,3)
        for ring in [5,15,c-4]:
            for x in range(width):
                for z in range(width):
                    if abs(math.hypot(x-c,z-c)-ring)<.6:t.set(x,0,z,glow)
        slab(t,1,c-2,c-2,c+2,c+2,stone);t.set(c,2,c,'riftborn:colossus_altar' if name=='colossus_arena' else 'riftborn:sovereign_altar')
        gateway(t,c,8,stone,glow,True)
        for x in [c-9,c+9]:t.chest(x,1,c+9,'awakening/'+name)
        if name=='sovereign_arena':
            for y in range(1,12):slab(t,y,c-8,c-20+y,c+8,c-20+y,brick)
            for x in [c-12,c+10]:pillar(t,x,c-24,31,brick,glow,3)
    elif name=='architect_spire':
        for x,z in [(3,3),(width-12,3),(3,width-12),(width-12,width-12)]:tower(t,x,z,9,height-4,brick,glow,name)
        for y in [10,22,34,46]:
            for x in range(9,width-9):
                for z in [10,11,width-12,width-11]:t.set(x,y,z,glow if x%6==0 else brick)
        t.set(c,1,c,'riftborn:architect_altar');gateway(t,c,8,stone,glow,True)
        for x in [c-8,c+8]:t.chest(x,1,c+10,'awakening/'+name)
    elif name=='storm_observatory':
        tower(t,c-5,c-5,11,30,brick,glow,name)
        for i in range(16):
            a=i*math.tau/16;x=c+round(math.cos(a)*14);z=c+round(math.sin(a)*14);pillar(t,x,z,18+int(12*abs(math.sin(a))),stone,glow,1)
        for x in range(c-8,c+9):
            for z in range(c-8,c+9):t.set(x,32,z,glow if x%5==0 else brick)
        t.set(c,33,c,'riftborn:ancient_stele',{'verse':'7'});t.chest(c+3,33,c,'awakening/'+name);t.set(c-3,33,c,'riftborn:abyssal_crystal_block')
        t.set(c,1,4,'riftborn:abyss_gate',{'active':'true'})
    else:
        for x,z,h in [(3,3,9),(width-6,3,13),(3,width-6,6),(width-6,width-6,10)]:pillar(t,x,z,min(height-3,h),brick,glow,2)
        for x in range(4,width-4):
            if x%5!=0:t.set(x,8,c,stone)
        t.chest(c-5,1,c+2,'awakening/'+name);t.chest(c+5,1,c+2,'awakening/'+name)
        if realm=='abyss':t.set(c,1,c-5,'riftborn:abyss_gate',{'active':'true'})
        else:t.set(c,1,c-5,'riftborn:rift_anchor',{'open':'true'})
    if realm=='rift':
        mobs=[('rift_stalker',28,c-6,c+5),('rift_wisp',20,c+6,c+5)]
        if name=='rift_citadel':mobs += [('void_brute',100,c,c+10)]
    else:
        mobs=[('abyss_stalker',42,c-7,c+8),('abyssal_warden',90,c+7,c+8),('rift_echo',46,c-7,c-8)]
        if name in ['abyssal_fortress','forgotten_laboratory']:mobs += [('abyssal_brute',150,c+7,c-8),('void_reaver',36,c,c+12)]
    for mob,hp,x,z in mobs:t.mob(x+.5,4.0 if mob in ['void_reaver','rift_wisp'] else 1.0,z+.5,mob,hp)
    for x,z in [(c-5,c-3),(c+5,c-3)]:
        t.set(x,0,z,stone);t.set(x,1,z,'riftborn:echo_fern' if realm=='rift' else 'riftborn:ashen_reeds')
    t.write(RES/f'data/riftborn/structure/awakening/{name}.nbt')
if __name__=='__main__':
    for name,spec in STRUCTURES.items():generate(name,spec)
    print('Generated ten distinct Awakening structures with reachable loot, lore, gateways, puzzle and arenas.')
