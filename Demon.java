public class Demon
{
    public static final int STALKER = 1;
    public static final int WRAITH = 2;
    public static final int BRUTE = 3;
    public static final int PHOTO_BOSS = 4;

    double x;
    double y;
    double moveAngle;
    int type;
    int health;
    int maxHealth;
    int attackCooldown;
    int wanderTimer;
    int flashTimer;
    int phase;
    int targetX;
    int targetY;
    int repathTimer;
    boolean hasTarget;
    boolean alert;

    public Demon(double x, double y, int type)
    {
        this.x = x;
        this.y = y;
        this.type = type;
        this.maxHealth = maxHealthFor(type);
        this.health = maxHealth;
        this.moveAngle = 0.0;
        this.attackCooldown = 0;
        this.wanderTimer = 0;
        this.flashTimer = 0;
        this.phase = 0;
        this.targetX = (int)x;
        this.targetY = (int)y;
        this.repathTimer = 0;
        this.hasTarget = false;
        this.alert = false;
    }

    public Demon(Demon other)
    {
        this(other.x, other.y, other.type);
    }

    public boolean isAlive()
    {
        return health > 0;
    }

    public double speed()
    {
        if (type == PHOTO_BOSS) {
            return alert ? 0.040 : 0.020;
        }
        if (type == WRAITH) {
            return alert ? 0.025 : 0.015;
        }
        if (type == BRUTE) {
            return alert ? 0.020 : 0.020;
        }
        return alert ? 0.030 : 0.030;
    }

    public double radius()
    {
        if (type == PHOTO_BOSS) {
            return 0.24;
        }
        return type == BRUTE ? 0.29 : 0.22;
    }

    public double attackRange()
    {
        if (type == PHOTO_BOSS) {
            return 1.05;
        }
        return type == BRUTE ? 0.82 : 0.68;
    }

    public int damage()
    {
        if (type == PHOTO_BOSS) {
            return 24;
        }
        if (type == WRAITH) {
            return 9;
        }
        if (type == BRUTE) {
            return 17;
        }
        return 12;
    }

    public int scoreDamage(int baseDamage)
    {
        if (type == PHOTO_BOSS) {
            return baseDamage - 18;
        }
        if (type == BRUTE) {
            return baseDamage - 8;
        }
        if (type == WRAITH) {
            return baseDamage + 8;
        }
        return baseDamage;
    }

    private int maxHealthFor(int type)
    {
        if (type == PHOTO_BOSS) {
            return 360;
        }
        if (type == WRAITH) {
            return 72;
        }
        if (type == BRUTE) {
            return 168;
        }
        return 104;
    }
}
