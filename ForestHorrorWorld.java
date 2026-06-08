import greenfoot.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

import java.awt.Robot;
import java.io.Console;

public class ForestHorrorWorld extends World
{
    private static int SCREEN_W = 1400;
    private static int SCREEN_H = 700;
    private static final int HALF_H = SCREEN_H / 2; //1 for more fps lol
    private static final int COLUMN_WIDTH = 1;
    private static final double FOV = Math.toRadians(68.0);
    private static final double CAMERA_PLANE = Math.tan(FOV / 2.0);
    private static final double PLAYER_RADIUS = 0.18;
    private static final double MOVE_SPEED = 0.054;
    private static final double TURN_SPEED = 0.050;
    private static final double MOUSE_SENSITIVITY = 0.0065;
    private static final int MOUSE_EDGE_ZONE = 78;
    private static final double TWO_PI = Math.PI * 2.0;
    private static final String BOSS_IMAGE_FILE = "1.jpg"; //boss moving closer ambient
    private static final String BOSS_MUSIC_FILE = "audio/mob2.mp3";
    private static final double BOSS_MUSIC_RANGE = 15.5;
    private static final String BACKGROUND_MUSIC = "audio/KitchenAce.mp3";
    private static final String FIRING_MUSIC = "audio/ShotGunFiring.mp3";
    private static final int DOOR_TEXTURE_SIZE = 96;
    private static final String[] KEY_FORWARD = {"w", "ц"};
    private static final String[] KEY_BACKWARD = {"s", "ы"};
    private static final String[] KEY_TURN_LEFT = {"a", "ф"};
    private static final String[] KEY_TURN_RIGHT = {"d", "в"};
    private static final String[] KEY_RESTART = {"r", "к"};
    private static final int AIR_BUFF= 0;
    private static final int AMMO_BUFF = 1;
    private static final int HP_BUFF = 2;
    private static final int RELIC_BUFF = 3;

    private ForestMap forest;
    private HorrorPlayer player;
    private ArrayList<Demon> demons;
    private ArrayList<HorrorPickup> pickups;
    private GreenfootImage frame;
    private GreenfootImage bossFace;
    private GreenfootImage bossScreamerFace;
    private GreenfootImage doorTexture;
    private GreenfootSound bossMusic;
    private GreenfootSound backgroundMusic;
    private GreenfootSound shotgunFiring;
    private MenuWorld menu;
    private final double[] zBuffer = new double[SCREEN_W]; // needs to be updated every frame for sprites
    private final Random random = new Random(7331);

    private int tick; //main frame counter
    private int kills;
    private int totalDemons;
    private int fireCooldown;
    private int muzzleTimer; //flash stays on screen timer
    private int hurtTimer;
    private int fearPulse;
    private int lightningTimer;
    private int bossOmenTimer;
    private int bossOmenCooldown;
    private int finalScreamerTimer;
    private int buffTimer;
    private int buffCurrent;
    private int finalScreamerLength;
    private int bossMusicVolume;
    private int lastMouseX;
    private boolean hasMousePosition; //is mouse inside window
    private double bossDistance;
    private boolean dead;
    private boolean won;
    private Robot robot;
    private boolean isGameActive = false;

    public ForestHorrorWorld(MenuWorld menu, int SCREEN_W, int SCREEN_H)
    {
        super(SCREEN_W, SCREEN_H, 1);
        this.SCREEN_W = SCREEN_W;
        this.SCREEN_H = SCREEN_H;
        this.menu = menu;
        Greenfoot.setSpeed(50);

        try {
            // Создаем робота для управления мышью
            robot = new Robot();
        } catch (Exception e) {
            e.printStackTrace();
        }
        restart();
    }

    public void act()
    {
        if (Greenfoot.isKeyDown("escape"))
        {
            stopBossMusic();
            PauseMusic();
            Greenfoot.setWorld(this.menu); 
        }
        // Если игра запущена, принудительно убираем мышь в левый верхний угол экрана (0, 0)
        if (isGameActive && robot != null) {
            robot.mouseMove(0, 0);
        }
        tick++;
        //global reset key
        if (dead || won){
            isGameActive = false;
            PauseMusic();
        }
        if ((dead || won) && isAnyKeyDown(KEY_RESTART)) {
            restart();
            return;
        }

        if (finalScreamerTimer > 0) {
            updateFinalScreamer();
            render();
            return;
        }
        if (buffTimer > 0) {
            buffTimer--;
        }
        updateWeather();
        if (!dead && !won) {
            handleInput();
            updateDemons();
            updatePickups();
            updateNerve();
        }


        //tick down for all timers
        if (fireCooldown > 0) {
            fireCooldown--;
        }
        if (muzzleTimer > 0) {
            muzzleTimer--;
        }
        if (hurtTimer > 0) {
            hurtTimer--;
        }
        if (fearPulse > 0) {
            fearPulse--;
        }
        if (bossOmenTimer > 0) {
            bossOmenTimer--;
        }
        if (bossOmenCooldown > 0) {
            bossOmenCooldown--;
        }

        updateBossPresence();
        render();
    }

    // Этот метод запускается, когда нажимается кнопка RUN
    public void started() {
        isGameActive = true;
        StartMusic();
    }

    // Этот метод запускается, когда нажимается кнопка PAUSE
    public void stopped() {
        isGameActive = false;
        PauseMusic();
    }
    private void restart()
    {
        stopBossMusic();
        forest = new ForestMap();
        player = new HorrorPlayer(forest.startX(), forest.startY(), forest.startAngle());

        // rebnder everthing from the map data
        demons = forest.createDemons();
        pickups = forest.createPickups();
        frame = new GreenfootImage(SCREEN_W, SCREEN_H);
        loadBossAssets();
        loadDoorTexture();
        LoadMusic();
        StartMusic();

        tick = 0;
        kills = 0;
        totalDemons = demons.size();
        fireCooldown = 0;
        muzzleTimer = 0;
        buffTimer = 0;
        buffCurrent = AIR_BUFF;
        hurtTimer = 0;
        fearPulse = 0;
        lightningTimer = 0;
        bossOmenTimer = 0;
        bossOmenCooldown = 0;
        finalScreamerTimer = 0;
        finalScreamerLength = 0;
        bossMusicVolume = 0;
        lastMouseX = SCREEN_W / 2;
        hasMousePosition = false;
        bossDistance = 999.0;
        dead = false;
        won = false;
        isGameActive = true;
        render();
    }

    private void loadBossAssets()
    {
        try {
            bossFace = new GreenfootImage(BOSS_IMAGE_FILE);
            bossScreamerFace = new GreenfootImage(bossFace);

            // clamp the screamer size
            int omenSize = Math.max(280, Math.min((int)(SCREEN_H * 0.70), (int)(SCREEN_W * 0.42)));
            bossScreamerFace.scale(omenSize, omenSize);
            darkenOmenImage(bossScreamerFace);
        } catch (IllegalArgumentException ex) {
            //disable boss if no image
            bossFace = null;
            bossScreamerFace = null;
        }

        try {
            bossMusic = new GreenfootSound(BOSS_MUSIC_FILE);
            bossMusic.setVolume(0);
        } catch (Throwable ex) {
            bossMusic = null;
        }
    }

    private void LoadMusic(){
        try{
            backgroundMusic = new GreenfootSound(BACKGROUND_MUSIC);
            backgroundMusic.setVolume(menu.soundVolume);
        }
        catch(Exception e){
            e.notify();
        }
        try{
            shotgunFiring = new GreenfootSound(FIRING_MUSIC);
            backgroundMusic.setVolume(menu.soundVolume);
        }
        catch(Exception e){
            e.notify();
        }
    }
    
    public void StartMusic(){
        backgroundMusic.playLoop();
    }

    public void PauseMusic(){;
        backgroundMusic.pause();
        shotgunFiring.pause();
    }

    public void ResumeMusic(){
        backgroundMusic.setVolume(menu.soundVolume);
        backgroundMusic.playLoop();
    }

    private void loadDoorTexture()
    {
        //WTF WHY DOOR NOT OPEN????
        if (!isGateOpen())
            doorTexture = new GreenfootImage("CloseDoor.png");
        else
            doorTexture = new GreenfootImage("OpenDoor.png");
        //doorTexture.scale(DOOR_TEXTURE_SIZE, DOOR_TEXTURE_SIZE);
        //doorTexture.setColor(new Color(255, 255, 255));
        //doorTexture.fillRect(0, 0, DOOR_TEXTURE_SIZE, DOOR_TEXTURE_SIZE);
        //doorTexture.setColor(new Color(24, 24, 24));
        //doorTexture.drawRect(0, 0, DOOR_TEXTURE_SIZE - 1, DOOR_TEXTURE_SIZE - 1);
        // doorTexture.drawString("DOOR HERE", 17, DOOR_TEXTURE_SIZE / 2 + 4);
    }

    //make creepy creepy image without photoshop ediiting (to lazy for it)

    private void darkenOmenImage(GreenfootImage image)
    {
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                Color color = image.getColorAt(x, y);
                int red = (int)(color.getRed() * 0.34);
                int green = (int)(color.getGreen() * 0.31);
                int blue = (int)(color.getBlue() * 0.36);
                image.setColorAt(x, y, new Color(red, green, blue, color.getAlpha()));
            }
        }
    }

    private void handleInput()
    {
        // manual keyboard turning
        if (isAnyKeyDown(KEY_TURN_LEFT)) {
            player.angle -= TURN_SPEED;
        }
        if (isAnyKeyDown(KEY_TURN_RIGHT)) {
            player.angle += TURN_SPEED;
        }
        
        updateMouseLook();
        player.angle = normalizeAngle(player.angle); // wrap around to keep it clean

        double forward = 0.0;
        if (isAnyKeyDown(KEY_FORWARD)) {
            forward += MOVE_SPEED;
        }
        if (isAnyKeyDown(KEY_BACKWARD)) {
            // going back is slightly slower than walking forward
            forward -= MOVE_SPEED * 0.72;
        }

        // basic trig to convert angle to movement vector
        double moveX = Math.cos(player.angle) * forward;
        double moveY = Math.sin(player.angle) * forward;
        movePlayer(moveX, moveY);

        // shoot with space or mouse click
        if (Greenfoot.isKeyDown("space") || Greenfoot.mouseClicked(null)) {
            fireWeapon();
        }
    
    }

    private boolean isAnyKeyDown(String[] keys)
    {
        for (int i = 0; i < keys.length; i++) {
            if (Greenfoot.isKeyDown(keys[i])) {
                return true;
            }
        }
        return false;
    }

    private void updateMouseLook()
    {
        MouseInfo mouse = Greenfoot.getMouseInfo();
        if (mouse == null) {
            hasMousePosition = false;
            return;
        }

        int mouseX = mouse.getX();
        if (hasMousePosition) {
            int delta = mouseX - lastMouseX;
            // fix for the camera glitching when the mouse enters the window
            if (Math.abs(delta) < SCREEN_W / 2) {
                player.angle += delta * MOUSE_SENSITIVITY;
            }
        }

        // edge turning because greenfoot won't let me hide the cursor
        if (mouseX < MOUSE_EDGE_ZONE) {
            double edge = (MOUSE_EDGE_ZONE - mouseX) / (double)MOUSE_EDGE_ZONE;
            player.angle -= edge * TURN_SPEED * 0.90; // 0.90 just felt better during testing
        } else if (mouseX > SCREEN_W - MOUSE_EDGE_ZONE) {
            double edge = (mouseX - (SCREEN_W - MOUSE_EDGE_ZONE)) / (double)MOUSE_EDGE_ZONE;
            player.angle += edge * TURN_SPEED * 0.90;
        }

        lastMouseX = mouseX;
        hasMousePosition = true;
    }

    private void movePlayer(double dx, double dy)
    {
        double nextX = player.x + dx;
        if (!forest.isBlocked(nextX, player.y, PLAYER_RADIUS)) {
            player.x = nextX;
        }

        double nextY = player.y + dy;
        if (!forest.isBlocked(player.x, nextY, PLAYER_RADIUS)) {
            player.y = nextY;
        }
    }

    private void updateWeather()
    {
        if (lightningTimer > 0) {
            lightningTimer--;
        } else if (random.nextInt(860) == 0) {
            lightningTimer = 7 + random.nextInt(7);
            fearPulse = Math.max(fearPulse, 12);
        }
    }

    //fox logic :)
    private void updateDemons()
    {
        for (int i = 0; i < demons.size(); i++) {
            Demon demon = demons.get(i);
            if (!demon.isAlive()) {
                continue;
            }

            demon.phase++;
            // tick down all the foxc timers
            if (demon.flashTimer > 0) {
                demon.flashTimer--;
            }
            if (demon.attackCooldown > 0) {
                demon.attackCooldown--;
            }
            if (demon.wanderTimer > 0) {
                demon.wanderTimer--;
            }
            if (demon.repathTimer > 0) {
                demon.repathTimer--;
            }


            //garis mod 
            double dx = player.x - demon.x;
            double dy = player.y - demon.y;
            double distance = Math.sqrt(dx * dx + dy * dy); //range check for pyth


            //logic
            boolean seesPlayer = distance < 8.5 && lineClear(demon.x, demon.y, player.x, player.y);
            boolean sensesPlayer = (demon.type == Demon.WRAITH && distance < 5.2)
                || (demon.type == Demon.PHOTO_BOSS && distance < 16.0);
            boolean bossHunts = demon.type == Demon.PHOTO_BOSS;

            if (seesPlayer || sensesPlayer || bossHunts) {
                demon.alert = true;
                rememberPlayerCell(demon);
                if (seesPlayer && distance < 3.4) {
                    demon.moveAngle = Math.atan2(dy, dx);
                } else {
                    // use BFS pathfinding to steer around corners
                    steerDemonAlongPath(demon, Math.atan2(dy, dx));
                }
            } else if (demon.hasTarget) {
                // move to last known player position
                steerDemonAlongPath(demon, demon.moveAngle);
                if (reachedTargetCell(demon)) {
                    demon.hasTarget = false;
                    demon.wanderTimer = 0;
                }
            } else if (demon.wanderTimer <= 0) {
                // pick a random dir 
                demon.moveAngle = random.nextDouble() * TWO_PI;
                demon.wanderTimer = 40 + random.nextInt(85);
            }

            if (distance > demon.attackRange()) {
                // add sine-wave sway to the move angle (break the rigid lines)
                double sway = Math.sin((demon.phase + i * 31) * 0.05) * 0.13;
                double moveAngle = demon.moveAngle + (demon.type == Demon.STALKER || demon.type == Demon.PHOTO_BOSS ? sway : 0.0);
                double speed = demon.speed();

                //
                // project velocity vector onto map: cos(a)*s, sin(a)*s
                tryMoveDemon(demon, Math.cos(moveAngle) * speed, Math.sin(moveAngle) * speed);
            } else if (demon.attackCooldown <= 0 && lineClear(demon.x, demon.y, player.x, player.y)) {
                // handle fox-boss screamer or standard damage
                if (demon.type == Demon.PHOTO_BOSS) {
                    startFinalBossScreamer();
                    demon.attackCooldown = 120;
                    continue;
                }
                player.health -= demon.damage();
                player.nerve -= nerveDamage(demon);

                //red screen effects
                hurtTimer = 16;
                fearPulse = 22;
                demon.attackCooldown = demon.type == Demon.BRUTE ? 46 : 34;

                //check game over
                if (player.health <= 0 || player.nerve <= 0) {
                    player.health = Math.max(0, player.health);
                    player.nerve = Math.max(0, player.nerve);
                    dead = true;
                }
            }
        }
    }


    //wtf these? DELETE TODO
    private int nerveDamage(Demon demon)
    {
        if (demon.type == Demon.PHOTO_BOSS) {
            return 34;
        }
        return demon.type == Demon.BRUTE ? 16 : 11;
    }

    private void rememberPlayerCell(Demon demon)
    {
        demon.targetX = (int)Math.floor(player.x);
        demon.targetY = (int)Math.floor(player.y);
        demon.hasTarget = true;
    }

    private boolean reachedTargetCell(Demon demon)
    {
        return (int)Math.floor(demon.x) == demon.targetX && (int)Math.floor(demon.y) == demon.targetY;
    }

    private void steerDemonAlongPath(Demon demon, double fallbackAngle)
    {
        if (!demon.hasTarget) {
            demon.moveAngle = fallbackAngle;
            return;
        }

        // dont run pathfinding every single tick,  waste of cpu
        // boss needs to react faster, 
        if (demon.repathTimer <= 0) {
            demon.moveAngle = pathAngleToTarget(demon, demon.targetX, demon.targetY, fallbackAngle);
            if (demon.type == Demon.PHOTO_BOSS) {
                demon.repathTimer = 8;
            } else if (demon.type == Demon.WRAITH) {
                demon.repathTimer = 12;
            } else {
                demon.repathTimer = 18;
            }
        }
    }

    // BFS pathfinder to figure out which tile to step into next
    private double pathAngleToTarget(Demon demon, int goalX, int goalY, double fallbackAngle)
    {
        // grid snap: convert world doubles to map indices
        int startX = (int)Math.floor(demon.x);
        int startY = (int)Math.floor(demon.y);
        
        if (startX == goalX && startY == goalY) {
            return fallbackAngle;
        }
        
        // bail out if points are out of bounds or inside walls
        if (!canWalkCell(startX, startY) || !canWalkCell(goalX, goalY)) {
            return fallbackAngle;
        }

        int width = forest.width();
        int height = forest.height();
        
        // search state: visited flags
        boolean[][] visited = new boolean[height][width];
        int[][] parentX = new int[height][width];
        int[][] parentY = new int[height][width];
        
        // initialize parent grid with null/invalid pointers
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                parentX[y][x] = -1;
                parentY[y][x] = -1;
            }
        }

        // manual queue implementation for performance
        int[] queueX = new int[width * height];
        int[] queueY = new int[width * height];
        int head = 0;
        int tail = 0;
        
        queueX[tail] = startX;
        queueY[tail] = startY;
        tail++;
        visited[startY][startX] = true;

        // 4-way connectivity
        int[] dirsX = {1, -1, 0, 0};
        int[] dirsY = {0, 0, 1, -1};
        boolean found = false;
        
        while (head < tail) {
            int currentX = queueX[head];
            int currentY = queueY[head];
            head++;

            // reached the player's tile
            if (currentX == goalX && currentY == goalY) {
                found = true;
                break;
            }

            for (int i = 0; i < 4; i++) {
                int nextX = currentX + dirsX[i];
                int nextY = currentY + dirsY[i];
                
                if (!canWalkCell(nextX, nextY) || visited[nextY][nextX]) {
                    continue;
                }

                visited[nextY][nextX] = true;
                parentX[nextY][nextX] = currentX;
                parentY[nextY][nextX] = currentY;
                queueX[tail] = nextX;
                queueY[tail] = nextY;
                tail++;
            }
        }

        if (!found) {
            return fallbackAngle;
        }

        // backtrack from goal to find the immediate next step from start
        int stepX = goalX;
        int stepY = goalY;
        while (!(parentX[stepY][stepX] == startX && parentY[stepY][stepX] == startY)) {
            int previousX = parentX[stepY][stepX];
            int previousY = parentY[stepY][stepX];
            if (previousX < 0 || previousY < 0) {
                return fallbackAngle;
            }
            stepX = previousX;
            stepY = previousY;
        }

        // vector math: target center of the next cell (+0.5) 
        // atan2(dy, dx) gives the precise angle in radians
        return Math.atan2(stepY + 0.5 - demon.y, stepX + 0.5 - demon.x);
    }

    private boolean canWalkCell(int x, int y)
    {
        return x >= 0 && y >= 0 && x < forest.width() && y < forest.height()
            && forest.cellAt(x, y) == ForestMap.EMPTY;
    }

    private void tryMoveDemon(Demon demon, double dx, double dy)
    {
        double nextX = demon.x + dx;
        if (!forest.isBlocked(nextX, demon.y, demon.radius())) {
            demon.x = nextX;
        } else {
            demon.moveAngle += 0.95;
            demon.wanderTimer = 18;
            demon.repathTimer = 0;
        }

        double nextY = demon.y + dy;
        if (!forest.isBlocked(demon.x, nextY, demon.radius())) {
            demon.y = nextY;
        } else {
            demon.moveAngle -= 0.80;
            demon.wanderTimer = 18;
            demon.repathTimer = 0;
        }
    }

    // loop through everything on the floor
    private void updatePickups()
    {
        for (int i = 0; i < pickups.size(); i++) {
            HorrorPickup pickup = pickups.get(i);
            if (pickup.taken) {
                continue;
            }

            // vector from player to item
            double dx = pickup.x - player.x;
            double dy = pickup.y - player.y;
            
            // squared distance to avoid expensive Math.sqrt() calls during every tick
            double distanceSq = dx * dx + dy * dy;
            
            // pickup radius check. 0.42 distanceSq is roughly 0.65 world units
            if (distanceSq > 0.42) {
                continue;
            }

            if (pickup.type == HorrorPickup.AMMO) {
                player.ammo = Math.min(player.ammo+18, 48);
                pickup.taken = true;
                buffCurrent = AMMO_BUFF;
                activateBuff();
            } else if (pickup.type == HorrorPickup.HEALTH) {
                // heal + cap at 100. also restores a bit of sanity/nerve
                player.health = Math.min(100, player.health + 35);
                player.nerve = Math.min(100, player.nerve + 18);
                pickup.taken = true;
                buffCurrent = HP_BUFF;
                activateBuff();
            } else if (pickup.type == HorrorPickup.RELIC) {
                player.relics++;
                player.nerve = Math.min(100, player.nerve + 20);
                fearPulse = 18; // quick screen shake when grabbing a relic
                pickup.taken = true;
                buffCurrent = RELIC_BUFF;
                activateBuff();
            } else if (pickup.type == HorrorPickup.GATE && isGateOpen()) {
                // simple win trigger if conditions are met
                won = true;
            }
        }
    }
    //Set timer show buff effect 
    public void activateBuff()
    {
        this.buffTimer = 100;
    }

    private void updateNerve()
    {
        boolean closeThreat = false;
        for (int i = 0; i < demons.size(); i++) {
            Demon demon = demons.get(i);
            if (!demon.isAlive()) {
                continue;
            }
            double dx = demon.x - player.x;
            double dy = demon.y - player.y;
            double distSq = dx * dx + dy * dy;
            if (distSq < 7.0 || (demon.type == Demon.PHOTO_BOSS && distSq < 36.0)) {
                closeThreat = true;
                break;
            }
        }

        if (closeThreat && tick % 18 == 0) {
            player.nerve = Math.max(0, player.nerve - 1);
            fearPulse = Math.max(fearPulse, 6);
        } else if (!closeThreat && tick % 55 == 0) {
            player.nerve = Math.min(100, player.nerve + 1);
        }

        if (player.nerve <= 0) {
            dead = true;
        }
    }

    private void fireWeapon()
    {
        if (fireCooldown > 0) {
            return;
        }

        fireCooldown = 13;
        muzzleTimer = 5;
        if (player.ammo <= 0) {
            fearPulse = Math.max(fearPulse, 9);
            return;
        }
        shotgunFiring.stop();
        player.ammo--;
        alertDemonsFromNoise(7.5);
        Demon best = null;
        double bestDistance = 999.0;
        for (int i = 0; i < demons.size(); i++) {
            Demon demon = demons.get(i);
            if (!demon.isAlive()) {
                continue;
            }
            double dx = demon.x - player.x;
            double dy = demon.y - player.y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            double angle = Math.atan2(dy, dx);
            double delta = Math.abs(normalizeAngle(angle - player.angle));
            double tolerance = 0.052 + 0.35 / Math.max(2.0, distance);
            if (delta < tolerance && distance < bestDistance && lineClear(player.x, player.y, demon.x, demon.y)) {
                best = demon;
                bestDistance = distance;
            }
        }
        shotgunFiring.play();
        if (best != null) {
            int damage = best.scoreDamage(50);
            best.health -= damage;
            best.alert = true;
            rememberPlayerCell(best);
            best.flashTimer = best.type == Demon.PHOTO_BOSS ? 9 : 6;
            fearPulse = Math.max(fearPulse, best.type == Demon.PHOTO_BOSS ? 16 : 8);
            if (best.health <= 0) {
                kills++;
                player.nerve = Math.min(100, player.nerve + 8);
            }
        }
    }

    private void alertDemonsFromNoise(double range)
    {
        double rangeSq = range * range;
        for (int i = 0; i < demons.size(); i++) {
            Demon demon = demons.get(i);
            if (!demon.isAlive()) {
                continue;
            }
            double dx = demon.x - player.x;
            double dy = demon.y - player.y;
            if (dx * dx + dy * dy <= rangeSq || demon.type == Demon.PHOTO_BOSS) {
                demon.alert = true;
                rememberPlayerCell(demon);
                demon.repathTimer = 0;
            }
        }
    }

    private void updateBossPresence()
    {
        Demon boss = findBoss();
        if (boss == null || !boss.isAlive() || dead || won) {
            bossDistance = 999.0;
            stopBossMusic();
            return;
        }

        double dx = boss.x - player.x;
        double dy = boss.y - player.y;
        bossDistance = Math.sqrt(dx * dx + dy * dy);
        updateBossMusicVolume(bossDistance);
        updateBossOmen();

        boolean visible = canSeeBoss(boss);
        if (bossDistance < 5.5 && visible) {
            fearPulse = Math.max(fearPulse, bossDistance < 3.2 ? 15 : 7);
            boss.alert = true;
        }

    }

    private boolean canSeeBoss(Demon boss)
    {
        if (boss == null || !lineClear(player.x, player.y, boss.x, boss.y)) {
            return false;
        }

        double angleToBoss = Math.atan2(boss.y - player.y, boss.x - player.x);
        double delta = Math.abs(normalizeAngle(angleToBoss - player.angle));
        return delta < FOV * 0.56 || bossDistance < 1.35;
    }

    private Demon findBoss()
    {
        for (int i = 0; i < demons.size(); i++) {
            Demon demon = demons.get(i);
            if (demon.type == Demon.PHOTO_BOSS) {
                return demon;
            }
        }
        return null;
    }

    private void updateBossOmen()
    {
        if (bossMusicVolume <= 0 || bossScreamerFace == null) {
            bossOmenTimer = 0;
            bossOmenCooldown = 0;
            return;
        }

        if (bossOmenTimer > 0 || bossOmenCooldown > 0) {
            return;
        }

        double closeness = bossMusicVolume / 100.0;
        int minDelay = Math.max(7, 74 - (int)(closeness * 66.0));
        int randomDelay = Math.max(0, (int)((1.0 - closeness) * 18.0));
        bossOmenTimer = closeness > 0.54 ? 2 : 1;
        bossOmenCooldown = minDelay + (randomDelay == 0 ? 0 : noise(tick, bossMusicVolume) % randomDelay);
    }

    private void triggerBossOmen(boolean strong)
    {
        bossOmenTimer = Math.max(bossOmenTimer, strong ? 2 : 1);
        bossOmenCooldown = Math.max(bossOmenCooldown, strong ? 16 : 28);
        fearPulse = Math.max(fearPulse, strong ? 20 : 10);
    }

    private void startFinalBossScreamer()
    {
        if (finalScreamerTimer > 0 || dead || won) {
            return;
        }

        finalScreamerLength = 72;
        finalScreamerTimer = finalScreamerLength;
        bossOmenTimer = 0;
        bossOmenCooldown = 0;
        hurtTimer = 28;
        fearPulse = 60;
        lightningTimer = 18;
        bossMusicVolume = 100;
        if (bossMusic != null) {
            try {
                bossMusic.setVolume(100);
                if (!bossMusic.isPlaying()) {
                    bossMusic.playLoop();
                }
            } catch (Throwable ex) {
                bossMusic = null;
            }
        }
    }

    private void updateFinalScreamer()
    {
        finalScreamerTimer--;
        bossMusicVolume = 100;
        hurtTimer = Math.max(hurtTimer, 4);
        fearPulse = Math.max(fearPulse, 54);
        lightningTimer = Math.max(lightningTimer, finalScreamerTimer % 9 < 4 ? 16 : 5);

        if (bossMusic != null) {
            try {
                bossMusic.setVolume(100);
                if (!bossMusic.isPlaying()) {
                    bossMusic.playLoop();
                }
            } catch (Throwable ex) {
                bossMusic = null;
            }
        }

        if (finalScreamerTimer <= 0) {
            player.health = 0;
            player.nerve = 0;
            dead = true;
        }
    }

    private void updateBossMusicVolume(double distance)
    {
        if (bossMusic == null) {
            return;
        }

        int targetVolume = bossVolumeForDistance(distance);
        try {
            if (targetVolume <= 0) {
                if (bossMusic.isPlaying()) {
                    bossMusic.stop();
                }
                bossMusicVolume = 0;
                return;
            }

            if (!bossMusic.isPlaying()) {
                bossMusic.playLoop();
            }
            if (bossMusicVolume < targetVolume) {
                bossMusicVolume = Math.min(targetVolume, bossMusicVolume + 4);
            } else if (bossMusicVolume > targetVolume) {
                bossMusicVolume = Math.max(targetVolume, bossMusicVolume - 6);
            }
            bossMusic.setVolume((int)(bossMusicVolume*((double)menu.soundVolume/100)));
            bossMusic.wait(1);
        } catch (Throwable ex) {
            bossMusic = null;
        }
    }

    private int bossVolumeForDistance(double distance)
    {
        if (distance >= BOSS_MUSIC_RANGE) {
            return 0;
        }
        double closeness = 1.0 - distance / BOSS_MUSIC_RANGE;
        return Math.max(0, Math.min(100, (int)(Math.pow(closeness, 1.55) * 100.0)));
    }

    private void stopBossMusic()
    {
        if (bossMusic == null) {
            return;
        }
        try {
            bossMusic.stop();
        } catch (Throwable ex) {
            bossMusic = null;
        }
        bossMusicVolume = 0;
    }
    //core lo
    private void render()
    {
        drawBackdrop();
        renderWalls();
        renderSprites();
        drawRainAndFog();
        drawWeapon();
        drawHud();
        drawMiniMap();

        // Отрисовка рамки бафа, если таймер активен
        if (buffTimer > 0) {
            drawBuffBorder();
        }

        if (hurtTimer > 0 || fearPulse > 0) {
            drawFearOverlay();
        }
        if (bossOmenTimer > 0) {
            drawBossOmenOverlay();
        }
        if (finalScreamerTimer > 0) {
            drawFinalBossScreamer();
        }
        if (dead || won) {
            drawEndOverlay();
        }
        setBackground(frame);
    }
    private void drawBackdrop()
    {

        // render the top half of the screen
        int lightning = lightningIntensity();
        for (int y = 0; y < HALF_H; y += 2) {
            double t = y / (double)HALF_H;
            int r = (int)(5 + t * 15) + lightning / 4;
            int g = (int)(8 + t * 18) + lightning / 4;
            int b = (int)(16 + t * 31) + lightning / 3;
            frame.setColor(new Color(clampColor(r), clampColor(g), clampColor(b)));
            frame.fillRect(0, y, SCREEN_W, 2);
        }

        drawMoon(lightning);
        drawCloudBands(lightning);

        for (int y = HALF_H; y < SCREEN_H; y += 2) {
            double t = (y - HALF_H) / (double)(SCREEN_H - HALF_H);
            int r = (int)(14 - t * 4) + lightning / 7;
            int g = (int)(17 - t * 4) + lightning / 8;
            int b = (int)(16 - t * 5) + lightning / 8;
            frame.setColor(new Color(clampColor(r), clampColor(g), clampColor(b)));
            frame.fillRect(0, y, SCREEN_W, 2);
        }

        drawGroundTexture();
        drawHorizonTrees(lightning);
        drawFlashlightMist();
    }

    private void drawMoon(int lightning)
    {
        int mx = SCREEN_W - 122;
        int my = 54;
        for (int i = 62; i > 12; i -= 10) {
            int alpha = Math.max(10, 44 - i / 2) + lightning / 3;
            frame.setColor(new Color(119, 144, 161, clampColor(alpha)));
            frame.fillOval(mx - i / 2, my - i / 2, i, i);
        }
        frame.setColor(new Color(196, 210, 203));
        frame.fillOval(mx - 20, my - 20, 40, 40);
        frame.setColor(new Color(132, 145, 149));
        frame.fillOval(mx - 2, my - 11, 9, 8);
        frame.fillOval(mx - 14, my + 4, 6, 6);
    }

    private void drawCloudBands(int lightning)
    {
        for (int i = 0; i < 7; i++) {
            int y = 34 + i * 20 + (int)(Math.sin((tick + i * 43) * 0.012) * 5);
            int x = (i * 151 - tick / 5) % (SCREEN_W + 160) - 120;
            frame.setColor(new Color(24 + lightning / 7, 29 + lightning / 7, 38 + lightning / 5, 70));
            frame.fillOval(x, y, 190, 24);
            frame.fillOval(x + 56, y - 7, 145, 27);
        }
    }

    private void drawGroundTexture()
    {
        for (int y = HALF_H + 8; y < SCREEN_H; y += 9) {
            double depth = (y - HALF_H) / (double)(SCREEN_H - HALF_H);
            int alpha = (int)(28 + depth * 55);
            frame.setColor(new Color(5, 8, 7, clampColor(alpha)));
            frame.drawLine(0, y, SCREEN_W, y + (int)(depth * 10));
        }

        int sway = (int)((player.x * 13 + player.y * 17 + tick * 0.4) % 28);
        for (int i = 0; i < 38; i++) {
            int y = HALF_H + 18 + i * 7;
            int width = 34 + i * 15;
            int x = SCREEN_W / 2 - width / 2 + ((i * 19 + sway) % 24) - 12;
            frame.setColor(new Color(29, 23, 20, 85));
            frame.drawLine(x, y, x + width, y + 3);
        }
    }

    private void drawHorizonTrees(int lightning)
    {
        frame.setColor(new Color(5 + lightning / 10, 9 + lightning / 12, 8 + lightning / 13));
        for (int i = 0; i < 46; i++) {
            int x = (i * 37 - tick / 10) % (SCREEN_W + 70) - 35;
            int h = 34 + noise(i, 9) % 64;
            frame.fillRect(x, HALF_H - h + 18, 5 + noise(i, 4) % 9, h);
            int[] xs = {x - 14, x + 5, x + 26};
            int[] ys = {HALF_H - h + 44, HALF_H - h + 5, HALF_H - h + 44};
            frame.fillPolygon(xs, ys, 3);
        }
    }

    private void drawFlashlightMist()
    {
        for (int i = 0; i < 7; i++) {
            int y = HALF_H - 16 + i * 18;
            int w = 160 + i * 54;
            int x = SCREEN_W / 2 - w / 2;
            frame.setColor(new Color(88, 103, 99, 13));
            frame.fillOval(x, y, w, 34);
        }
    }

    private void renderWalls()
    {
        double dirX = Math.cos(player.angle);
        double dirY = Math.sin(player.angle);
        double planeX = -dirY * CAMERA_PLANE;
        double planeY = dirX * CAMERA_PLANE;

        for (int x = 0; x < SCREEN_W; x += COLUMN_WIDTH) {
            double cameraX = 2.0 * x / SCREEN_W - 1.0;
            double rayDirX = dirX + planeX * cameraX;
            double rayDirY = dirY + planeY * cameraX;
            RayHit hit = castRay(rayDirX, rayDirY);

            int lineHeight = (int)(SCREEN_H / Math.max(0.05, hit.distance));

            int drawStart = Math.max(0, -lineHeight / 2 + HALF_H);
            int drawEnd = Math.min(SCREEN_H - 1, lineHeight / 2 + HALF_H);
            Color wallColor = texturedWallColor(hit, x);
            frame.setColor(wallColor);
            frame.fillRect(x, drawStart, COLUMN_WIDTH, Math.max(1, drawEnd - drawStart + 1));

            drawWallDetails(hit, x, drawStart, drawEnd, wallColor);
            for (int c = 0; c < COLUMN_WIDTH && x + c < SCREEN_W; c++) {
                zBuffer[x + c] = hit.distance;
            }
        }
    }

    private RayHit castRay(double rayDirX, double rayDirY)
    {
        int mapX = (int)player.x;
        int mapY = (int)player.y;
        double deltaDistX = rayDirX == 0.0 ? 1.0e30 : Math.abs(1.0 / rayDirX);
        double deltaDistY = rayDirY == 0.0 ? 1.0e30 : Math.abs(1.0 / rayDirY);
        double sideDistX;
        double sideDistY;
        int stepX;
        int stepY;

        if (rayDirX < 0.0) {
            stepX = -1;
            sideDistX = (player.x - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0 - player.x) * deltaDistX;
        }

        if (rayDirY < 0.0) {
            stepY = -1;
            sideDistY = (player.y - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0 - player.y) * deltaDistY;
        }

        int side = 0;
        int cell = ForestMap.PINE_WALL;
        for (int safety = 0; safety < 96; safety++) {
            if (sideDistX < sideDistY) {
                sideDistX += deltaDistX;
                mapX += stepX;
                side = 0;
            } else {
                sideDistY += deltaDistY;
                mapY += stepY;
                side = 1;
            }

            cell = forest.cellAt(mapX, mapY);
            if (cell != ForestMap.EMPTY) {
                break;
            }
        }

        double distance;
        if (side == 0) {
            distance = rayDirX == 0.0 ? 99.0 : (mapX - player.x + (1.0 - stepX) / 2.0) / rayDirX;
        } else {
            distance = rayDirY == 0.0 ? 99.0 : (mapY - player.y + (1.0 - stepY) / 2.0) / rayDirY;
        }
        distance = Math.max(0.05, distance);

        double texture;
        if (side == 0) {
            texture = player.y + distance * rayDirY;
        } else {
            texture = player.x + distance * rayDirX;
        }
        texture -= Math.floor(texture);

        return new RayHit(distance, side, cell, mapX, mapY, texture);
    }

    private Color texturedWallColor(RayHit hit, int screenX)
    {
        Color base = wallBaseColor(hit.cell);
        double flashlight = flashlightAt(screenX, hit.distance);
        double shade = 0.22 + flashlight * 1.25 + lightningIntensity() / 115.0;
        if (hit.side == 1) {
            shade *= 0.72;
        }

        int bark = noise(hit.mapX * 17 + (int)(hit.texture * 70.0), hit.mapY * 13);
        shade *= 0.78 + bark / 510.0;
        Color lit = shade(base, shade);

        double fog = Math.min(0.86, hit.distance * 0.055);
        return blend(lit, new Color(14, 20, 23), fog);
    }

    private void drawWallDetails(RayHit hit, int x, int top, int bottom, Color wallColor)
    {
        int h = bottom - top;
        if (h <= 0) {
            return;
        }

        int groove = (int)(hit.texture * 48.0) + noise(hit.mapX, hit.mapY) % 7;
        if (hit.cell == ForestMap.STONE) {
            if (groove % 9 < 2) {
                frame.setColor(shade(wallColor, 0.52));
                frame.drawLine(x, top, x, bottom);
            }
            if ((hit.mapX + hit.mapY + groove) % 17 == 0) {
                frame.setColor(new Color(65, 170, 151, 130));
                frame.drawLine(x, top + h / 3, x, top + h / 3 + Math.max(8, h / 7));
            }
        } else {
            if (groove % 8 < 3) {
                frame.setColor(shade(wallColor, 0.58));
                frame.drawLine(x, top, x, bottom);
            } else if (groove % 13 == 0) {
                frame.setColor(shade(wallColor, 1.22));
                frame.drawLine(x, top + h / 5, x, bottom - h / 7);
            }
        }

        if (hit.cell == ForestMap.ROOTS && bottom > HALF_H) {
            frame.setColor(new Color(7, 12, 9, 175));
            frame.drawLine(x, bottom - h / 6, x + COLUMN_WIDTH, bottom);
        }
    }

    private void renderSprites()
    {
        ArrayList<HorrorSprite> sprites = new ArrayList<HorrorSprite>();
        for (int i = 0; i < pickups.size(); i++) {
            HorrorPickup pickup = pickups.get(i);
            if (!pickup.taken) {
                sprites.add(new HorrorSprite(pickup));
            }
        }
        for (int i = 0; i < demons.size(); i++) {
            Demon demon = demons.get(i);
            if (demon.isAlive()) {
                sprites.add(new HorrorSprite(demon));
            }
        }

        for (int i = 0; i < sprites.size(); i++) {
            HorrorSprite sprite = sprites.get(i);
            double dx = sprite.x - player.x;
            double dy = sprite.y - player.y;
            sprite.distanceSq = dx * dx + dy * dy;
        }

        Collections.sort(sprites, new Comparator<HorrorSprite>() {
            public int compare(HorrorSprite a, HorrorSprite b) {
                return Double.compare(b.distanceSq, a.distanceSq);
            }
        });

        double dirX = Math.cos(player.angle);
        double dirY = Math.sin(player.angle);
        double planeX = -dirY * CAMERA_PLANE;
        double planeY = dirX * CAMERA_PLANE;

        for (int i = 0; i < sprites.size(); i++) {
            renderSprite(sprites.get(i), dirX, dirY, planeX, planeY);
        }
    }

    private void renderSprite(HorrorSprite sprite, double dirX, double dirY, double planeX, double planeY)
    {
        double spriteX = sprite.x - player.x;
        double spriteY = sprite.y - player.y;
        double inverse = 1.0 / (planeX * dirY - dirX * planeY);
        double transformX = inverse * (dirY * spriteX - dirX * spriteY);
        double transformY = inverse * (-planeY * spriteX + planeX * spriteY);

        if (transformY <= 0.12) {
            return;
        }

        int screenX = (int)((SCREEN_W / 2.0) * (1.0 + transformX / transformY));
        double heightScale = 0.46;
        double widthScale = 0.40;
        if (sprite.demon != null) {
            heightScale = sprite.demon.type == Demon.BRUTE ? 1.08 : 0.96;
            widthScale = sprite.demon.type == Demon.BRUTE ? 0.88 : 0.62;
            if (sprite.demon.type == Demon.PHOTO_BOSS) {
                heightScale = 1.34;
                widthScale = 0.98;
                if (transformY < 3.0) {
                    screenX += (noise(tick, (int)(transformY * 40.0)) % 11) - 5;
                }
            }
            if (sprite.demon.type == Demon.WRAITH) {
                heightScale = 1.05;
                widthScale = 0.56;
            }
        } else if (sprite.pickup.type == HorrorPickup.GATE) {
            heightScale = 1.14;
            widthScale = 0.70;
        }

        int spriteHeight = Math.abs((int)(SCREEN_H / transformY * heightScale));
        int spriteWidth = Math.abs((int)(SCREEN_H / transformY * widthScale));
        int drawStartY = -spriteHeight / 2 + HALF_H;
        int drawEndY = spriteHeight / 2 + HALF_H;
        int drawStartX = -spriteWidth / 2 + screenX;
        int drawEndX = spriteWidth / 2 + screenX;

        for (int stripe = Math.max(0, drawStartX); stripe < Math.min(SCREEN_W, drawEndX); stripe += COLUMN_WIDTH) {
            if (transformY >= zBuffer[stripe]) {
                continue;
            }
            if (sprite.demon != null) {
                drawDemonStripe(sprite.demon, stripe, screenX, drawStartY, drawEndY, spriteWidth, transformY);
            } else {
                drawPickupStripe(sprite.pickup, stripe, screenX, drawStartY, drawEndY, spriteWidth, transformY);
            }
        }
    }

    private void drawDemonStripe(Demon demon, int x, int centerX, int top, int bottom, int width, double depth)
    {
        int height = bottom - top;
        if (height <= 0 || width <= 0) {
            return;
        }

        double relative = (x - centerX) / (width / 2.0);
        double abs = Math.abs(relative);
        double light = demon.flashTimer > 0 ? 1.8 : 0.72 + flashlightAt(x, depth) * 0.95;
        light += lightningIntensity() / 110.0;
        double fog = Math.min(0.65, depth * 0.045);

        if (demon.type == Demon.PHOTO_BOSS) {
            drawPhotoBossStripe(demon, x, centerX, top, height, width, relative, abs, light, fog, depth);
        } else if (demon.type == Demon.WRAITH) {
            drawWraithStripe(demon, x, top, height, abs, relative, light, fog);
        } else if (demon.type == Demon.BRUTE) {
            drawBruteStripe(demon, x, top, height, abs, relative, light, fog);
        } else {
            drawStalkerStripe(demon, x, top, height, abs, relative, light, fog);
        }
    }

    private void drawPhotoBossStripe(Demon demon, int x, int centerX, int top, int height, int width, double relative, double abs, double light, double fog, double depth)
    {
        if (bossFace == null) {
            drawBruteStripe(demon, x, top, height, abs, relative, light, fog);
            return;
        }

        double edgeNoise = (noise(x + tick, demon.phase) - 128) / 900.0;
        if (abs > 0.92 + edgeNoise) {
            return;
        }

        int sourceW = bossFace.getWidth();
        int sourceH = bossFace.getHeight();
        int sourceX = clampIndex((int)((relative + 1.0) * 0.5 * (sourceW - 1)), sourceW);
        int step = height > 430 ? 3 : 2;
        int jitter = bossDistance < 4.0 ? noise(x, tick) % 5 - 2 : 0;
        double pulse = 0.56 + light * 0.55 + Math.sin((tick + x) * 0.10) * 0.05;

        for (int y = Math.max(0, top); y < Math.min(SCREEN_H, top + height); y += step) {
            double v = (y - top) / (double)Math.max(1, height);
            if (v < 0.0 || v > 1.0) {
                continue;
            }

            int sourceY = clampIndex((int)(v * (sourceH - 1)), sourceH);
            Color sample = bossFace.getColorAt(sourceX, sourceY);
            Color color = shade(sample, pulse);

            if ((x + y + tick) % 47 == 0 || (bossDistance < 3.6 && (x + tick) % 31 == 0)) {
                color = blend(color, new Color(218, 12, 26), 0.55);
            }
            if (demon.flashTimer > 0 && (y + x) % 11 < 5) {
                color = blend(color, new Color(255, 245, 220), 0.42);
            }

            double darkness = Math.min(0.76, fog + depth * 0.025);
            color = blend(color, new Color(5, 6, 7), darkness);
            fillRectClipped(x + jitter, y, COLUMN_WIDTH + Math.abs(jitter), step, color);
        }

        if (abs > 0.74 && (x + tick) % 5 == 0) {
            fillRectClipped(x, top + height / 10, COLUMN_WIDTH, (int)(height * 0.80), new Color(2, 3, 4, 170));
        }
        if (bossDistance < 4.5 && (x - centerX) % 17 == 0) {
            frame.setColor(new Color(195, 18, 32, 145));
            frame.drawLine(x + jitter, top, x + jitter, top + height);
        }
    }

    //nah procedure-generating?
    //TODO: replace with .jpg
    private void drawStalkerStripe(Demon demon, int x, int top, int height, double abs, double relative, double light, double fog)
    {
        Color body = blend(shade(new Color(18, 23, 20), light), new Color(8, 13, 14), fog);
        Color bone = blend(shade(new Color(180, 174, 140), light), new Color(18, 22, 22), fog);
        Color eye = demon.flashTimer > 0 ? new Color(255, 246, 215) : new Color(96, 245, 141);

        if (abs > 0.28 && abs < 0.72) {
            int antlerTop = top + (int)(height * 0.02);
            int antlerBottom = top + (int)(height * (0.22 + abs * 0.08));
            fillRectClipped(x, antlerTop, COLUMN_WIDTH, antlerBottom - antlerTop, bone);
        }
        if (abs < 0.34) {
            double curve = Math.sqrt(1.0 - (abs / 0.34) * (abs / 0.34));
            int head = top + (int)(height * 0.24);
            int radius = (int)(height * 0.18 * curve);
            fillRectClipped(x, head - radius, COLUMN_WIDTH, radius * 2, body);
        }
        if (abs < 0.45) {
            int bodyTop = top + (int)(height * 0.38);
            int bodyBottom = top + (int)(height * (0.94 - abs * 0.12));
            fillRectClipped(x, bodyTop, COLUMN_WIDTH, bodyBottom - bodyTop, body);
        }
        if (abs > 0.36 && abs < 0.67) {
            int armTop = top + (int)(height * 0.43);
            int armBottom = top + (int)(height * 0.80);
            fillRectClipped(x, armTop, COLUMN_WIDTH, armBottom - armTop, shade(body, 0.70));
        }
        if ((relative > -0.18 && relative < -0.08) || (relative > 0.08 && relative < 0.18)) {
            int eyeY = top + (int)(height * 0.23);
            fillRectClipped(x, eyeY, COLUMN_WIDTH, Math.max(2, height / 34), eye);
        }
    }

    private void drawWraithStripe(Demon demon, int x, int top, int height, double abs, double relative, double light, double fog)
    {
        double wave = Math.sin((demon.phase + x) * 0.08) * 0.05;
        abs = Math.abs(relative + wave);
        Color glow = blend(shade(new Color(104, 190, 210), light), new Color(13, 21, 25), fog);
        Color cloth = blend(shade(new Color(44, 67, 78), light), new Color(9, 13, 16), fog);
        Color face = demon.flashTimer > 0 ? new Color(236, 255, 249) : new Color(145, 238, 228);

        if (abs < 0.46) {
            double curve = Math.sqrt(1.0 - (abs / 0.46) * (abs / 0.46));
            int head = top + (int)(height * 0.22);
            int radius = (int)(height * 0.17 * curve);
            fillRectClipped(x, head - radius, COLUMN_WIDTH, radius * 2, glow);
        }
        if (abs < 0.58) {
            int bodyTop = top + (int)(height * 0.34);
            int bodyBottom = top + (int)(height * (0.92 - abs * 0.23 + Math.sin(demon.phase * 0.06) * 0.03));
            fillRectClipped(x, bodyTop, COLUMN_WIDTH, bodyBottom - bodyTop, cloth);
        }
        if (abs > 0.44 && abs < 0.80) {
            int veilTop = top + (int)(height * 0.48);
            int veilBottom = top + (int)(height * 0.85);
            fillRectClipped(x, veilTop, COLUMN_WIDTH, veilBottom - veilTop, shade(glow, 0.58));
        }
        if ((relative > -0.16 && relative < -0.06) || (relative > 0.06 && relative < 0.16)) {
            int eyeY = top + (int)(height * 0.23);
            fillRectClipped(x, eyeY, COLUMN_WIDTH, Math.max(2, height / 28), face);
        }
    }

    private void drawBruteStripe(Demon demon, int x, int top, int height, double abs, double relative, double light, double fog)
    {
        Color flesh = blend(shade(new Color(113, 30, 33), light), new Color(18, 15, 16), fog);
        Color muscle = blend(shade(new Color(72, 18, 24), light), new Color(10, 11, 12), fog);
        Color horn = blend(shade(new Color(194, 174, 124), light), new Color(18, 18, 16), fog);
        Color fire = demon.flashTimer > 0 ? new Color(255, 241, 140) : new Color(255, 74, 54);

        if (abs > 0.24 && abs < 0.76) {
            int hornTop = top + (int)(height * 0.03);
            int hornBottom = top + (int)(height * (0.20 + abs * 0.04));
            fillRectClipped(x, hornTop, COLUMN_WIDTH, hornBottom - hornTop, horn);
        }
        if (abs < 0.48) {
            double curve = Math.sqrt(1.0 - (abs / 0.48) * (abs / 0.48));
            int head = top + (int)(height * 0.24);
            int radius = (int)(height * 0.20 * curve);
            fillRectClipped(x, head - radius, COLUMN_WIDTH, radius * 2, flesh);
        }
        if (abs < 0.64) {
            int bodyTop = top + (int)(height * 0.38);
            int bodyBottom = top + (int)(height * (0.94 - abs * 0.08));
            fillRectClipped(x, bodyTop, COLUMN_WIDTH, bodyBottom - bodyTop, flesh);
        }
        if (abs > 0.50 && abs < 0.86) {
            int armTop = top + (int)(height * 0.42);
            int armBottom = top + (int)(height * 0.78);
            fillRectClipped(x, armTop, COLUMN_WIDTH, armBottom - armTop, muscle);
        }
        if ((relative > -0.21 && relative < -0.10) || (relative > 0.10 && relative < 0.21)) {
            int eyeY = top + (int)(height * 0.22);
            fillRectClipped(x, eyeY, COLUMN_WIDTH, Math.max(2, height / 30), fire);
        }
        if (abs < 0.20) {
            int mouthY = top + (int)(height * 0.31);
            fillRectClipped(x, mouthY, COLUMN_WIDTH, Math.max(2, height / 42), new Color(8, 5, 6));
        }
    }

    private void drawPickupStripe(HorrorPickup pickup, int x, int centerX, int top, int bottom, int width, double depth)
    {
        int height = bottom - top;
        if (height <= 0 || width <= 0) {
            return;
        }

        double relative = (x - centerX) / (width / 2.0);
        double abs = Math.abs(relative);
        double light = 0.80 + flashlightAt(x, depth);
        double fog = Math.min(0.70, depth * 0.045);

        if (pickup.type == HorrorPickup.GATE) {
            drawGateStripe(x, top, height, relative, abs, light, fog);
            return;
        }

        if (pickup.type == HorrorPickup.RELIC) {
            if (abs < 0.50) {
                double curve = Math.sqrt(1.0 - (abs / 0.50) * (abs / 0.50));
                int centerY = top + height / 2 + (int)(Math.sin(tick * 0.08) * 4);
                int radius = (int)(height * 0.32 * curve);
                Color relic = blend(shade(new Color(56, 226, 175), light), new Color(12, 20, 20), fog);
                fillRectClipped(x, centerY - radius, COLUMN_WIDTH, radius * 2, relic);
            }
            return;
        }

        if (abs > 0.78) {
            return;
        }

        int boxTop = top + (int)(height * 0.32);
        int boxBottom = top + (int)(height * 0.74);
        if (pickup.type == HorrorPickup.AMMO) {
            Color brass = blend(shade(new Color(184, 133, 48), light), new Color(21, 18, 13), fog);
            fillRectClipped(x, boxTop, COLUMN_WIDTH, boxBottom - boxTop, brass);
            if (abs < 0.18) {
                fillRectClipped(x, boxTop, COLUMN_WIDTH, boxBottom - boxTop, shade(brass, 0.55));
            }
        } else if (pickup.type == HorrorPickup.HEALTH) {
            Color cloth = blend(shade(new Color(213, 218, 204), light), new Color(20, 21, 19), fog);
            fillRectClipped(x, boxTop, COLUMN_WIDTH, boxBottom - boxTop, cloth);
            if (abs < 0.18 || (x % 8 < 4 && abs < 0.56)) {
                fillRectClipped(x, top + (int)(height * 0.45), COLUMN_WIDTH, Math.max(2, height / 8), new Color(178, 34, 45));
            }
        }
    }

    private void drawGateStripe(int x, int top, int height, double relative, double abs, double light, double fog)
    {
        if (abs > 0.68) {
            return;
        }
        if (doorTexture == null) {
            loadDoorTexture();
        }

        int sourceX = clampIndex((int)((relative + 1.0) * 0.5 * (doorTexture.getWidth() - 1)), doorTexture.getWidth());
        int drawTop = Math.max(0, top);
        int drawBottom = Math.min(SCREEN_H, top + height);
        int step = Math.max(1, height / DOOR_TEXTURE_SIZE);

        for (int y = drawTop; y < drawBottom; y += step) {
            double textureY = (y - top) / (double)Math.max(1, height);
            int sourceY = clampIndex((int)(textureY * (doorTexture.getHeight() - 1)), doorTexture.getHeight());
            Color sample = doorTexture.getColorAt(sourceX, sourceY);
            Color lit = blend(shade(sample, light), new Color(12, 12, 14), fog);
            fillRectClipped(x, y, COLUMN_WIDTH, step, lit);
        }

        if (abs > 0.61) {
            loadDoorTexture();
            Color edge = isGateOpen() ? new Color(53, 224, 143) : new Color(134, 38, 47);
            fillRectClipped(x, top, COLUMN_WIDTH, height, edge);
        }
    }

    private void drawRainAndFog()
    {
        int dropCount = Math.max(150, SCREEN_W * SCREEN_H / 3600);
        int fallLength = Math.max(14, SCREEN_H / 38);
        int wind = Math.max(3, SCREEN_W / 360);
        for (int i = 0; i < dropCount; i++) {
            int x = positiveModulo(i * 89 + tick * (4 + i % 3), SCREEN_W + 80) - 40;
            int y = positiveModulo(i * 47 + tick * (11 + i % 4), SCREEN_H + 90) - 45;
            int alpha = 34 + (i % 5) * 11;
            frame.setColor(new Color(92, 107, 118, alpha));
            frame.drawLine(x, y, x + wind, y + fallLength);
        }

        for (int y = HALF_H - 8; y < SCREEN_H - 70; y += 24) {
            int alpha = 13 + Math.max(0, 80 - Math.abs(y - HALF_H)) / 5;
            frame.setColor(new Color(91, 111, 106, clampColor(alpha)));
            frame.fillRect(0, y, SCREEN_W, 11);
        }
    }

    private void drawWeapon()
    {
        int baseY = SCREEN_H - 76 + (muzzleTimer > 0 ? 5 : 0);
        frame.setColor(new Color(56, 42, 36));
        frame.fillOval(SCREEN_W / 2 - 92, SCREEN_H - 48, 64, 42);
        frame.fillOval(SCREEN_W / 2 + 34, SCREEN_H - 48, 64, 42);

        frame.setColor(new Color(25, 26, 30));
        frame.fillRect(SCREEN_W / 2 - 20, baseY - 15, 40, 78);
        frame.setColor(new Color(82, 86, 88));
        frame.fillRect(SCREEN_W / 2 - 16, baseY - 43, 32, 34);
        frame.setColor(new Color(142, 150, 151));
        frame.drawLine(SCREEN_W / 2 - 13, baseY - 37, SCREEN_W / 2 + 13, baseY - 37);
        frame.setColor(new Color(11, 12, 14));
        frame.fillRect(SCREEN_W / 2 - 9, baseY - 52, 18, 13);

        if (muzzleTimer > 0 && player.ammo > 0) {
            frame.setColor(new Color(255, 235, 130, 210));
            frame.fillOval(SCREEN_W / 2 - 26, baseY - 88, 52, 54);
            frame.setColor(new Color(255, 113, 48));
            frame.fillOval(SCREEN_W / 2 - 13, baseY - 73, 26, 24);
        }
    }

    private void drawHud()
    {
        frame.setColor(new Color(9, 10, 12, 218));
        frame.fillRect(0, SCREEN_H - 40, SCREEN_W, 40);
        frame.setColor(new Color(50, 66, 61));
        frame.drawLine(0, SCREEN_H - 41, SCREEN_W, SCREEN_H - 41);

        drawBar(18, SCREEN_H - 28, 118, 10, player.health, 100, new Color(183, 38, 50));
        drawBar(158, SCREEN_H - 28, 104, 10, player.nerve, 100, new Color(67, 183, 158));
        drawBar(284, SCREEN_H - 28, 92, 10, player.ammo, 48, new Color(204, 149, 55));

        frame.setColor(new Color(220, 224, 214));
        frame.drawString("HP " + player.health, 18, SCREEN_H - 10);
        frame.drawString("NERVE " + player.nerve, 158, SCREEN_H - 10);
        frame.drawString("AMMO " + player.ammo, 284, SCREEN_H - 10);
        frame.drawString("DEMONS " + kills + "/" + totalDemons, 420, SCREEN_H - 10);

        if (isGateOpen()) {
            frame.setColor(new Color(79, 238, 158));
            frame.drawString("DOOR OPEN", 604, SCREEN_H - 10);
        } else {
            frame.setColor(new Color(155, 168, 158));
            frame.drawString("COLLECT " + collectedCollectables() + "/" + totalCollectables(), 604, SCREEN_H - 10);
        }

        int cx = SCREEN_W / 2;
        int cy = SCREEN_H / 2;
        frame.setColor(new Color(212, 229, 217, 180));
        frame.drawLine(cx - 11, cy, cx - 4, cy);
        frame.drawLine(cx + 4, cy, cx + 11, cy);
        frame.drawLine(cx, cy - 11, cx, cy - 4);
        frame.drawLine(cx, cy + 4, cx, cy + 11);
    }

    private void drawBuffBorder()
    {
        // 1. Вычисляем прозрачность (макс 140 из 255). 
        // За 30 тиков до конца эффекта рамка начнет плавно исчезать.
        int maxAlpha = 20;
        if (buffTimer < 30) {
            maxAlpha = (int)(maxAlpha * (buffTimer / 30.0));
        }
        
        // 2. Получаем Graphics2D для рисования поверх игрового кадра
        java.awt.Graphics2D g2d = frame.getAwtImage().createGraphics();
        
        // Включаем сглаживание углов
        g2d.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING, 
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON
        );

        // 3. Создаем геометрию рамки через класс Area
        // Сначала создаем прямоугольник размером со весь экран
        java.awt.geom.Area screenArea = new java.awt.geom.Area(
            new java.awt.Rectangle(0, 0, SCREEN_W, SCREEN_H)
        );

        // Настраиваем отступы для овала (чем больше отступ, тем толще рамка по краям)
        int offsetX = 10; // отступ слева и справа
        int offsetY = 10; // отступ сверху и снизу

        // Создаем овал, который мы хотим "вырезать" из центра
        java.awt.geom.Ellipse2D centerEllipse = new java.awt.geom.Ellipse2D.Double(
            offsetX, 
            offsetY, 
            SCREEN_W - (offsetX * 2), 
            SCREEN_H - (offsetY * 2)
        );
        java.awt.geom.Area ellipseArea = new java.awt.geom.Area(centerEllipse);

        // ВЫЧИТАЕМ овал из прямоугольника экрана. Остаются только края!
        screenArea.subtract(ellipseArea);

        int r=0,g=0, b =0;
        int lr=0,lg=0,lb=0;
        if (buffCurrent == AMMO_BUFF){
            r = 255; g = 210; b=0;
            lr = 255; lg = 225; lb = 180;
        }
        else if (buffCurrent == HP_BUFF){
            r = 247; g =13; b = 13;
            lr = 255; g = 12; b =0;
        }
        else if (buffCurrent == RELIC_BUFF){
            r = 0; g = 128; b = 242;
            lr=2; lg=195; lb=255;
        }
        // 4. Закрашиваем получившиеся края желтым цветом
        g2d.setColor(new java.awt.Color(r, g, b, clampColor(maxAlpha)));
        g2d.fill(screenArea);

        // 5. Добавляем красивый светящийся контур по границе выреза
        g2d.setColor(new java.awt.Color(lr, lg, lb, clampColor(maxAlpha / 2)));
        g2d.setStroke(new java.awt.BasicStroke(4.0f)); // Толщина линии свечения
        g2d.draw(centerEllipse);
        
        g2d.dispose();
    }

    private void drawMiniMap()
    {
        int scale = Math.max(5, Math.min(8, SCREEN_W / 180));
        int mapW = forest.width() * scale;
        int mapH = forest.height() * scale;
        int offsetX = SCREEN_W - mapW - 18;
        int offsetY = 18;

        frame.setColor(new Color(4, 6, 7, 210));
        frame.fillRect(offsetX - 5, offsetY - 5, mapW + 10, mapH + 10);
        frame.setColor(new Color(71, 91, 82, 180));
        frame.drawRect(offsetX - 5, offsetY - 5, mapW + 10, mapH + 10);

        for (int y = 0; y < forest.height(); y++) {
            for (int x = 0; x < forest.width(); x++) {
                int cell = forest.cellAt(x, y);
                if (cell == ForestMap.EMPTY) {
                    frame.setColor(new Color(16, 22, 21, 210));
                } else if (cell == ForestMap.STONE) {
                    frame.setColor(new Color(62, 74, 70, 230));
                } else if (cell == ForestMap.ROOTS) {
                    frame.setColor(new Color(55, 39, 30, 230));
                } else {
                    frame.setColor(new Color(34, 57, 39, 230));
                }
                frame.fillRect(offsetX + x * scale, offsetY + y * scale, scale, scale);
            }
        }

        for (int i = 0; i < pickups.size(); i++) {
            HorrorPickup pickup = pickups.get(i);
            if (pickup.taken) {
                continue;
            }
            if (pickup.type == HorrorPickup.GATE) {
                frame.setColor(isGateOpen() ? new Color(55, 238, 146) : new Color(132, 43, 54));
            } else if (pickup.type == HorrorPickup.RELIC) {
                frame.setColor(new Color(72, 224, 185));
            } else if (pickup.type == HorrorPickup.HEALTH) {
                frame.setColor(new Color(220, 220, 205));
            } else {
                frame.setColor(new Color(219, 165, 58));
            }
            frame.fillRect(offsetX + (int)(pickup.x * scale) - 2, offsetY + (int)(pickup.y * scale) - 2, 4, 4);
        }

        for (int i = 0; i < demons.size(); i++) {
            Demon demon = demons.get(i);
            if (!demon.isAlive()) {
                continue;
            }
            if (demon.type == Demon.PHOTO_BOSS) {
                frame.setColor(new Color(255, 35, 55));
                frame.fillOval(offsetX + (int)(demon.x * scale) - 4, offsetY + (int)(demon.y * scale) - 4, 8, 8);
            } else {
                frame.setColor(new Color(195, 43, 54));
                frame.fillOval(offsetX + (int)(demon.x * scale) - 3, offsetY + (int)(demon.y * scale) - 3, 6, 6);
            }

            if (demon.hasTarget) {
                frame.setColor(new Color(180, 58, 64, 120));
                frame.drawLine(
                    offsetX + (int)(demon.x * scale),
                    offsetY + (int)(demon.y * scale),
                    offsetX + demon.targetX * scale + scale / 2,
                    offsetY + demon.targetY * scale + scale / 2
                );
            }
        }

        int px = offsetX + (int)(player.x * scale);
        int py = offsetY + (int)(player.y * scale);
        frame.setColor(new Color(86, 196, 245));
        frame.fillOval(px - 4, py - 4, 8, 8);
        frame.drawLine(px, py, px + (int)(Math.cos(player.angle) * 13), py + (int)(Math.sin(player.angle) * 13));
    }

    private void drawBar(int x, int y, int width, int height, int value, int max, Color color)
    {
        int fill = Math.max(0, Math.min(width, value * width / Math.max(1, max)));
        frame.setColor(new Color(27, 30, 31));
        frame.fillRect(x, y, width, height);
        frame.setColor(color);
        frame.fillRect(x, y, fill, height);
        frame.setColor(new Color(113, 124, 119));
        frame.drawRect(x, y, width, height);
    }

    //Draw The oval of the flashlight
    // private void drawVignette()
    // {
    //     return;
    //     for (int i = 0; i < 72; i += 4) {
    //         int alpha = 9 + i / 4;
    //         frame.setColor(new Color(0, 0, 0, alpha));
    //         frame.drawRect(i, i, SCREEN_W - i * 2 - 1, SCREEN_H - i * 2 - 1);
    //     }
    //     int beamW = 290 + (int)(Math.sin(tick * 0.08) * 12);
    //     frame.setColor(new Color(122, 139, 122, 16));
    //     frame.fillOval(SCREEN_W / 2 - beamW / 2, HALF_H - 95, beamW, 215);
    // }

    private void drawFearOverlay()
    {
        if (hurtTimer > 0) {
            frame.setColor(new Color(127, 10, 20, 45));
            frame.fillRect(0, 0, SCREEN_W, SCREEN_H);
        }

        if (fearPulse > 0) {
            int alpha = Math.min(80, 18 + fearPulse * 2);
            frame.setColor(new Color(0, 0, 0, alpha));
            for (int i = 0; i < 11; i++) {
                int offset = i * 3 + (tick % 3);
                frame.drawRect(offset, offset, SCREEN_W - offset * 2 - 1, SCREEN_H - offset * 2 - 1);
            }
        }
    }

    private void drawBossOmenOverlay()
    {
        if (bossScreamerFace == null) {
            return;
        }

        double closeness = Math.max(0.0, Math.min(1.0, bossMusicVolume / 100.0));
        int alpha = Math.min(158, 34 + (int)(closeness * 124.0));
        int jitter = 4 + (int)(closeness * 14.0);
        int x = SCREEN_W / 2 - bossScreamerFace.getWidth() / 2 + noise(tick * 7, bossMusicVolume + 3) % (jitter * 2 + 1) - jitter;
        int y = HALF_H - bossScreamerFace.getHeight() / 2 + noise(tick * 11, bossMusicVolume + 9) % (jitter + 1) - jitter / 2;

        bossScreamerFace.setTransparency(alpha);
        frame.drawImage(bossScreamerFace, x, y);

        int shadowAlpha = Math.max(28, 72 - (int)(closeness * 20.0));
        frame.setColor(new Color(0, 0, 0, shadowAlpha));
        frame.fillRect(x, y, bossScreamerFace.getWidth(), bossScreamerFace.getHeight());

        if (closeness > 0.38) {
            frame.setColor(new Color(124, 12, 24, 24 + (int)(closeness * 34.0)));
            int slices = 2 + (int)(closeness * 4.0);
            for (int i = 0; i < slices; i++) {
                int lineY = y + positiveModulo(tick * 13 + i * 71, bossScreamerFace.getHeight());
                frame.drawLine(x, lineY, x + bossScreamerFace.getWidth(), lineY + noise(i, tick) % 7 - 3);
            }
        }
    }

    private void drawFinalBossScreamer()
    {
        if (bossFace == null) {
            frame.setColor(new Color(150, 0, 20, 180));
            frame.fillRect(0, 0, SCREEN_W, SCREEN_H);
            return;
        }

        double progress = 1.0 - finalScreamerTimer / (double)Math.max(1, finalScreamerLength);
        double pulse = Math.sin(tick * 0.72) * 0.18 + Math.sin(tick * 1.63) * 0.10;
        double scale = 1.06 + progress * 0.42 + pulse;
        int baseW = (int)(SCREEN_W * scale);
        int baseH = (int)(SCREEN_H * scale);
        GreenfootImage flash = new GreenfootImage(bossFace);
        flash.scale(baseW, baseH);

        int alpha = 205 + (finalScreamerTimer % 6 < 3 ? 42 : 0);
        if (finalScreamerTimer % 11 == 0) {
            alpha = 130;
        }
        flash.setTransparency(clampColor(alpha));

        int shake = 16 + (int)(progress * 58.0);
        int x = SCREEN_W / 2 - baseW / 2 + noise(tick * 13, finalScreamerTimer) % (shake * 2 + 1) - shake;
        int y = SCREEN_H / 2 - baseH / 2 + noise(tick * 17, finalScreamerTimer + 5) % (shake * 2 + 1) - shake;
        frame.drawImage(flash, x, y);

        if (finalScreamerTimer % 4 < 2) {
            GreenfootImage closeFlash = new GreenfootImage(bossFace);
            int closeW = (int)(SCREEN_W * (1.26 + progress * 0.38));
            int closeH = (int)(SCREEN_H * (1.26 + progress * 0.38));
            closeFlash.scale(closeW, closeH);
            closeFlash.setTransparency(72 + (int)(progress * 82));
            int closeX = SCREEN_W / 2 - closeW / 2 - shake / 2;
            int closeY = SCREEN_H / 2 - closeH / 2 + shake / 3;
            frame.drawImage(closeFlash, closeX, closeY);
        }

        frame.setColor(new Color(155, 0, 19, 90 + (int)(progress * 92)));
        frame.fillRect(0, 0, SCREEN_W, SCREEN_H);

        if (finalScreamerTimer % 7 < 3) {
            frame.setColor(new Color(255, 255, 238, 130));
            frame.fillRect(0, 0, SCREEN_W, SCREEN_H);
        }

        frame.setColor(new Color(0, 0, 0, 160));
        int borders = 34 + (int)(progress * 44);
        for (int i = 0; i < borders; i += 4) {
            frame.drawRect(i, i, SCREEN_W - i * 2 - 1, SCREEN_H - i * 2 - 1);
        }

        int slices = 16 + (int)(progress * 22);
        for (int i = 0; i < slices; i++) {
            int lineY = positiveModulo(tick * 29 + i * 47, SCREEN_H);
            int offset = noise(i * 19, tick) % (42 + (int)(progress * 70)) - 20;
            frame.setColor(new Color(i % 2 == 0 ? 230 : 20, i % 2 == 0 ? 230 : 0, i % 2 == 0 ? 220 : 0, 80));
            frame.drawLine(0, lineY, SCREEN_W, lineY + offset);
        }
    }

    private void drawEndOverlay()
    {
        int boxW = 360;
        int boxH = 96;
        int x = SCREEN_W / 2 - boxW / 2;
        int y = SCREEN_H / 2 - boxH / 2;
        frame.setColor(new Color(5, 6, 7, 230));
        frame.fillRect(x, y, boxW, boxH);
        frame.setColor(won ? new Color(72, 231, 151) : new Color(185, 33, 49));
        frame.drawRect(x, y, boxW, boxH);
        frame.drawRect(x + 2, y + 2, boxW - 4, boxH - 4);
        frame.setColor(new Color(226, 229, 216));
        frame.drawString(won ? "THE FOREST LETS YOU GO" : "THE FOREST TAKES YOU", x + 104, y + 36);
        frame.drawString("PRESS R TO RETURN", x + 126, y + 64);
    }

    private boolean isGateOpen()
    {
        return collectedCollectables() >= totalCollectables();
    }

    private int collectedCollectables()
    {
        int collected = 0;
        for (int i = 0; i < pickups.size(); i++) {
            HorrorPickup pickup = pickups.get(i);
            if (isCollectable(pickup) && pickup.taken) {
                collected++;
            }
        }
        return collected;
    }

    private int totalCollectables()
    {
        int total = 0;
        for (int i = 0; i < pickups.size(); i++) {
            if (isCollectable(pickups.get(i))) {
                total++;
            }
        }
        return total;
    }

    private boolean isCollectable(HorrorPickup pickup)
    {
        return pickup.type != HorrorPickup.GATE;
    }

    private boolean lineClear(double ax, double ay, double bx, double by)
    {
        double dx = bx - ax;
        double dy = by - ay;
        double distance = Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int)(distance / 0.045));
        for (int i = 1; i < steps; i++) {
            double t = i / (double)steps;
            if (forest.isSolidAt(ax + dx * t, ay + dy * t)) {
                return false;
            }
        }
        return true;
    }

    private double flashlightAt(int screenX, double distance)
    {
        double center = Math.abs(screenX - SCREEN_W / 2.0) / (SCREEN_W / 2.0);
        double cone = Math.max(0.0, 1.0 - center * 1.55);
        double range = Math.max(0.0, 1.0 - distance / 15.0);
        double flicker = 0.92 + Math.sin(tick * 0.17) * 0.04 + Math.sin(tick * 0.031) * 0.035;
        return cone * cone * range * flicker;
    }

    //flash light
    private int lightningIntensity()
    {
        if (lightningTimer <= 0) {
            return 0;
        }

        // start with a base glow and add more based on the timer for a fade-out effect
        return 36 + lightningTimer * 9;
    }

    private Color wallBaseColor(int cell)
    {
        if (cell == ForestMap.ANCIENT_TREE) {
            return new Color(66, 48, 36);
        }
        if (cell == ForestMap.STONE) {
            return new Color(64, 75, 72);
        }
        if (cell == ForestMap.ROOTS) {
            return new Color(45, 34, 26);
        }
        return new Color(35, 54, 39);
    }

    private void fillRectClipped(int x, int y, int width, int height, Color color)
    {
        if (height <= 0 || width <= 0 || x >= SCREEN_W || x + width <= 0) {
            return;
        }
        int startY = Math.max(0, y);
        int endY = Math.min(SCREEN_H - 1, y + height);
        if (endY <= startY) {
            return;
        }
        int startX = Math.max(0, x);
        int endX = Math.min(SCREEN_W, x + width);
        frame.setColor(color);
        frame.fillRect(startX, startY, endX - startX, endY - startY);
    }

    private Color shade(Color color, double amount)
    {
        return new Color(
            clampColor((int)(color.getRed() * amount)),
            clampColor((int)(color.getGreen() * amount)),
            clampColor((int)(color.getBlue() * amount)),
            color.getAlpha()
        );
    }

    private Color blend(Color a, Color b, double amount)
    {
        amount = Math.max(0.0, Math.min(1.0, amount));
        double inv = 1.0 - amount;
        return new Color(
            clampColor((int)(a.getRed() * inv + b.getRed() * amount)),
            clampColor((int)(a.getGreen() * inv + b.getGreen() * amount)),
            clampColor((int)(a.getBlue() * inv + b.getBlue() * amount)),
            clampColor((int)(a.getAlpha() * inv + b.getAlpha() * amount))
        );
    }

    private int clampColor(int value)
    {
        return Math.max(0, Math.min(255, value));
    }

    private int clampIndex(int value, int size)
    {
        return Math.max(0, Math.min(size - 1, value));
    }

    private int positiveModulo(int value, int mod)
    {
        int result = value % mod;
        if (result < 0) {
            result += mod;
        }
        return result;
    }

    private int noise(int x, int y)
    {
        int n = x * 374761393 + y * 668265263;
        n = (n ^ (n >> 13)) * 1274126177;
        return (n ^ (n >> 16)) & 255;
    }

    private double normalizeAngle(double angle)
    {
        while (angle <= -Math.PI) {
            angle += TWO_PI;
        }
        while (angle > Math.PI) {
            angle -= TWO_PI;
        }
        return angle;
    }
}
