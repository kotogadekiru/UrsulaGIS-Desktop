package gui.test;

import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JSlider;

import com.fazecast.jSerialComm.SerialPort;

public class testSerialComms {

	public static void main(String[] args) {
		JFrame window = new JFrame();
		JSlider slider = new JSlider();
		slider.setMaximum(1023);
		window.add(slider);
		window.pack();
		window.setVisible(true);

		SerialPort[] ports = SerialPort.getCommPorts();
		System.out.println("Select a port:");
		int i = 1;
		for(SerialPort port : ports)
			System.out.println(i++ +  ": " + port.getSystemPortName());
		Scanner s = new Scanner(System.in);
		int chosenPort = s.nextInt();
		s.close();
		SerialPort serialPort = ports[chosenPort - 1];
		if(serialPort.openPort())
			System.out.println("Port opened successfully.");
		else {
			System.out.println("Unable to open the port.");
			return;
		}
		serialPort.setComPortParameters(38400, 8, 1, SerialPort.NO_PARITY);
		serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
                
                		System.out.println("scanner");
		Scanner data = new Scanner(serialPort.getInputStream());
                
                		//System.out.println("while loop pre");
		int value = 0;
		while(data.hasNextLine()){
                       	 System.out.print(" ---- ");
                       	 System.out.println(data.nextLine());
                        
			try{value = Integer.parseInt(data.nextLine());}catch(Exception e){}
			slider.setValue(value);
                        
		}
		data.close();
		System.out.println("Done.");

	}

}
