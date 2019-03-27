package Model;

import org.jetbrains.annotations.NotNull;

import java.util.Random;

class Child{



    public enum State{NAPS, CRAWLS, WON, GROUNDED, SAD}

    private static final int SIGHT_LIMIT = 5;
    private static final Random rnd = new Random();
    private static final int NAP_TOKEN_TIME = 500; //in milliseconds
    private static final int REFRESH_RATE = 500; // in milliseconds
    private static final int ENERGY_LIMIT = 10;
    private static final int MIN_NAP_TOKENS = 2;
    private static final int MAX_NAP_TOKENS = 10;


    private int x, y;
    private int napTokens = 0;
    private final Torus torus;
    private int energy = 0;
    private State state;

    Child(int x, int y, Torus torus) {
        this.x = x;
        this.y = y;
        this.torus = torus;
        this.state = State.NAPS;
        new Thread(this::act).start();
    }

    Child(int x, int y, Torus torus, @NotNull State state){
        this.x = x;
        this.y = y;
        this.torus = torus;
        switch (state) {
            case NAPS:
                this.energy = 0;
                this.state = State.NAPS;
                break;
            case CRAWLS:
                this.energy = ENERGY_LIMIT;
                this.state = State.CRAWLS;
                break;
            case GROUNDED:
                this.state = State.GROUNDED;
                break;
        }
        if(this.state != State.GROUNDED) new Thread(this::act).start();
    }

    int getX(){
        synchronized (this)
        {
            return x;
        }
    }
    int getY(){
        synchronized (this)
        {
            return y;
        }
    }
    State getState(){
        synchronized (this){
            return state;
        }
    }
    private void setState(State state){
        synchronized (this){
            this.state = state;
        }
    }
    void upset() {
        if(state == State.CRAWLS || state == State.NAPS)
            state = State.SAD;
    }
    boolean satisfy() {
        if(getState() == State.NAPS || getState() == State.GROUNDED){
            setState(State.GROUNDED);
            return true;
        } else{
            setState(State.WON);
            return false;
        }

    }

    private void getNap(){
        try {
            setState(State.NAPS);
            Thread.sleep(NAP_TOKEN_TIME);
            if(napTokens == 0)
            {
                synchronized (this){
                    napTokens = rnd.nextInt(MAX_NAP_TOKENS-MIN_NAP_TOKENS+1)+MIN_NAP_TOKENS;
                    energy = rnd.nextInt(1 + ENERGY_LIMIT);
                }
            }
            synchronized (this){
                napTokens--;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Torus.Direction lookForManInRed()
    {
        return torus.wayToManInRed(x,y,SIGHT_LIMIT);
    }

    private void move(Torus.Direction direction) {
        Torus.MoveCode code = torus.move(x,y,direction);
        energy--;
        if(code == Torus.MoveCode.OK || code == Torus.MoveCode.GOT_MAN_IN_RED) {
            synchronized (this){
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
                }
                x = torus.moduloX(x);
                y = torus.moduloY(y);

            }
            if(code == Torus.MoveCode.GOT_MAN_IN_RED) setState(State.WON);
        }
    }

    private void act() {
        while(!torus.isGameEnded() && getState() != State.GROUNDED){
            if(!torus.isGamePaused()){
                if(energy < 1) getNap();
                else if(State.NAPS == getState()){
                    if(napTokens > 0) getNap();
                    else setState(State.CRAWLS);
                }
                else{
                    Torus.Direction direction = lookForManInRed();
                    if(direction == Torus.Direction.NONE) {
                        move(torus.randomDirection());
                        getNap();
                    }
                    else move(direction);
                }
            }
            try {
                Thread.sleep(REFRESH_RATE);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
