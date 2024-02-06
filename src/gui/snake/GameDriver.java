package gui.snake;

public class GameDriver{
	 
	  // Outline of game.
	  //
	  // There will be two players, represented by a red snake and a blue snake.
	  // 
	  // The game will consist of multiple rounds, until one player "crashes" 5 times.
	  //
	  // A "crash" occurs when the head of one snake runs into any part of the body of
	  // the competitor's snake. After a crash, the score of the player who did NOT crash
	  // is augmented by the length of that player's snake. After a crash, both players
	  // have their snakes reset to the original length.
	  //
	  // A player's snake is lengthened if he runs over "food", which appears randomly on
	  // the screen. Whichever player gets to the food first will get their snake lengthened.
	  //
	  
	  public static void main(String[] args){
	    DisplayWindow d = new DisplayWindow();
	    DrawGame p = new DrawGame();
	    d.addPanel(p);
	    p.requestFocusInWindow();
	    d.showFrame();
	  }
	}