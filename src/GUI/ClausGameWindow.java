package GUI;

import Model.GameSave;
import Model.Torus;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;

public class ClausGameWindow {
    private JPanel MainPanel;
    private Torus game;
    private int N = 10, M = 20;
    private static final int MIN_CHILDREN = 10, MAX_CHILDREN = 20;
    private static final int FPS = 25;
    private final JLabel[][] fields;
    private static final Random rnd = new Random();
    private boolean gameLost = false;
    private boolean gameWon = false;
    private boolean windowClosed = false;
    private final File file = new File("save.tor");
    private BufferedImage MIR;// = ImageIO.read(new FileInputStream("res/Fisto.png"));
    private BufferedImage MIRCaught;
    private BufferedImage child;
    private BufferedImage childNaps;
    private BufferedImage childSad;
    private BufferedImage empty;

    private boolean isGameLost(){
        synchronized (this){return gameLost;}
    }
    private boolean isGameWon(){synchronized (this){ return gameWon;}}
    private boolean isWindowClosed(){synchronized (this){return windowClosed;}}

    private void closeWindow(JFrame frame){
        synchronized (this){
            windowClosed = true;
            frame.dispose();
        }
    }

    private void display(Torus.Occupation[][] occupations) {
        for(int i = 0; i < M; i++)
        {
            for(int j = 0; j < N; j++)
            {
                switch(occupations[i][j])
                {
                    case NONE:
                        fields[i][j].setIcon(new ImageIcon(empty));
                        break;
                    case MAN_IN_RED:
                        fields[i][j].setIcon(new ImageIcon(MIR));
                        break;
                    case C_NAPS:
                        fields[i][j].setIcon(new ImageIcon(childNaps));
                        break;
                    case C_WON:
                        fields[i][j].setIcon(new ImageIcon(MIRCaught));
                        break;
                    case C_CRAWLS:
                        fields[i][j].setIcon(new ImageIcon(child));
                        break;
                    case C_GROUNDED:
                        fields[i][j].setIcon(new ImageIcon(empty));
                        break;
                    case C_SAD:
                        fields[i][j].setIcon(new ImageIcon(childSad));
                }
            }
        }
    }

    private ClausGameWindow(@NotNull JFrame frame) {
        try {
            MIR = ImageIO.read(new File("D:\\Desktop\\studia\\Informatyka\\Java\\List9\\res\\Fisto.png"));
            MIRCaught = ImageIO.read(new File("D:\\Desktop\\studia\\Informatyka\\Java\\List9\\res\\got.png"));
            child = ImageIO.read(new File("D:\\Desktop\\studia\\Informatyka\\Java\\List9\\res\\He-Man.png"));
            childNaps = ImageIO.read(new File("D:\\Desktop\\studia\\Informatyka\\Java\\List9\\res\\nap.png"));
            childSad = ImageIO.read(new File("D:\\Desktop\\studia\\Informatyka\\Java\\List9\\res\\sceletor.png"));
            empty = ImageIO.read(new File("D:\\Desktop\\studia\\Informatyka\\Java\\List9\\res\\black.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        frame.setContentPane(MainPanel);
        GameSave save = null;
        try {
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file));
            save = (GameSave)inputStream.readObject();
        } catch (Exception ignored) {
        }
        if(save == null) game = new Torus(M,N,rnd.nextInt(MAX_CHILDREN-MIN_CHILDREN+1)+MIN_CHILDREN);
        else game = new Torus(save.getOccupations());
        M = game.getWidth();
        N = game.getHeight();
        fields = new JLabel[M][N];
        frame.setSize(1280,720);
        MainPanel.setLayout(new BorderLayout());
        JPanel SubMainPanel = new JPanel();
        SubMainPanel.setLayout(new GridLayout(N,M));
        SubMainPanel.setVisible(true);
        SubMainPanel.setEnabled(true);
        MainPanel.add(SubMainPanel,SwingConstants.CENTER);
        for(int j = 0; j < N; j++)
        {
            for(int i = 0; i < M; i++)
            {
                fields[i][j] = new JLabel();
                assert empty != null;
                fields[i][j].setIcon(new ImageIcon(empty));
                fields[i][j].setBackground(Color.BLACK);
                fields[i][j].setSize(20,20);
                fields[i][j].setVisible(true);
                SubMainPanel.add(fields[i][j]);
            }
        }
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch(e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE: {
                        game.pause();
                        GameSave save;
                        if(!isGameLost() && !isGameWon() && !isWindowClosed()){
                            save = new GameSave(game.getOccupation());
                        }else save = null;
                        try {
                            ObjectOutputStream outputStream =
                                    new ObjectOutputStream(new FileOutputStream(file.getPath()));
                            outputStream.writeObject(save);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                        game.abort();
                        closeWindow(frame);
                        break;
                    }
                    case KeyEvent.VK_SPACE: {
                        if(game.isGamePaused()) game.unpause();
                        else game.pause();
                        break;
                    }
                    case KeyEvent.VK_UP:
                    {
                        game.moveManInRed(Torus.Direction.NORTH);
                        break;
                    }
                    case KeyEvent.VK_DOWN:
                    {
                        game.moveManInRed(Torus.Direction.SOUTH);
                        break;
                    }
                    case KeyEvent.VK_LEFT:
                    {
                        game.moveManInRed(Torus.Direction.WEST);
                        break;
                    }
                    case KeyEvent.VK_RIGHT:
                    {
                        game.moveManInRed(Torus.Direction.EAST);
                        break;
                    }
                }
            }
        });


    }

    public static void main(String[] args){
        JFrame frame = new JFrame();
        ClausGameWindow gameWindow = new ClausGameWindow(frame);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        Timer timer = new Timer(1000/FPS,x -> {
            if(gameWindow.game.isGamePaused()) return;
            gameWindow.refresh();
        });
        frame.pack();
        timer.start();
        gameWindow.game.unpause();
        while(true){
            if(gameWindow.isWindowClosed()){
                timer.stop();
                break;
            }
            if(gameWindow.isGameLost()){
                timer.stop();
                gameWindow.printLooseMessage(frame);
                break;
            }
            if(gameWindow.isGameWon()){
                timer.stop();
                gameWindow.printWinMessage(frame);
                break;
            }
        }
    }

    private void printWinMessage(JFrame frame) {
        frame.setTitle("YOU WIN");
    }

    private void printLooseMessage(JFrame frame) {
        frame.setTitle("YOU LOOSE");
    }

    private void refresh(){
        Torus.Occupation[][] occupations = game.getOccupation();
        display(occupations);
        switch (game.getGameState()) {
            case WON:
                synchronized (this){
                    gameWon = true;
                }
                break;
            case LOST:
                synchronized (this){
                    gameLost = true;
                }
                break;
                default:
        }
    }

}
