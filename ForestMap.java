import java.util.ArrayList;

public class ForestMap
{
    //wall definitions for raycaster alg
    public static final int EMPTY = 0;
    public static final int PINE_WALL = 1; // default forest wall/trees
    public static final int ANCIENT_TREE = 2;
    public static final int STONE = 3;
    public static final int ROOTS = 4;


    //representation of map
    //S -> player spawn
    //M -> boss
    //v/w -> smaller door guards
    //a/h/r -> collectables
    //X -> exit door


    //# -> wall
    //. -> free dir
    private static final String[] DATA = {
        "###########################",
        "#M....#..S.............a..#",
        "#.##..#.#..#.#.##.##.###..#",
        "#..........#...#.....#....#",
        "#...#.###.###.#.###.#.##..#",
        "#...#.....#...#.....#.....#",
        "#...#...#.#.###...###.##..#",
        "#.....#.#.#.....#...#.....#",
        "####.#.#.#.###.###.#.###..#",
        "#...#...#...#...#...#.....#",
        "#.###.##.#..#.#.#.#.###...#",
        "#...#.........#...#.......#",
        "###.#...#..##.###.#.###...#",
        "#...#...#.....#...#...#...#",
        "#.###.#.##..#.#.#..##.#...#",
        "#.....#...h...#.....#.....#",
        "#.#####.#.....###.#.#.##..#",
        "#.........a.....r.....v.wX#",
        "###########################"
    };

    private int[][] cells;
    private double startX;
    private double startY;
    private double startAngle;
    //required for storing demons and pickups useless stuf coords and type loaded and parsed from map
    private final ArrayList<Demon> demonTemplates = new ArrayList<Demon>(); 
    private final ArrayList<HorrorPickup> pickupTemplates = new ArrayList<HorrorPickup>();

    public ForestMap()
    {
        parse();
    }

    public int width()
    {
        return DATA[0].length();
    }

    public int height()
    {
        return DATA.length;
    }

    public double startX()
    {
        return startX;
    }

    public double startY()
    {
        return startY;
    }

    public double startAngle()
    {
        return startAngle;
    }

    public ArrayList<Demon> createDemons()
    {
        ArrayList<Demon> result = new ArrayList<Demon>();
        for (int i = 0; i < demonTemplates.size(); i++) {
            result.add(new Demon(demonTemplates.get(i)));
        }
        return result;
    }

    public ArrayList<HorrorPickup> createPickups()
    {
        ArrayList<HorrorPickup> result = new ArrayList<HorrorPickup>();
        for (int i = 0; i < pickupTemplates.size(); i++) {
            result.add(new HorrorPickup(pickupTemplates.get(i)));
        }
        return result;
    }

    public int cellAt(int x, int y)
    {
        //bound check for crash prevent
        if (x < 0 || y < 0 || y >= cells.length || x >= cells[0].length) {
            return PINE_WALL;
        }
        return cells[y][x];
    }

    // check if the entity hits a wall, taking its size into account
    public boolean isBlocked(double x, double y, double radius)
    {
        // check 4 corners of a bounding box around the center (x, y)
        return isSolidAt(x - radius, y - radius) // top-left
            || isSolidAt(x + radius, y - radius) // top-right
            || isSolidAt(x - radius, y + radius) // bottom-left
            || isSolidAt(x + radius, y + radius); // bottom-right
    }

    public boolean isSolidAt(double x, double y)
    {   
        // floor the coords to snap them to the grid indices
        return cellAt((int)Math.floor(x), (int)Math.floor(y)) != EMPTY;
    }

    //parse map

    private void parse()
    {
        cells = new int[DATA.length][DATA[0].length()];
        //if S missing default cords
        startX = 1.5;
        startY = 1.5;
        startAngle = 0.0;

        for (int y = 0; y < DATA.length; y++) {
            for (int x = 0; x < DATA[y].length(); x++) {
                char c = DATA[y].charAt(x);
                cells[y][x] = EMPTY; // floor by default
                
                if (c == '#') {
                    // weird formula to assign wall textures based on grid position
                    // 1+abs(...) ensures we get a valid texture ID (1-4)
                    cells[y][x] = 1 + Math.abs((x * 37 + y * 19 + x * y) % 4);
                } 
                else if (c == 'S') {
                    // start pos: add 0.5 to center the player in the middle of the tile
                    startX = x + 0.5;
                    startY = y + 0.5;
                    startAngle = 0.0;
                } 
                else if (c == 'v') {
                    demonTemplates.add(new Demon(x + 0.5, y + 0.5, Demon.STALKER));
                } 
                else if (c == 'w') {
                    // floating ghost guys
                    demonTemplates.add(new Demon(x + 0.5, y + 0.5, Demon.WRAITH));
                } 
                else if (c == 'b') {
                    // the big tanks
                    demonTemplates.add(new Demon(x + 0.5, y + 0.5, Demon.BRUTE));
                } 
                else if (c == 'M') {
                    // Boss spawn point
                    demonTemplates.add(new Demon(x + 0.5, y + 0.5, Demon.PHOTO_BOSS));
                } 
                else if (c == 'a') {
                    // ammo crate
                    pickupTemplates.add(new HorrorPickup(x + 0.5, y + 0.5, HorrorPickup.AMMO));
                } 
                else if (c == 'h') {
                    // health kit
                    pickupTemplates.add(new HorrorPickup(x + 0.5, y + 0.5, HorrorPickup.HEALTH));
                } 
                else if (c == 'r') {
                    // relic (delete?) - need these to win
                    pickupTemplates.add(new HorrorPickup(x + 0.5, y + 0.5, HorrorPickup.RELIC));
                } 
                else if (c == 'X') {
                    // end game gate
                    pickupTemplates.add(new HorrorPickup(x + 0.5, y + 0.5, HorrorPickup.GATE));
                }
            }
        }
    }
}
