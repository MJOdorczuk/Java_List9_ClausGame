package Model;

import java.util.ArrayList;
import java.util.Random;

public class Torus {

    private static final int MIN_DIMENSION = 10;
    private static final Random rnd = new Random();

    int moduloX(int x) {
        if(x < 0) x = width - 1;
        if(x >= width) x = 0;
        return x;
    }

    int moduloY(int y) {
        if(y < 0) y = height - 1;
        if(y >= height) y = 0;
        return y;
    }

    public enum Direction{NORTH, SOUTH, EAST, WEST, NONE}
    public enum MoveCode{OK, BLOCKED, GOT_MAN_IN_RED}
    public enum Occupation{NONE, MAN_IN_RED, C_NAPS, C_WON, C_CRAWLS, C_GROUNDED, C_SAD}
    public enum GameState{GOING, PAUSED, WON, LOST}

    private final int width, height;
    private final ArrayList<Child> children = new ArrayList<>();
    private final ManInRed joseph = new ManInRed();
    private boolean gamePaused = true;
    private boolean gameEnded = false;

    public Torus(int width, int height, int numberOfChildren) {
        Random rnd = new Random();
        if(numberOfChildren > width * height - 1) numberOfChildren = width * height - 1;
        this.width = width < MIN_DIMENSION ? MIN_DIMENSION : width;
        this.height = height < MIN_DIMENSION ? MIN_DIMENSION : height;
        class Point{
            private int x, y;
            private Point(int x, int y){this.x = x; this.y = y;}
            @Override
            public boolean equals(Object object) {
                if(object instanceof Point)
                {
                    Point o = (Point) object;
                    return x == o.x && y == o.y;
                }
                return false;
            }
            private boolean containedIn(ArrayList<Point> points){
                for(Point point : points){
                    if(equals(point)) return true;
                }
                return false;
            }
        }
        ArrayList<Point> lockedPoints = new ArrayList<>();
        lockedPoints.add(new Point(joseph.getX(),joseph.getY()));
        for(int i = 0; i < numberOfChildren; i++)
        {
            int x = rnd.nextInt(width);
            int y = rnd.nextInt(height);
            Point p = new Point(x,y);
            while(p.containedIn(lockedPoints) || (p.x == joseph.getX() && p.y == joseph.getY())){
                p.x++;
                if(p.x >= width){
                    p.x = 0;
                    p.y++;
                    if(p.y >= height) p.y = 0;
                }
            }
            children.add(new Child(p.x,p.y,this));
        }
    }

    public Torus(Occupation[][] occupations) {
        width = occupations.length;
        height = occupations[0].length;
        for(int i = 0; i < width; i++)
        {
            for(int j = 0; j < height; j++)
            {
                switch (occupations[i][j]) {
                    case C_NAPS:
                        children.add(new Child(i,j,this,Child.State.NAPS));
                        break;
                    case C_CRAWLS:
                        children.add(new Child(i,j,this,Child.State.CRAWLS));
                        break;
                    case C_GROUNDED:
                        children.add(new Child(i,j,this,Child.State.GROUNDED));
                        break;
                    case MAN_IN_RED:
                    {
                        this.joseph.setPosition(i,j);
                        break;
                    }
                }
            }
        }
    }

    Direction randomDirection(){
        switch(rnd.nextInt(4)){
            case 0:
                return Direction.NORTH;
            case 1:
                return Direction.SOUTH;
            case 2:
                return Direction.EAST;
            case 3:
                return Direction.WEST;
        }
        return Direction.NONE;
    }

    Direction wayToManInRed(int x, int y, int sightLimit) {
        int jx, jy;
        synchronized (this)
        {
            jx = joseph.getX();
            jy = joseph.getY();
        }
        int distX = Math.abs(jx - x);
        if(distX > width - distX){
            distX = width - distX;
            int temp = jx;
            jx = x;
            x = temp;
        }
        int distY = Math.abs(jy - y);
        if(distY > height - distY){
            distY = height - distY;
            int temp = jy;
            jy = y;
            y = temp;
        }
        if(distX + distY > sightLimit) return Direction.NONE;
        else if(Math.abs(jx - x) > Math.abs(jy - y))
        {
            if(jx > x) return Direction.EAST;
            else return Direction.WEST;
        }
        else if(jy > y) return Direction.SOUTH;
        else return Direction.NORTH;
    }

    MoveCode move(int x, int y, Direction direction) {
        switch (direction) {
            case NORTH:
                y--;
                break;
            case SOUTH:
                y++;
                break;
            case EAST:
                x++;
                break;
            case WEST:
                x--;
                break;
            case NONE:
                return MoveCode.OK;
        }
        y = moduloY(y);
        x = moduloX(x);
        for(Child child : children)
        {
            if(child.getX() == x && child.getY() == y) return MoveCode.BLOCKED;
        }
        if(joseph.getX() == x && joseph.getY() == y)
        {
            endGame();
            return MoveCode.GOT_MAN_IN_RED;
        }
        else return MoveCode.OK;
    }

    public void moveManInRed(Direction direction){
        if(isGamePaused()) return;
        joseph.move(direction,width,height);
        synchronized (this){
            int x = joseph.getX();
            int y = joseph.getY();
            for(Child child : children)
            {
                if(child.getX() == x && child.getY() == y)
                {
                    if(!child.satisfy()) {
                        endGame();
                    }
                    else{
                        if(children.stream().allMatch(c -> c.getState() == Child.State.GROUNDED)) endGame();
                    }
                }
            }
        }
    }

    private void endGame() {
        synchronized (this){
            for(Child child : children) child.upset();
            gameEnded = true;
        }
    }

    boolean isGameEnded(){
        synchronized (this){
            return gameEnded;
        }
    }

    public boolean isGamePaused(){
        synchronized (this){
            return gamePaused;
        }
    }

    public GameState getGameState(){
        if(gameEnded)
            if(children.stream().noneMatch(child ->
                    child.getState() == Child.State.SAD)) return GameState.WON;
            else return GameState.LOST;
        else if(gamePaused) return GameState.PAUSED;
        else return GameState.GOING;
    }

    public Occupation[][] getOccupation() {
         Occupation[][] occupations = new Occupation[width][height];
         for(int i = 0; i < width; i ++)
         {
             for(int j = 0; j < height; j++)
             {
                 occupations[i][j] = Occupation.NONE;
             }
         }
         synchronized (this){
             occupations[joseph.getX()][joseph.getY()] = Occupation.MAN_IN_RED;
             for(Child child : children)
             {
                 Occupation state = Occupation.NONE;
                 switch (child.getState()) {
                     case NAPS:
                         state = Occupation.C_NAPS;
                         break;
                     case CRAWLS:
                         state = Occupation.C_CRAWLS;
                         break;
                     case WON:
                         state = Occupation.C_WON;
                         break;
                     case GROUNDED:
                         state = Occupation.C_GROUNDED;
                         break;
                     case SAD:
                         state = Occupation.C_SAD;
                         break;
                 }
                 occupations[child.getX()][child.getY()] = state;
             }
         }
         return occupations;
    }

    public void pause(){
        synchronized (this){
            gamePaused = true;
        }
    }

    public void unpause(){
        synchronized (this){
            gamePaused = false;
        }
    }

    public void abort(){
        endGame();
    }

    public int getWidth(){return width;}

    public int getHeight(){return height;}
}
