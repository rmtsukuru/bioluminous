import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.List; 
import java.util.LinkedList; 
import java.util.ListIterator; 
import processing.sound.*; 
import java.util.Set; 
import java.util.HashSet; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Gather extends PApplet {





static final int SCREEN_WIDTH = 640;
static final int SCREEN_HEIGHT = 480;
static final int FPS = 60;
static final int ENEMY_SPAWN_RATE = 2;
static final float BASE_ENEMY_SPAWN_CHANCE = 0.2f;
static final float ENEMY_SPAWN_CHANCE_MINUTE_STEP = 0.2f;
static final int POWERUP_SPAWN_CAP = 16;
static final float POWERUP_SPAWN_CHANCE = 0.6f;
static final int DEATH_TIMER_FRAMES = (int) (0.45f * FPS);
static final int WIN_TIMER_FRAMES = (int) (1.5f * FPS);

Screen screen;

Player player;

int counter = 0;
int deathTimer = DEATH_TIMER_FRAMES;
int winTimer = WIN_TIMER_FRAMES;
float healthBarTop;

static Gather instance;

public void settings() {
  size(SCREEN_WIDTH, SCREEN_HEIGHT);
}

public void setup() {
  frameRate(FPS);
  
  Gather.instance = this;
  
  Graphics.configure(this);
  Audio.configure(this);
  Input.configure(this);
  Level.configure(this);
  
  screen = new GameScreen();
  
  spawnPowerups();
  Artifact artifact = new Artifact();
  artifact.spawn();
  Level.addEntity(artifact);
  
  player = new Player(30, 100);
  healthBarTop = (float) player.health / Player.MAX_HP;
  Level.addEntity(player);
}

public void draw() {
  if (screen instanceof GameScreen && player.health <= 0) {
    screen = new DeathScreen();
  } 
  else if (screen instanceof GameScreen && player.hasArtifact && (player.x == 0 || player.x + player.width == Level.mapWidth())) {
    screen = new WinScreen();
  }
  
  screen.draw();
}
class Actor extends Entity {
  
  boolean goingUp, goingDown, goingLeft, goingRight;
  
  boolean facingRight;
  boolean onGround;
  boolean jumping;
  
  float speed;
  float xVelocity;
  float yVelocity;
  
  int jumpTimer;
  int jumpMax;
  
  Actor() {
    this(0, 0);
  }
  
  Actor(float x, float y) {
    super(x, y);
    speed = 1;
    
    facingRight = false;
    onGround = false;
    jumping = false;
  }
  
  public boolean hasGravity() {
    return true;
  }
  
  public void setVelocity() {
    xVelocity = 0;
    if (goingLeft) {
      xVelocity -= speed;
      if (!goingRight) {
        facingRight = false;
      }
    }
    if (goingRight) {
      xVelocity += speed;
      if (!goingLeft) {
        facingRight = true;
      }
    }
    
    if (hasGravity()) {
      if (jumping && onGround) {
        onGround = false;
        jumpTimer = jumpMax;
      }
      
      if (jumpTimer > 0 && !onGround && jumping) {
        jumpTimer--;
        applyGravity(false);
      }
      else {
        jumpTimer = 0;
        applyGravity();
      }
    }
    else {
      yVelocity = 0;
      if (goingUp) {
        yVelocity -= speed;
      }
      if (goingDown) {
        yVelocity += speed;
      }
    }
  }
  
  public void applyGravity() {
    applyGravity(true);
  }
  
  public void applyGravity(boolean down) {
    if (down) {
      onGround = false;
      if (yVelocity < speed * 2) {
        yVelocity += speed / 10;
      }
    }
    else {
      if (yVelocity > speed * -2) {
        yVelocity -= speed / 5;
      }
    }
  }
  
  public void update() {
    setVelocity();
    x += xVelocity;
    y += yVelocity;
  }
  
  public void handleTileCollision() {
    // Do nothing, this is for overriding.
  }
  
  public void handleEntityCollision(Entity other) {
    // Do nothing, this is also for overriding.
  }
}


static class Audio {
  
  static final boolean MUTE = false;
  static final float VOLUME = 0.5f;
  
  static PApplet parent;
  
  public static void configure(PApplet parent) {
    Audio.parent = parent;
  }
  
  public static void play(String filename) {
    if (!MUTE) {
      SoundFile file = new SoundFile(parent, "audio/" + filename);
      file.play();
      file.amp(VOLUME);
    }
  }
}
class Bullet extends PlayerAttack {
  
  static final int WIDTH = 16;
  static final int HEIGHT = 8;
  static final int SPEED = 18;
  static final int DAMAGE = 30;
  
  final int COLOR = color(200, 150, 0);
  
  Bullet() {
    this(false);
  }
  
  Bullet(boolean facingRight) {
    super();
    this.facingRight = facingRight;
    if (facingRight) {
      goingRight = true;
    }
    else {
      goingLeft = true;
    }
    
    this.width = WIDTH;
    this.height = HEIGHT;
    this.fillColor = COLOR;
    this.speed = SPEED;
  }
  
  public int getDamage(Entity entity) {
    if (this.deleted) {
      return 0;
    }
    
    this.delete();
    return DAMAGE;
  }
  
  public boolean hasGravity() {
    return false;
  }
  
  public void setVelocity() {
    super.setVelocity();
    Level.handleTileCollision(this);
  }
  
  public void update() {
    super.update();
  }
  
  public void handleTileCollision() {
    this.delete();
  }
  
  public void handleEntityCollision(Entity other) {
    if (other instanceof Enemy) {
      this.delete();
    }
  }
}
class Enemy extends Actor {

  final int MAX_HP_BASE = 40;
  final int MAX_HP_INCREMENT = 10;
  final int HP_BAR_COLOR = color(230, 20, 20);
  final int ATTACK_DAMAGE = 10;
  static final int JUMP_TIMER_FRAMES = (int) (0.2f * FPS);
  static final float SPEED = 1;
  static final float ARTIFACT_SPEED_MODIFIER = 3;
  
  int health, maxHealth;
  
  public Enemy() {
    this(0, 0);
  }
  
  public Enemy(float x, float y) {
    super(x, y);
    this.jumpMax = JUMP_TIMER_FRAMES;
    
    this.fillColor = color(10, 10, 10);
    setMaxHealth();
    health = maxHealth;
  }
  
  public void setMaxHealth() {
    float x = (float) Math.random();
    maxHealth = MAX_HP_BASE;
    if (x >= 0.1f) {
      maxHealth += MAX_HP_INCREMENT;
    }
    if (x >= 0.5f) {
      maxHealth += MAX_HP_INCREMENT;
    }
    if (x >= 0.9f) {
      maxHealth += MAX_HP_INCREMENT;
    }
  }
  
  public int getDamage() {
    return ATTACK_DAMAGE;
  }
  
  public void setSpeed() {
    if (player.hasArtifact) {
      speed = SPEED * ARTIFACT_SPEED_MODIFIER;
    }
    else {
      speed = SPEED;
    }
  }
  
  public void setVelocity() {
    this.goingLeft = false;
    this.goingRight = false;
    if (player.x + player.width/2 < x) {
      this.goingLeft = true;
    }
    else if (player.x + player.width/2 > x + width) {
      this.goingRight = true;
    }
    setSpeed();
    super.setVelocity();
    float tempX = this.xVelocity;
    Level.handleTileCollision(this);
    this.jumping = abs(tempX) > abs(this.xVelocity);
  }
  
  public void update() {
    super.update();
    Level.handleEntityCollision(this);
  }
  
  public void handleEntityCollision(Entity other) {
    if (other instanceof PlayerAttack) {
      PlayerAttack attack = (PlayerAttack) other;
      health -= attack.getDamage(this);
      if (health <= 0) {
        this.delete();
      }
    }
  }
  
  public void draw() {
    super.draw();
    if (health < maxHealth) {
      Graphics.drawBar((int) x - 8, (int) y - 18, width + 16, 13, (float) health / maxHealth, HP_BAR_COLOR);
    }
  }
}
class Entity {
  float x, y;
  int width, height, borderRadius;
  int strokeColor;
  int fillColor;
  boolean deleted;
  
  Entity() {
    this(0, 0);
  }
  
  Entity(float x, float y) {
    this(x, y, 180, 255);
  }
  
  Entity(float x, float y, int strokeColor, int fillColor) {
    this(x, y, strokeColor, fillColor, 32);
  }
  
  Entity(float x, float y, int strokeColor, int fillColor, int size) {
    this.x = x;
    this.y = y;
    width = height = size;
    this.strokeColor = strokeColor;
    this.fillColor = fillColor;
    this.deleted = false;
  }
  
  public void update() {
    // Do literally nothing.
  }
  
  public void draw() {
    stroke(strokeColor);
    fill(fillColor);
    Graphics.drawRect(x, y, width, height, borderRadius);
  }
  
  public void delete() {
    deleted = true;
  }
}
static class Graphics {
  static final float BAR_DECAY_RATE = 0.5f / FPS;
  
  static PApplet parent;
  
  static float cameraX;
  static float cameraY;
  
  public static void configure(PApplet parent) {
    Graphics.parent = parent;
    
    cameraX = 0;
    cameraY = 0;
  }
  
  public static void update(Player player) {
    cameraX = player.x - Gather.SCREEN_WIDTH / 2;
    cameraY = player.y - Gather.SCREEN_HEIGHT / 2;
    
    if (cameraX < 0) {
      cameraX = 0;
    }
    else if (cameraX + Gather.SCREEN_WIDTH > Level.mapWidth()) {
      cameraX = Level.mapWidth() - Gather.SCREEN_WIDTH;
    }
    if (cameraY < 0) {
      cameraY = 0;
    }
    else if (cameraY + Gather.SCREEN_HEIGHT > Level.mapHeight()) {
      cameraY = Level.mapHeight() - Gather.SCREEN_HEIGHT;
    }
  }
  
  public static void drawRect(float x, float y, float width, float height) {
    drawRect(x, y, width, height, 0);
  }
  
  public static void drawRect(float x, float y, float width, float height, float borderRadius) {
    parent.rect(x - cameraX, y - cameraY, width, height, borderRadius);
  }
  
  public static void drawBar(int x, int y, int width, int height, float fillRatio, int fillColor) {
    parent.stroke(255);
    parent.fill(0);
    drawRect(x, y, width, height);
    parent.noStroke();
    parent.fill(fillColor);
    float boundedRatio = max(0, min(fillRatio, 1));
    drawRect(x + 2, y + 2, (width - 3) * boundedRatio, height - 3);
  }
  
  public static void drawDecayingBar(int x, int y, int width, int height, float fillRatio, int fillColor, float decayRatio, int decayColor) {
    parent.stroke(255);
    parent.fill(0);
    parent.rect(x, y, width, height);
    parent.noStroke();
    parent.fill(decayColor);
    float boundedRatio = max(0, min(decayRatio, 1));
    parent.rect(x + 2, y + 2, (width - 3) * boundedRatio, height - 3);
    boundedRatio = max(0, min(fillRatio, 1));
    parent.fill(fillColor);
    parent.rect(x + 2, y + 2, (width - 3) * boundedRatio, height - 3);
  }
}
static class Input {

  static class KeyState {
    boolean beingHeld, pressed, released;
  
    KeyState() {
      beingHeld = false;
      pressed = false;
      released = false;
    }
  }
  
  static PApplet parent;

  static ArrayList<Object> keys = new ArrayList();
  static HashMap<Object, KeyState> keyState = new HashMap();

  public static void configure(PApplet parent) {
    Input.parent = parent;
    
    keys.add('z');
    keys.add('x');
    keys.add(' ');
    keys.add(UP);
    keys.add(DOWN);
    keys.add(LEFT);
    keys.add(RIGHT);
  
    for (Object k : keys) {
      keyState.put(k, new KeyState());
    }
  }

  public static boolean isKey(Object key) {
    if (key instanceof Integer) {
      return isKey((int) key);
    }
    return isKey((char) key);
  }
  
  public static boolean isKey(char key) {
    return parent.key != CODED && parent.key == key;
  }
  
  public static boolean isKey(int key) {
    return parent.key == CODED && parent.keyCode == key;
  }
  
  public static boolean holdKey(Object key) {
    return keyState.get(key).beingHeld;
  }
  
  public static boolean pressKey(Object key) {
    return keyState.get(key).pressed;
  }
  
  public static boolean releaseKey(Object key) {
    return keyState.get(key).released;
  }
  
  public static void resetKeys() {
    for (Object k : keys) {
      KeyState state = keyState.get(k);
      
      state.pressed = false;
      state.released = false;
    }
  }
  
  public static void keyPressed() {
    for (Object k : keys) {
      if (isKey(k)) {
        KeyState state = keyState.get(k);
        
        if (!state.beingHeld) {
          state.pressed = true;
        }
        state.beingHeld = true;
      }
    }
  }
  
  public static void keyReleased() {
    for (Object k : keys) {
      if (isKey(k)) {
        KeyState state = keyState.get(k);
        
        state.released = true;
        state.beingHeld = false;
      }
    }
  }
}

public void keyPressed() {
  Input.keyPressed();
}

public void keyReleased() {
  Input.keyReleased();
}
static class Level {
  
  static final int TILE_SIZE = 32;

  static int[][] tiles;

  static List<Entity> entities;
  static List<Entity> newEntities;
  
  static PApplet parent;
  
  public static void loadMap(String filename) {
    List<String> lines = new LinkedList();
    BufferedReader reader = parent.createReader("levels/" + filename);
    try {
      String currentLine = reader.readLine();
      while(currentLine != null) {
        lines.add(currentLine);
        currentLine = reader.readLine();
      }
      reader.close();
    }
    catch (IOException e) {
      println("Failed to load map data, terminating.");
      e.printStackTrace();
      System.exit(-1);
    }
    tiles = new int[lines.size()][];
    for (int i = 0; i < lines.size(); i++) {
      char[] temp = lines.get(i).toCharArray();
      tiles[i] = new int[temp.length];
      for (int j = 0; j < temp.length; j++) {
        tiles[i][j] = Integer.parseInt("" + temp[j]);
      }
    }
  }

  public static void configure(PApplet parent) {
    Level.parent = parent;
    
    loadMap("map01");
    
    entities = new LinkedList();
    newEntities = new LinkedList();
  }

  public static void addEntity(Entity entity) {
    newEntities.add(entity);
  }
  
  public static void drawTiles() {
    int minX = gridIndex(Graphics.cameraX);
    int maxX = gridIndex(Graphics.cameraX + SCREEN_WIDTH);
    int minY = gridIndex(Graphics.cameraY);
    int maxY = gridIndex(Graphics.cameraY + SCREEN_HEIGHT);
    for (int i = minY; i <= maxY; i++) {
      for (int j = minX; j <= maxX; j++) {
        if (i < tiles.length && j < tiles[i].length && tiles[i][j] == 1) {
          parent.stroke(155, 120, 120);
          parent.fill(70, 0, 150);
          Graphics.drawRect(j * TILE_SIZE, i * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        }
      }
    }
  }
  
  public static int mapWidth() {
    return TILE_SIZE * tiles[0].length;
  }
  
  public static int mapHeight() {
    return TILE_SIZE * tiles.length;
  }

  public static int gridIndex(float x) {
    return (int) Math.floor(x / TILE_SIZE);
  }

  public static boolean isPassableTile(int gridX, int gridY) {
    if (gridX < 0 || gridX >= tiles[0].length || gridY < 0 || gridY >= tiles.length) {
      return false;
    }
  
    return tiles[gridY][gridX] == 0;
  }

  public static boolean isPassable(float x, float y) {
    int gridX = gridIndex(x);
    int gridY = gridIndex(y);
  
    return isPassableTile(gridX, gridY);
  }
  
  public static int scalarGridIndex(int x, int y) {
    return x + (y * tiles[0].length);
  }
  
  public static int gridXFromScalar(int i) {
    return i % tiles[0].length;
  }
  
  public static int gridYFromScalar(int i) {
    return i / tiles[0].length;
  }

  public static int tileLeft(int gridX, int gridY) {
    return (gridX * TILE_SIZE);
  }

  public static int tileRight(int gridX, int gridY) {
    return (gridX * TILE_SIZE) + TILE_SIZE;
  }

  public static int tileTop(int gridX, int gridY) {
    return (gridY * TILE_SIZE);
  }

  public static int tileBottom(int gridX, int gridY) {
    return (gridY * TILE_SIZE) + TILE_SIZE;
  }
  
  public static Entity setRandomSpawnPosition(Entity subject, int minTileX, int maxTileX, int minTileY, int maxTileY) {
    List<Integer> passableTiles = new ArrayList();
    for (int i = minTileX; i <= maxTileX; i++) {
      for (int j = minTileY; j <= maxTileY; j++) {
        if (isPassableTile(i, j)) {
          passableTiles.add(scalarGridIndex(i, j));
        }
      }
    }
    int randomTile = Math.round((float) Math.random() * passableTiles.size());
    int scalar = passableTiles.get(min(randomTile, passableTiles.size()-1));
    subject.x = tileLeft(gridXFromScalar(scalar), gridYFromScalar(scalar));
    subject.y = tileTop(gridXFromScalar(scalar), gridYFromScalar(scalar));
    return subject;
  }
  
  public static Entity setRandomSpawnPosition(Entity subject, Player player) {
    int minTileX = gridIndex(max(0, player.x - Gather.SCREEN_WIDTH / 2));
    int maxTileX = gridIndex(min(mapWidth(), player.x + Gather.SCREEN_WIDTH / 2));
    int minTileY = gridIndex(max(0, player.y - Gather.SCREEN_HEIGHT / 2));
    int maxTileY = gridIndex(min(mapHeight(), player.y + Gather.SCREEN_HEIGHT / 2));
    
    return setRandomSpawnPosition(subject, minTileX, maxTileX, minTileY, maxTileY);
  }
  
  public static Entity setRandomSpawnPosition(Entity subject) {
    return setRandomSpawnPosition(subject, 0, (mapWidth() / TILE_SIZE) - 1, 0, (mapHeight() / TILE_SIZE) - 1);
  }

  public static boolean areColliding(Entity a, Entity b) {
    return a.x < b.x + b.width && b.x < a.x + a.width && a.y < b.y + b.height && b.y < a.y + a.height;
  }

  public static float getCollisionVelocity(boolean horizontal, Actor actor) {
    if (horizontal) {
      // Horizontal
      if (actor.xVelocity < 0) {
        // Left
        int minTileX = gridIndex(actor.x + actor.xVelocity);
        int maxTileX = gridIndex(actor.x + 1) - 1;
        int minTileY = gridIndex(actor.y);
        int maxTileY = gridIndex(actor.y + actor.height - 1);
        
        for (int i = maxTileX; i >= minTileX; i--) {
          for (int j = minTileY; j <= maxTileY; j++) {
            if (!isPassableTile(i, j)) {
              return max(tileRight(i, j) - actor.x, actor.xVelocity);
            }
          }
        }
      }
      else if (actor.xVelocity > 0) {
        // Right
        int minTileX = gridIndex(actor.x + actor.width - 1) + 1;
        int maxTileX = gridIndex(actor.x + actor.width + actor.xVelocity);
        int minTileY = gridIndex(actor.y);
        int maxTileY = gridIndex(actor.y + actor.height - 1);
        
        for (int i = minTileX; i <= maxTileX; i++) {
          for (int j = minTileY; j <= maxTileY; j++) {
            if (!isPassableTile(i, j)) {
              return min(tileLeft(i, j) - (actor.x + actor.width), actor.xVelocity);
            }
          }
        }
      }
      return actor.xVelocity;
    }
    else {
      // Vertical
      float tempX = actor.x + actor.xVelocity;
      
      if (actor.yVelocity < 0) {
        // Top
        int minTileY = gridIndex(actor.y + actor.yVelocity);
        int maxTileY = gridIndex(actor.y) - 1;
        int minTileX = gridIndex(tempX);
        int maxTileX = gridIndex(tempX + actor.width - 1);
        
        for (int j = maxTileY; j >= minTileY; j--) {
          for (int i = minTileX; i <= maxTileX; i++) {
            if (!isPassableTile(i, j)) {
              return max(tileBottom(i, j) - actor.y, actor.yVelocity);
            }
          }
        }
      }
      else if (actor.yVelocity > 0) {
        // Bottom
        int minTileY = gridIndex(actor.y + actor.height - 1) + 1;
        int maxTileY = gridIndex(actor.y + actor.height + actor.yVelocity);
        int minTileX = gridIndex(tempX);
        int maxTileX = gridIndex(tempX + actor.width - 1);
        
        for (int j = minTileY; j <= maxTileY; j++) {
          for (int i = minTileX; i <= maxTileX; i++) {
            if (!isPassableTile(i, j)) {
              if (!actor.goingUp) {
                actor.onGround = true;
                actor.jumping = false;
              }
              return min(tileTop(i, j) - (actor.y + actor.height), actor.yVelocity);
            }
          }
        }
      }
      return actor.yVelocity;  
    }
  }
  
  public static void handleTileCollision(Actor actor) {
    float startXVelocity = actor.xVelocity;
    float startYVelocity = actor.yVelocity;
    actor.xVelocity = getCollisionVelocity(true, actor);
    actor.yVelocity = getCollisionVelocity(false, actor);
    
    if (startXVelocity != actor.xVelocity || startYVelocity != actor.yVelocity) {
      actor.handleTileCollision();
    }
  }
  
  public static void handleEntityCollision(Actor actor) {
    for (Entity entity : entities) {
      if (areColliding(actor, entity)) {
        actor.handleEntityCollision(entity);
        if (!(entity instanceof Enemy)) {
          Actor other = (Actor) entity;
          other.handleEntityCollision(actor);
        }
      }
    }
  }
}
class Player extends Actor {
  
  static final int WIDTH = 27;
  static final int HEIGHT = 60;
  static final int RADIUS = 7;
  static final float SPEED = 5;
  static final int GUN_HEIGHT = 21;
  static final int BLADE_HEIGHT = 28;
  
  final int HP_BAR_COLOR = color(50, 180, 120);
  final int HP_DECAY_COLOR = color(255, 0, 0);
  static final int MAX_HP = 100;
  static final int MAX_LOADED_BULLETS = 6;
  static final int STARTING_BULLETS = 6;
  
  static final int JUMP_TIMER_FRAMES = (int) (0.18f * FPS);
  static final int DAMAGE_TIMER_FRAMES = (int) (0.8f * FPS);
  static final int RELOAD_TIMER_FRAMES = (int) (0.6f * FPS);
  
  final int BORDER_COLOR = color(255, 230, 230);
  final int COLOR = color(0, 200, 250);
  
  int health;
  int bullets;
  int reserveBullets;
  boolean hasArtifact;
  
  int damageTimer;
  int reloadTimer;
  boolean swordDrawn;
  Set<Entity> hitEnemies;
  
  Player() {
    this(0, 0);
  }
  
  Player(int x, int y) {
    this.x = x;
    this.y = y;
    this.width = WIDTH;
    this.height = HEIGHT;
    this.speed = SPEED;
    this.strokeColor = BORDER_COLOR;
    this.fillColor = COLOR;
    this.borderRadius = RADIUS;
    this.jumpTimer = 0;
    this.jumpMax = JUMP_TIMER_FRAMES;
    
    this.health = MAX_HP;
    this.bullets = MAX_LOADED_BULLETS;
    this.reserveBullets = STARTING_BULLETS;
    this.hasArtifact = false;
    this.damageTimer = 0;
    this.swordDrawn = false;
    this.hitEnemies = new HashSet();
  }
  
  public void setVelocity() {
    super.setVelocity();
    Level.handleTileCollision(this);
  }
  
  public void update() {
    if (health > MAX_HP) {
      health = MAX_HP;
    }
    if (swordDrawn) {
      this.goingLeft = this.goingRight = this.goingUp = this.goingDown = this.jumping = false;
    }
    else {
      this.goingLeft = Input.holdKey(LEFT);
      this.goingRight = Input.holdKey(RIGHT);
      this.goingDown = Input.holdKey(DOWN);
      this.goingUp = Input.holdKey(UP);
      
      if (Input.pressKey(' ')) {
        this.jumping = true;
      }
      else if (Input.releaseKey(' ')) {
        this.jumping = false;
      }
    }
    
    if (reloadTimer > 0) {
      reloadTimer--;
    }
    
    if (Input.pressKey('z')) {
      if (bullets > 0) {
        bullets--;
        Bullet bullet = new Bullet(facingRight);
        bullet.y = y + GUN_HEIGHT;
        if (facingRight) {
          bullet.x = x + this.width;
        }
        else {
          bullet.x = x;
        }
        Audio.play("shoot00.wav");
        
        Level.addEntity(bullet);
        if (bullets == 0) {
          reloadTimer = RELOAD_TIMER_FRAMES;
        }
      }
      else if(reloadTimer == 0 && reserveBullets > 0) {
        int reloadCount = min(6, reserveBullets);
        reserveBullets -= reloadCount;
        bullets = reloadCount;
        Audio.play("click01.wav");
      }
      else {
        Audio.play("click00.wav");
        reloadTimer = RELOAD_TIMER_FRAMES;
      }
    }
    
    if (Input.pressKey('x') && !swordDrawn) {
      Sword sword = new Sword(this);
      swordDrawn = true;
      Audio.play("slice00.wav");
      
      Level.addEntity(sword);
    }
    
    if (damageTimer > 0) {
      damageTimer--;
    }
    else {
      hitEnemies.clear();
    }
    
    super.update();
    Input.resetKeys();
    Level.handleEntityCollision(this);
  }
  
  public void handleEntityCollision(Entity other) {
    if (other instanceof Enemy && !hitEnemies.contains(other)) {
      if (damageTimer == 0) {
        damageTimer = DAMAGE_TIMER_FRAMES;
      }
      hitEnemies.add(other);
      Enemy enemy = (Enemy) other;
      health -= enemy.getDamage();
      Audio.play("hit00.wav");
    }
  }
}
abstract class PlayerAttack extends Actor {
  
  public abstract int getDamage(Entity entity);
  
  public void update() {
    super.update();
  }
}
public void spawnPowerups() {
  for (int i = 0; i < POWERUP_SPAWN_CAP; i++) {
    if (Math.random() < POWERUP_SPAWN_CHANCE) {
      Level.addEntity(Level.setRandomSpawnPosition(getRandomPowerup(0, 0)));
    }
  }
}

public Powerup getRandomPowerup(float x, float y) {
  if (Math.random() < Powerup.HEALTH_PACK_RATE) {
    return new HealthPack(x, y);
  }
  else {
    return new Ammo(x, y);
  }
}

abstract class Powerup extends Actor {
  
  static final int SIZE = 16;
  static final float HEALTH_PACK_RATE = 0.4f;
  
  Powerup() {
    super(0, 0);
  }
  
  Powerup(float x, float y) {
    super();
    this.x = x;
    this.y = y;
    this.width = SIZE;
    this.height = SIZE;
  }
  
  public void update() {
    super.update();
    Level.handleTileCollision(this);
  }
  
  public boolean hasGravity() {
    return true;
  }
  
  public void handleEntityCollision(Entity other) {
    if (other instanceof Player) {
      grantBoon((Player) other);
      this.delete();
    }
  }
  
  public abstract void grantBoon(Player player);
}

class Ammo extends Powerup {
  
  static final int BULLET_AMOUNT = 6;
  
  Ammo() {
    this(0, 0);
  }
  
  Ammo(float x, float y) {
    super(x, y);
    this.fillColor = color(160, 130, 140);
  }
  
  public void grantBoon(Player player) {
    player.reserveBullets += BULLET_AMOUNT;
    Audio.play("beep00.wav");
  }
}

class HealthPack extends Powerup {
  
  static final int HEAL_AMOUNT = 20;
  
  HealthPack() {
    this(0, 0);
  }
  
  HealthPack(float x, float y) {
    super(x, y);
    this.fillColor = color(120, 170, 150);
  }
  
  public void grantBoon(Player player) {
    player.health += HEAL_AMOUNT;
    Audio.play("beep01.wav");
  }
}

class Artifact extends Powerup {
  
  int[][] SPAWN_POINTS = {{1888, 2400}, {2440, 1216}, {1899, 1216}, {520, 750}, {4280, 1225}};
  
  Artifact() {
    this(0, 0);
  }
  
  Artifact(float x, float y) {
    super(x, y);
    this.fillColor = color(220, 34, 157);
  }
  
  public void grantBoon(Player player) {
    player.hasArtifact = true;
    Audio.play("shriek01.wav");
  }
  
  public void spawn() {
    int i = (int) (Math.random() * SPAWN_POINTS.length);
    this.x = SPAWN_POINTS[i][0];
    this.y = SPAWN_POINTS[i][1];
  }
}
interface Screen {
  public void draw();
}

class GameScreen implements Screen {
  
  public void handleSpawning() {
    counter++;
    if (counter % (FPS * ENEMY_SPAWN_RATE) == 0) {
      float rate = BASE_ENEMY_SPAWN_CHANCE;
      if (counter > 60 * FPS) {
        rate += ENEMY_SPAWN_CHANCE_MINUTE_STEP * (counter / (60.0f * FPS));
      }
      if (player.hasArtifact) {
        rate = 1;
      }
      if (Math.random() < rate) {
        Level.addEntity(Level.setRandomSpawnPosition(new Enemy(), player));
      }
    }
  }
  
  public void drawHud() {
    if (healthBarTop * Player.MAX_HP > player.health) {
      if (player.damageTimer == 0) {
        healthBarTop -= Graphics.BAR_DECAY_RATE;
      }
    }
    else {
      healthBarTop = (float) player.health / Player.MAX_HP;
    }
    fill(255);
    text("Health:", 5, 20);
    Graphics.drawDecayingBar(50, 8, 120, 16, (float) player.health / Player.MAX_HP, player.HP_BAR_COLOR, 
      healthBarTop, player.HP_DECAY_COLOR);
    fill(255);
    text("Ammo: " + player.bullets + "/" + player.reserveBullets, 5, 40);
    if (player.hasArtifact) {
      text("Artifact retrieved", 5, 60);
    }
  }
  
  public void draw() {
    background(20, 20, 60);

    Level.drawTiles();
    
    handleSpawning();
  
    ListIterator<Entity> iterator = Level.newEntities.listIterator();
    while (iterator.hasNext()) {
      Entity newEntity = iterator.next();
      Level.entities.add(newEntity);
      iterator.remove();
    }
  
    iterator = Level.entities.listIterator();
    while (iterator.hasNext()) {
      Entity entity = iterator.next();
      entity.update();
      entity.draw();
  
      if (entity.deleted) {
        iterator.remove();
      }
    }
    
    fill(120, 120, 150, 80);
    rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
  
    Graphics.update(player);
    
    drawHud();
  }
}

class WinScreen implements Screen {
  
  public void draw() {
    if (winTimer == 0) {
      System.exit(0);
    }
    else {
      winTimer--;
      
      background(20, 20, 60);
      Level.drawTiles();
      
      for (Entity entity : Level.entities) {
        entity.draw();
      }
      
      fill(190, 0, 255, 80);
      rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
      fill(255, 255, 255);
      text("Mission accomplished!", SCREEN_WIDTH / 2 - 50, SCREEN_HEIGHT / 2);
    }
  }
}

class DeathScreen implements Screen {
  
  public void draw() {
    if (deathTimer == 0) {
      System.exit(0);
    }
    else {
      deathTimer--;
      fill(255, 0, 0);
      rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
    }
  }
}



class Sword extends PlayerAttack {
  
  static final int WIDTH = 68;
  static final int HEIGHT = 3;
  static final int DAMAGE = 20;
  static final int TTL = (int) (1.0f/3 * FPS);
  
  final int COLOR = color(180, 0, 20);
  
  Player player;
  int timer;
  Set<Entity> damagedTargets;
  
  Sword(Player player) {
    super();
    this.player = player;
    this.timer = TTL;
    this.damagedTargets = new HashSet();
    
    this.width = 0;
    this.height = HEIGHT;
    this.fillColor = COLOR;
  }
  
  public void update() {
    super.setVelocity();
    this.x = player.x;
    if (player.facingRight) {
      this.x += player.width;
    }
    else {
      this.x -= this.width;
    }
    this.y = player.y + Player.BLADE_HEIGHT;
    
    this.timer--;
    if (timer <= 0) {
      player.swordDrawn = false;
      delete();
    }
    else if (timer <= TTL / 3) {
      width -= round((WIDTH / (TTL / 3)));
    }
    else if (timer > TTL * 2 / 3) {
      width += round((WIDTH / (TTL / 3)));
    }
  }
  
  public int getDamage(Entity entity) {
    if (damagedTargets.contains(entity)) {
      return 0;
    }
    
    damagedTargets.add(entity);
    return DAMAGE;
  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Gather" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
