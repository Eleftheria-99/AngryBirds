/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2015,  XiaoYu (Gary) Ge, Stephen Gould,Jochen Renz
 ** Sahan Abeyasinghe, Jim Keys,   Andrew Wang, Peng Zhang
 ** Team DataLab Birds: Karel Rymes, Radim Spetlik, Tomas Borovicka
 ** All rights reserved.
 **This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License.
 **To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/
 *or send a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 *****************************************************************************/

package ab.demo.other;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Point;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.Socket;
import java.net.UnknownHostException;

import java.util.List;
import java.util.ArrayList;

import dl.utils.*;

import ab.vision.ABType;
import ab.vision.Vision;
import ab.vision.ABObject;
import ab.vision.GameStateExtractor.GameState;

import ab.planner.TrajectoryPlanner;

import external.ClientMessageEncoder;
import external.ClientMessageTable;

/**
 * A server/client version of the java util class that encodes client messages and decodes 
 * the corresponding server messages complying with the protocols. Its subclass is ClientActionRobotJava.java 
 * which decodes the received server messages into java objects.
 * */
public class ClientActionRobot {
	Socket requestSocket;
	OutputStream out;
	InputStream in;
	String message;

	public ClientActionRobot(String... ip) {
		String _ip = "localhost";
		if (ip.length > 0) {
			_ip = ip[0];
		}
		try {
			// 1. creating a socket to connect to the server
			requestSocket = new Socket(_ip, 2004);
			requestSocket.setReceiveBufferSize(100000);
			System.out.println("Connected to " + _ip + " in port 2004");
			out = requestSocket.getOutputStream();
			out.flush();
			in = requestSocket.getInputStream();
		} catch (UnknownHostException unknownHost) {
			System.err.println("You are trying to connect to an unknown host!");
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
	}

	public  BufferedImage doScreenShot() {
		BufferedImage bfImage = null;
		try {
			// 2. get Input and Output streams
			byte[] doScreenShot = ClientMessageEncoder.encodeDoScreenShot();
			out.write(doScreenShot);
			out.flush();
			// System.out.println("client executes command: screen shot");

			//Read the message head : 4-byte width and 4-byte height, respectively
			byte[] bytewidth = new byte[4];
			byte[] byteheight = new byte[4];
			int width, height;
			in.read(bytewidth);
			width = bytesToInt(bytewidth);
			in.read(byteheight);
			height = bytesToInt(byteheight);
			
			//initialize total bytes of the screenshot message
			//not include the head
			int totalBytes = width * height * 3;

			//read the raw RGB data
			byte[] bytebuffer;
			//System.out.println(width + "  " + height);
			byte[] imgbyte = new byte[totalBytes];
			int hasReadBytes = 0;
			while (hasReadBytes < totalBytes) {
				bytebuffer = new byte[2048];
				int nBytes = in.read(bytebuffer);
				if (nBytes != -1)
					System.arraycopy(bytebuffer, 0, imgbyte, hasReadBytes,
							nBytes);
				else
					break;
				hasReadBytes += nBytes;
			}
			
			//set RGB data using BufferedImage  
			bfImage = new BufferedImage(width, height,
					BufferedImage.TYPE_INT_RGB);
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int R = imgbyte[(y * width + x) * 3] & 0xff;
					int G = imgbyte[(y * width + x) * 3 + 1] & 0xff;
					int B = imgbyte[(y * width + x) * 3 + 2] & 0xff;
					Color color = new Color(R, G, B);
					int rgb;
					rgb = color.getRGB();
					bfImage.setRGB(x, y, rgb);
				}
			}
			
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}
		return bfImage;

	}

	//convert a byte[4] array to int value
	public int bytesToInt(byte... b) {
		int value = 0;
		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			value += (b[i] & 0x000000FF) << shift;
		}
		return value;
	}

	//convert an int value to byte[4] array
	public static byte[] intToByteArray(int a) {
		byte[] ret = new byte[4];
		ret[3] = (byte) (a & 0xFF);
		ret[2] = (byte) ((a >> 8) & 0xFF);
		ret[1] = (byte) ((a >> 16) & 0xFF);
		ret[0] = (byte) ((a >> 24) & 0xFF);
		return ret;
	}

	//send message to fully zoom out
	public byte fullyZoomOut() {
		try {
			out.write(ClientMessageEncoder.fullyZoomOut());
			out.flush();

			return (byte) in.read();		 
		} catch (IOException e) {

			e.printStackTrace();
		} 
		return 0;

	}
	
	//send message to fully zoom out
	public byte fullyZoomIn() {
		try {
			out.write(ClientMessageEncoder.fullyZoomIn());
			out.flush();
			return (byte) in.read();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return 0;

	}
	public byte clickInCenter() {
		try {
			out.write(ClientMessageEncoder.clickInCenter());
			out.flush();
			return (byte) in.read();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return 0;

	}
	//register team id
	public byte[] configure(byte[] team_id) {
		try {
			out.write(ClientMessageEncoder.configure(team_id));
			out.flush();
			byte[] result = new byte[4];
			in.read(result);
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}

	//load a certain level
	public byte loadLevel(byte... i) {
		try {
			
			out.write(ClientMessageEncoder.loadLevel(i));
			return (byte) in.read();
		} catch (IOException e) {

			e.printStackTrace();
		}
		return 0;
	}

	
	//send a message to restart the level
	public byte restartLevel() {
		try {
			out.write(ClientMessageEncoder.restart());
			return (byte) in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 0;

	}

	//send a shot message to execute a shot in the safe mode
	public byte[] shoot(byte[] fx, byte[] fy, byte[] dx, byte[] dy, byte[] t1,
			byte[] t2, boolean polar) {
		byte[] inbuffer = new byte[16];
		try 
		{
			if (polar)
				out.write(ClientMessageEncoder.pshoot(fx, fy, dx, dy, t1, t2));
			else
				out.write(ClientMessageEncoder.cshoot(fx, fy, dx, dy, t1, t2));
			out.flush();
			in.read(inbuffer);

			return inbuffer;
		} 
		catch (IOException e) 
		{
			e.printStackTrace();
		}

		return new byte[] { 0 };
	}
	
	//send a shot message to execute a shot in the fast mode
	public byte[] shootFast(byte[] fx, byte[] fy, byte[] dx, byte[] dy, byte[] t1, byte[] t2, boolean polar, TrajectoryPlanner tp, Rectangle sling, ABType birdOnSling, List<ABObject> blocks, List<ABObject> birds, int nOfShots) {
		byte[] inbuffer = new byte[16];
		try {
			if (polar)
				out.write(ClientMessageEncoder.pFastshoot(fx, fy, dx, dy, t1, t2));
			else
				out.write(ClientMessageEncoder.cFastshoot(fx, fy, dx, dy, t1, t2));
			out.flush();

		
			in.read(inbuffer);

			// trajectory check
			waitingForSceneToBeSteady(birds, birdOnSling);			

			return inbuffer;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new byte[] { 0 };
	}

	//send a sequence of shots message
	public byte[] cshootSequence(byte[]... shots) {
		byte[] inbuffer = new byte[16];

		byte[] msg = ClientMessageEncoder.mergeArray(
				new byte[] { ClientMessageTable
						.getValue(ClientMessageTable.shootSeq) },
				new byte[] { (byte) shots.length });
		for (byte[] shot : shots) {
			msg = ClientMessageEncoder.mergeArray(msg,
					new byte[] { ClientMessageTable
							.getValue(ClientMessageTable.cshoot) }, shot);
		}

		try {
			out.write(msg);
			
			in.read(inbuffer);
			return inbuffer;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new byte[] { 0 };
	}

	
	//send a message to get the current state
	public byte getState() {
		try {
			out.write(ClientMessageEncoder.getState());
			out.flush();
			// System.out.println("IN READ  " + in.read());
			return (byte) in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return 0;
	}
	// send a message to score of each level
	public byte[] getBestScores()
	{
		int level = 21;
		int totalBytes = level * 4;
		byte[] buffer = new byte[totalBytes];
		try {
			out.write(ClientMessageEncoder.getBestScores());
			out.flush();
		
			in.read(buffer);
		    return buffer; 
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer;	
	}
	// send a message to score of each level
	public byte[] getMyScore()
	{
		int level = 21;
		int totalBytes = level * 4;
		byte[] buffer = new byte[totalBytes];
		try {
			out.write(ClientMessageEncoder.getMyScore());
			out.flush();
		
			in.read(buffer);
		    return buffer; 
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer;	
	}

	/*Checks and waits until no objects are moving in the screenshot*/
	public void waitingForSceneToBeSteady(List<ABObject> birdsOld, ABType birdOnSling)
	{
		int last = -1;
		long tStart = System.currentTimeMillis()-2;

		while (GameState.values()[getState()] == GameState.PLAYING && System.currentTimeMillis() - tStart < 22000)
		{
			
			BufferedImage screen = doScreenShot();

			// erase birds
			Graphics2D g2d = screen.createGraphics();

			for (ABObject bird : birdsOld)
			{
				g2d.setColor(new Color(148,206,222));				
				g2d.fillRect(bird.getCenter().x-40, bird.getCenter().y-40, 80, 80);

			}

			Vision vision = new Vision(screen);
			int total = 0;

			List<ABObject> possibleBirdsFromSling = new ArrayList<ABObject>();			

			List<ABObject> birds = vision.findBirdsMBR();

			for (int i = 0; i < birds.size(); ++i)
			{
				ABObject bird = birds.get(i);				

				if (bird.type == birdOnSling
					&& bird.type != ABType.BlueBird)
				{
					possibleBirdsFromSling.add(bird);
				}

				total += bird.getTotal();
			}

			List<ABObject> pigs = vision.findPigsRealShape();
			for (ABObject pig : pigs)
			{
				total += pig.getTotal();
			}

        	List<ABObject> blocks = vision.findBlocksRealShape();
        	for (ABObject block :blocks)
			{
				total += block.getTotal();
			}

			if (  Math.abs(last-total) < 2  
				&& possibleBirdsFromSling.size() == 0 
				&& pigs.size() != 0)
			{
				break;
			}

			last = total;

			try 
			{
				Thread.sleep(700);
			} 
			catch (InterruptedException e) 
			{
				e.printStackTrace();
			}
		}	

	}	
	
	public static void main(String args[])
	{
		ClientActionRobot robot = new ClientActionRobot();
		byte[] id = {1,2,3,4};
		robot.configure(id);
		while(true)
			robot.doScreenShot();
	}

}
