package Model;

class ManInRed {

    private int x, y;

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

    void move(Torus.Direction direction, int width, int height) {
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
                break;
        }
        if(y < 0) y = height - 1;
        if(y >= height) y = 0;
        if(x < 0) x = width - 1;
        if(x >= width) x = 0;
    }

    void setPosition(int x, int y){
        synchronized (this){
            this.x = x;
            this.y = y;
        }
    }
}
