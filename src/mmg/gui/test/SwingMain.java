package mmg.gui.test;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class SwingMain extends JFrame
{
    public SwingMain()
    {
        WorldWindowGLCanvas wwd = new WorldWindowGLCanvas();
        wwd.setPreferredSize(new java.awt.Dimension(1000, 800));
      
     
        this.getContentPane().add(wwd, java.awt.BorderLayout.CENTER);
        wwd.setModel(new BasicModel());
    }

    public static void main(String[] args)
    {
        java.awt.EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                JFrame frame = new SwingMain();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.pack();
                frame.setVisible(true);
            }
        });
    }
}
