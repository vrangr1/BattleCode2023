import sys
from pathlib import Path

def encode(x, y):
    return (x+7) + 15*(y+7)

# Modify as feasible in bytecode restrictions
# Amplifies is 34 but set to 25 for bytecode reasons
RADII = {'Carrier': 20, 'Launcher': 20, 'Destabilizer': 20, 'Booster': 20, 'Headquarter': 34, 'Amplifier': 25}
SMALLER_RADII = {'Carrier': 10, 'Launcher': 10, 'Destabilizer': 10, 'Booster': 10, 'Headquarter': 20, 'Amplifier': 20}
CURRENT_VALUES = {'Carrier': [5,8,15,40,100], 'Launcher': [5,8,15,9999,9999], 'Destabilizer': [5,8,15,9999,9999], 'Booster': [5,8,15,9999,9999], 'Headquarter': [5,8,15,9999,9999], 'Amplifier': [5,8,15,9999,99999]}

DIRECTIONS = {
    (1, 0): 'Direction.EAST',
    (-1, 0): 'Direction.WEST',
    (0, 1): 'Direction.NORTH',
    (0, -1): 'Direction.SOUTH',
    (1, 1): 'Direction.NORTHEAST',
    (-1, 1): 'Direction.NORTHWEST',
    (1, -1): 'Direction.SOUTHEAST',
    (-1, -1): 'Direction.SOUTHWEST',
}

def dist(x, y):
    return x*x + y*y

def gen_constants(radius):
    out = f""""""
    for x in range(-7, 8):
        for y in range(-7, 8):
            if dist(x, y) <= radius:
                out += f"""
    static MapLocation l{encode(x,y)}; // location representing relative coordinate ({x}, {y})
    static MapInfo m{encode(x,y)}; // map info at location representing relative coordinate ({x}, {y})
    static int d{encode(x,y)}; // shortest distance to location from current location
    static Direction dir{encode(x,y)}; // best direction to take now to optimally get to location
"""
    return out

def sign(x):
    if x > 0:
        return 1
    if x < 0:
        return -1
    return 0

def gen_init(radius):
    out = f"""
        l{encode(0,0)} = rc.getLocation();
        m{encode(0,0)} = rc.senseMapInfo(l{encode(0,0)});
        d{encode(0,0)} = 0;
        dir{encode(0,0)} = Direction.CENTER;
"""
    for r2 in range(1, radius+1):
        for x in range(-7, 8):
            for y in range(-7, 8):
                if dist(x, y) == r2:
                    out += f"""
        l{encode(x,y)} = l{encode(x - sign(x), y - sign(y))}.add({DIRECTIONS[(sign(x), sign(y))]}); // ({x}, {y}) from ({x - sign(x)}, {y - sign(y)})
        d{encode(x,y)} = 99999;
        dir{encode(x,y)} = null;
"""
    return out

def gen_bfs(radius, unit):
    visited = set([encode(0,0)])
    if unit == 'Carrier':
        occupied_val = 8
    else:
        occupied_val = 2
    out = f"""
"""
    for r2 in range(1, radius+1):
        for x in range(-7, 8):
            for y in range(-7, 8):
                if dist(x, y) == r2:
                    out += f"""
        if (rc.canSenseLocation(l{encode(x,y)})) {{ // check ({x}, {y})"""
                    indent = ""
                    if r2 <= occupied_val:
                        out += f"""
            if (!rc.isLocationOccupied(l{encode(x,y)})) {{ """
                        indent = "    "
                    dxdy = [(dx, dy) for dx in range(-1, 2) for dy in range(-1, 2) if (dx, dy) != (0, 0) and dist(x+dx,y+dy) <= radius]
                    dxdy = sorted(dxdy, key=lambda dd: dist(x+dd[0], y+dd[1]))
                    for dx, dy in dxdy:
                        if encode(x+dx, y+dy) in visited:
                            out += f"""
            {indent}if (d{encode(x,y)} > d{encode(x+dx,y+dy)}) {{ // from ({x+dx}, {y+dy})
                {indent}d{encode(x,y)} = d{encode(x+dx,y+dy)};
                {indent}dir{encode(x,y)} = {DIRECTIONS[(-dx, -dy)] if (x+dx,y+dy) == (0, 0) else f'dir{encode(x+dx,y+dy)}'};
            {indent}}}"""
                    out += f"""
            {indent}d{encode(x,y)} += locationScore(l{encode(x,y)}, m{encode(x,y)}, dir{encode(x,y)});"""
                    if r2 <= occupied_val:
                        out += f"""
            }}"""
                    visited.add(encode(x,y))
                    out += f"""
        }}"""
                if dist(x, y) == r2:
                    out += f"""
        else if (rc.onTheMap(l{encode(x,y)})) {{ // check ({x}, {y})"""
                    indent = ""
                    dxdy = [(dx, dy) for dx in range(-1, 2) for dy in range(-1, 2) if (dx, dy) != (0, 0) and dist(x+dx,y+dy) <= radius]
                    dxdy = sorted(dxdy, key=lambda dd: dist(x+dd[0], y+dd[1]))
                    for dx, dy in dxdy:
                        if encode(x+dx, y+dy) in visited:
                            out += f"""
            {indent}if (d{encode(x,y)} > d{encode(x+dx,y+dy)}) {{ // from ({x+dx}, {y+dy})
                {indent}d{encode(x,y)} = d{encode(x+dx,y+dy)};
                {indent}dir{encode(x,y)} = {DIRECTIONS[(-dx, -dy)] if (x+dx,y+dy) == (0, 0) else f'dir{encode(x+dx,y+dy)}'};
            {indent}}}"""
                    out += f"""
            {indent}d{encode(x,y)} += 12;"""
                    visited.add(encode(x,y))
                    out += f"""
        }}
"""
    return out

def gen_selection(radius, smaller_radius):
    out = f"""
        int target_dx = target.x - l{encode(0,0)}.x;
        int target_dy = target.y - l{encode(0,0)}.y;
        switch (target_dx) {{"""
    for tdx in range(-7, 8):
        if tdx**2 <= radius:
            out += f"""
                case {tdx}:
                    switch (target_dy) {{"""
            for tdy in range(-7, 8):
                if dist(tdx, tdy) <= radius:
                    out += f"""
                        case {tdy}:
                            return dir{encode(tdx, tdy)}; // destination is at relative location ({tdx}, {tdy})"""
            out += f"""
                    }}
                    break;"""
    out += f"""
        }}
        
        Direction ans = null;
        double bestScore = 0;
        double currDist = Math.sqrt(l{encode(0,0)}.distanceSquaredTo(target));
        double cost = Double.MAX_VALUE;
        """
    for x in range(-7, 8):
        for y in range(-7, 8):
            if smaller_radius < dist(x, y) <= radius: # on the edge of the radius radius
                out += f"""
        double score{encode(x,y)} = (currDist - Math.sqrt(l{encode(x,y)}.distanceSquaredTo(target))) / d{encode(x,y)};
        if (score{encode(x,y)} > bestScore) {{
            bestScore = score{encode(x,y)};
            ans = dir{encode(x,y)};
            cost = d{encode(x,y)};
        }}
"""
    return out

def gen_print(radius):
    out = f"""
        // System.out.println("LOCAL DISTANCES:");"""
    for y in range(7, -8, -1):
        if y**2 <= radius:
            out += f"""
        // System.out.println("""
            for x in range(-7, 8):
                if dist(x, y) <= radius:
                    out += f""""\\t" + d{encode(x,y)} + """
                else:
                    out += f""""\\t" + """
            out = out[:-3] + """);"""
    out += f"""
        // System.out.println("DIRECTIONS:");"""
    for y in range(7, -8, -1):
        if y**2 <= radius:
            out += f"""
        // System.out.println("""
            for x in range(-7, 8):
                if dist(x, y) <= radius:
                    out += f""""\\t" + dir{encode(x,y)} + """
                else:
                    out += f""""\\t" + """
            out = out[:-3] + """);"""
    return out

def gen_full(bot, unit):
    radius = RADII[unit]
    smaller_radius = SMALLER_RADII[unit]
    curr_values = CURRENT_VALUES[unit]
    out_file = Path('../src/') / bot / f'path' / f'Bot{unit}Pathing.java'
    with open(out_file, 'w') as f:
        f.write(f"""// Inspired by https://github.com/IvanGeffner/battlecode2021/blob/master/thirtyone/BFSMuckraker.java
// Modified from https://github.com/mvpatel2000/Battlecode2022/blob/main/scripts/generate_pathing.py
// This file is automatically generated by gen_pathing.py. Do not edit.

package {bot}.path;

import battlecode.common.*;

public class Bot{unit}Pathing implements UnitPathing {{
    
    RobotController rc;
{gen_constants(radius)}

    public Bot{unit}Pathing(RobotController rc) {{
        this.rc = rc;
    }}

    public int locationScore(MapLocation loc, MapInfo mapLoc, Direction inputDir) throws GameActionException {{
        mapLoc = rc.senseMapInfo(loc);
        if (!mapLoc.isPassable())
            return 99999;
        if (inputDir == null)   
            return 99999;
        return currentSwitchScore(inputDir, mapLoc);
    }}

    public int currentSwitchScore(Direction inputDir, MapInfo mapLoc) throws GameActionException{{
        Direction currentDir = mapLoc.getCurrentDirection();
        switch(currentDir) {{
            case NORTH:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[0]};
                    case NORTHEAST:
                        return {curr_values[1]};
                    case EAST:
                        return {curr_values[2]};
                    case SOUTHEAST:
                        return {curr_values[3]};
                    case SOUTH:
                        return {curr_values[4]};
                    case SOUTHWEST:
                        return {curr_values[3]};
                    case WEST:
                        return {curr_values[2]};
                    case NORTHWEST:
                        return {curr_values[1]};
                    default:
                        return 99999;
                }}
            case NORTHEAST:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[1]};
                    case NORTHEAST:
                        return {curr_values[0]};
                    case EAST:
                        return {curr_values[1]};
                    case SOUTHEAST:
                        return {curr_values[2]};
                    case SOUTH:
                        return {curr_values[3]};
                    case SOUTHWEST:
                        return {curr_values[4]};
                    case WEST:
                        return {curr_values[3]};
                    case NORTHWEST:
                        return {curr_values[2]};
                    default:
                        return 99999;
                }}
            case EAST:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[2]};
                    case NORTHEAST:
                        return {curr_values[1]};
                    case EAST:
                        return {curr_values[0]};
                    case SOUTHEAST:
                        return {curr_values[1]};
                    case SOUTH:
                        return {curr_values[2]};
                    case SOUTHWEST:
                        return {curr_values[3]};
                    case WEST:
                        return {curr_values[4]};
                    case NORTHWEST:
                        return {curr_values[3]};
                    default:
                        return 99999;
                }}
            case SOUTHEAST:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[3]};
                    case NORTHEAST:
                        return {curr_values[2]};
                    case EAST:
                        return {curr_values[1]};
                    case SOUTHEAST:
                        return {curr_values[0]};
                    case SOUTH:
                        return {curr_values[1]};
                    case SOUTHWEST:
                        return {curr_values[2]};
                    case WEST:
                        return {curr_values[3]};
                    case NORTHWEST:
                        return {curr_values[4]};
                    default:
                        return 99999;
                }}
            case SOUTH:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[4]};
                    case NORTHEAST:
                        return {curr_values[3]};
                    case EAST:
                        return {curr_values[2]};
                    case SOUTHEAST:
                        return {curr_values[1]};
                    case SOUTH:
                        return {curr_values[0]};
                    case SOUTHWEST:
                        return {curr_values[1]};
                    case WEST:
                        return {curr_values[2]};
                    case NORTHWEST:
                        return {curr_values[3]};
                    default:
                        return 99999;
                }}
            case SOUTHWEST:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[3]};
                    case NORTHEAST:
                        return {curr_values[4]};
                    case EAST:
                        return {curr_values[3]};
                    case SOUTHEAST:
                        return {curr_values[2]};
                    case SOUTH:
                        return {curr_values[1]};
                    case SOUTHWEST:
                        return {curr_values[0]};
                    case WEST:
                        return {curr_values[1]};
                    case NORTHWEST:
                        return {curr_values[2]};
                    default:
                        return 99999;
                }}
            case WEST:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[2]};
                    case NORTHEAST:
                        return {curr_values[3]};
                    case EAST:
                        return {curr_values[4]};
                    case SOUTHEAST:
                        return {curr_values[3]};
                    case SOUTH:
                        return {curr_values[2]};
                    case SOUTHWEST:
                        return {curr_values[1]};
                    case WEST:
                        return {curr_values[0]};
                    case NORTHWEST:
                        return {curr_values[1]};
                    default:
                        return 99999;
                }}
            case NORTHWEST:
                switch(inputDir){{
                    case NORTH:
                        return {curr_values[1]};
                    case NORTHEAST:
                        return {curr_values[2]};
                    case EAST:
                        return {curr_values[3]};
                    case SOUTHEAST:
                        return {curr_values[4]};
                    case SOUTH:
                        return {curr_values[3]};
                    case SOUTHWEST:
                        return {curr_values[2]};
                    case WEST:
                        return {curr_values[1]};
                    case NORTHWEST:
                        return {curr_values[0]};
                    default:
                        return 99999;
                }}
            default:
                return 10;
        }}
    }}

    public Direction bestDir(MapLocation target) throws GameActionException {{
{gen_init(radius)}
{gen_bfs(radius, unit)}
{gen_print(radius)}
{gen_selection(radius, smaller_radius)}
        if (cost > 9999){{
            Nav.goTo(target);
            return null;
        }}
        return ans;
    }}

}}
""")

if __name__ == '__main__':
    if len(sys.argv) == 2:
        for unit in ('Carrier', 'Launcher', 'Destabilizer', 'Booster', 'Headquarter', 'Amplifier'):
            gen_full(sys.argv[1], unit)
    else:
        gen_full(sys.argv[1], sys.argv[2])

    