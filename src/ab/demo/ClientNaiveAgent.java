/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2014, XiaoYu (Gary) Ge, Stephen Gould, Jochen Renz
 **  Sahan Abeyasinghe,Jim Keys,  Andrew Wang, Peng Zhang
 ** All rights reserved.
**This work is licensed under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
**To view a copy of this license, visit http://www.gnu.org/licenses/
 *****************************************************************************/
package ab.demo;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ab.demo.other.ActionRobot;
import ab.demo.other.ClientActionRobot;
import ab.demo.other.ClientActionRobotJava;
import ab.demo.other.Shot;
import ab.planner.TrajectoryPlanner;
import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;
import dl.utils.AbstractHeuristic;
import dl.utils.DLLevelSelection;
import dl.utils.DestroyAsManyPigsAtOnceAsPossibleHeuristics;
import dl.utils.LogWriter;
//Naive agent (server/client version)
import dl.utils.SceneState;

public class ClientNaiveAgent implements Runnable {

	// Wrapper of the communicating messages
	private ClientActionRobotJava ar;
	public byte currentLevel = -1;
	public int failedCounter = 0;
	public int[] solved;
	TrajectoryPlanner tp;
	private int id = 28888;
	private boolean firstShot;
	private DLLevelSelection levelSchemer;
	private Point prevTarget;
	private Random randomGenerator;

	/**
	 * Constructor using the default IP
	 */
	public ClientNaiveAgent() {
		// the default ip is the localhost
		ar = new ClientActionRobotJava("127.0.0.1");
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;

	}

	/**
	 * Constructor with a specified IP
	 */
	public ClientNaiveAgent(String ip) {
		ar = new ClientActionRobotJava(ip);
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;

	}

	public ClientNaiveAgent(String ip, int id) {
		ar = new ClientActionRobotJava(ip);
		tp = new TrajectoryPlanner();
		randomGenerator = new Random();
		prevTarget = null;
		firstShot = true;
		this.id = id;
	}

	public int getNextLevel() {
		int level = 0;
		boolean unsolved = false;
		// all the level have been solved, then get the first unsolved level
		for (int i = 0; i < solved.length; i++) {
			if (solved[i] == 0) {
				unsolved = true;
				level = i + 1;
				if (level <= currentLevel && currentLevel < solved.length)
					continue;
				else
					return level;
			}
		}
		if (unsolved)
			return level;
		level = (currentLevel + 1) % solved.length;
		if (level == 0)
			level = solved.length;
		return level;
	}

	/*
	 * Run the Client (Naive Agent)
	 */
	private void checkMyScore() {

		int[] scores = ar.checkMyScore();
		System.out.println(" My score: ");
		int level = 1;
		for (int i : scores) {
			System.out.println(" level " + level + "  " + i);
			if (i > 0)
				solved[level - 1] = 1;
			level++;
		}
	}

	public void run() {
		byte[] info = ar.configure(ClientActionRobot.intToByteArray(id));
		solved = new int[info[2]];

		levelSchemer = new DLLevelSelection(info, ar);

		// load the initial level (default 1)
		// Check my score
		checkMyScore();

		currentLevel = (byte) getNextLevel();
		ar.loadLevel(currentLevel);
		// ar.loadLevel((byte)9);
		GameState state;
		while (true) {

			state = solve();

			if (state != GameState.PLAYING) {
				LogWriter.lastScore = 0;
			}

			// If the level is solved , go to the next level
			if (state == GameState.WON) {

				levelSchemer.updateStats(ar, true);

				ar.loadLevel(levelSchemer.currentLevel);

				// make a new trajectory planner whenever a new level is entered
				tp = new TrajectoryPlanner();

				// first shot on this level, try high shot first
				firstShot = true;

			}
			// If lost, then restart the level
			else if (state == GameState.LOST) {

				levelSchemer.updateStats(ar, false);

				ar.loadLevel(levelSchemer.currentLevel);

			} else if (state == GameState.LEVEL_SELECTION) {
				System.out.println(
						"unexpected level selection page, go to the last current level : " + levelSchemer.currentLevel);
				ar.loadLevel(levelSchemer.currentLevel);
			} else if (state == GameState.MAIN_MENU) {
				System.out.println("unexpected main menu page, reload the level : " + levelSchemer.currentLevel);
				ar.loadLevel(levelSchemer.currentLevel);
			} else if (state == GameState.EPISODE_MENU) {
				System.out.println("unexpected episode menu page, reload the level: " + levelSchemer.currentLevel);
				ar.loadLevel(levelSchemer.currentLevel);
			}

		}

	}

	/**
	 * Solve a particular level by shooting birds directly to pigs only intended for
	 * one level
	 * 
	 * @return GameState: the game state after shots.
	 */
	public GameState solve()

	{

		// capture Image
		BufferedImage screenshot = ar.doScreenShot();

		// process image
		Vision vision = new Vision(screenshot);

		// find the slingshot
		Rectangle sling = vision.findSlingshotMBR();

		// get bird type on sling
		ABType birdOnSling = vision.getBirdTypeOnSling();

		GameState startState = ar.checkState();
		if (startState != GameState.PLAYING) {

			return startState;

		}

		// If the level is loaded (in PLAYING state) but no slingshot detected or no
		// bird on sling is detected, then the agent will try to do something with it.
		while ((sling == null || birdOnSling == ABType.Unknown) && ar.checkState() == GameState.PLAYING) {
			visionInfo retValues = new visionInfo(sling, vision, screenshot, birdOnSling);
			waitTillSlingshotIsFound(retValues);
			sling = retValues.sling;
			vision = retValues.vision;
			screenshot = retValues.screenshot;
			birdOnSling = retValues.birdOnSling;
		}

		startState = ar.checkState();
		if (startState != GameState.PLAYING) {
			return startState;
		}

		// get all the pigs
		List<ABObject> pigs = vision.findPigsMBR();
		final List<ABObject> birds = vision.findBirdsRealShape();
		final List<ABObject> hills = vision.findHills();
		final List<ABObject> blocks = vision.findBlocksRealShape();
		int gnd = vision.getGroundLevel();
		tp.ground = gnd;

		// creates the logwriter that will be used to store the information about turns
		final LogWriter log = new LogWriter("output.csv");
		log.appendStartLevel(levelSchemer.currentLevel, pigs, birds, blocks, hills, birdOnSling);
		log.saveStart(ar.doScreenShot());

		// accumulates information about the scene that we are currently playing
		SceneState currentState = new SceneState(pigs, hills, blocks, sling, vision.findTNTs(), prevTarget, firstShot,
				birds, birdOnSling);
		// Prepare shot.
		Shot shot = null;

		GameState state = ar.checkState();
		// if there is a sling, then play, otherwise skip.
		if (sling != null) {

			// If there are pigs, we pick up a pig randomly and shoot it.
			if (!pigs.isEmpty()) {

				AbstractHeuristic tmp = new DestroyAsManyPigsAtOnceAsPossibleHeuristics(currentState, ar, tp, log);
				shot = tmp.getShot();

			} else {
				System.err.println("No Release Point Found, will try to zoom out");
				// try to zoom out
				ActionRobot.fullyZoomOut();

				return state;
			}

			// check whether the slingshot is changed. the change of the slingshot indicates
			// a change in the scale.
			state = performTheActualShooting(log, currentState, shot);

		}
		return state;
	}

	private GameState performTheActualShooting(LogWriter log, SceneState currentState, Shot shot) {
		ar.fullyZoomOut();
		BufferedImage screenshot = ar.doScreenShot();
		Vision vision = new Vision(screenshot);
		Rectangle _sling = vision.findSlingshotRealShape();

		GameState state = null;
		if (_sling != null) {
			double scale_diff = Math.pow((currentState._sling.width - _sling.width), 2)
					+ Math.pow((currentState._sling.height - _sling.height), 2);

			if (scale_diff < 25) {
				if (shot.getDx() < 0) {
					ar.shoot(shot.getX(), shot.getY(), shot.getDx(), shot.getDy(), 0, shot.getT_tap(), false, tp,
							currentState._sling, currentState._birdOnSling, currentState._blocks, currentState._birds,
							1);
				}

				try {

					state = ar.checkState();
					log.appendScore(ar.getCurrentScore(), state);
					log.flush(ar.doScreenShot());
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (state == GameState.PLAYING) {
					vision = new Vision(ar.doScreenShot());
					List<Point> traj = vision.findTrajPoints();
					Point releasePoint = new Point(shot.getX() + shot.getDx(), shot.getY() + shot.getDy());

					// adjusts trajectory planner
					tp.adjustTrajectory(traj, vision.findSlingshotRealShape(), releasePoint);

					firstShot = false;
				}
			} else
				System.out.println("Scale is changed, can not execute the shot, will re-segement the image");
		} else
			System.out.println("no sling detected, can not execute the shot, will re-segement the image");

		return state;
	}
	
	/**
	**	performs the waiting for the slingshot to be found by zooming out and in and out
	**/
	private void waitTillSlingshotIsFound(visionInfo inf)
	{
		if (inf.sling == null)
		{
			System.out.println("No slingshot detected. Please remove pop up or zoom out");
		}
		else if (inf.birdOnSling == ABType.Unknown)
		{
			System.out.println("No bird on sling detected!!");
		}

		ar.fullyZoomOut();	
		inf.screenshot = ar.doScreenShot();			
		inf.vision = new Vision(inf.screenshot);
		inf.sling = inf.vision.findSlingshotRealShape();
		inf.birdOnSling = inf.vision.getBirdTypeOnSling();

		if ( inf.birdOnSling == ABType.Unknown )
		{
			ar.fullyZoomIn();
			inf.screenshot = ar.doScreenShot();			
			inf.vision = new Vision(inf.screenshot);
			inf.birdOnSling = inf.vision.getBirdTypeOnSling();				
			ar.fullyZoomOut();
			
			inf.screenshot = ar.doScreenShot();			
			inf.vision = new Vision(inf.screenshot);	
			inf.sling = inf.vision.findSlingshotRealShape();
		}
	}

	private double distance(Point p1, Point p2) {
		return Math.sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y)));
	}
	
	private class visionInfo
	{
		public Rectangle sling;
		public Vision vision;
		public BufferedImage screenshot;
		public ABType birdOnSling;

		public visionInfo(Rectangle sl, Vision vis, BufferedImage sc, ABType birdie)
		{
			sling = sl;
			vision = vis;
			screenshot = sc;
			birdOnSling = birdie;
		}
	}

	public static void main(String args[]) {

		ClientNaiveAgent na;
		if (args.length > 0)
			na = new ClientNaiveAgent(args[0]);
		else
			na = new ClientNaiveAgent();
		na.run();

	}
}
