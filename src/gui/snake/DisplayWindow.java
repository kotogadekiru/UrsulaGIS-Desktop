package gui.snake;

import java.awt.*;
import javax.swing.*;
public  class DisplayWindow extends JFrame{

  private Container c;

  public DisplayWindow(){
    super("Snake 'em");
    c = this.getContentPane();
  }

  public void addPanel(JPanel p){
    p.setPreferredSize(new Dimension(800,600));
    c.add(p);
  }

  public void showFrame(){
    this.pack();
    this.setVisible(true);
    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }
}
