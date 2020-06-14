package abnew.utils;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import ab.demo.other.ClientActionRobot;
import ab.planner.TrajectoryPlanner;
import ab.vision.ABObject;
import ab.vision.ABType;

public class PlanTheTrajectory {
	
	
	public ClientActionRobot actionRobot = null;

	public TrajectoryPlanner tp = null;

	public Rectangle sling = null;

	public ABType birdOnSling = null;

	public Point releasePoint = null;
	public double releaseAngle = 0.0;

	public Point targetPoint = null;
	public ABObject targetObject = null;	

	public List<Point> trajectoryPoints = null;
	public int trajectoryUtility = 0;

	public List<ABObject> hillsAll = null;
	public List<ABObject> blocksAll = null;	

	public List<ABObject> hillsInTheWay = null;
	public List<ABObject> blocksInTheWay = null;
	public List<Double> blocksInTheWayDistanceFromTheTarget = null;
	public List<ABObject> pigsInTheWay = null;

	public int numberOfPigsInTheWay = 0;				

	public int heuristicUtility = 0;
	
	public boolean buildingFlag = false;

	public BufferedImage plannedScreenshot = null;

	/**
	*	Constructor needs all the information about the scene, also the target point and target object
	* 	it then calculates the trajectory and uses the trajectory points to compute the objects in the way.
	*/
	public PlanTheTrajectory(ClientActionRobot tmpActionRobot, TrajectoryPlanner tmpTp, Rectangle tmpSling, ABType tmpBirdOnSling, Point tmpReleasePoint, Point tmpTargetPoint, ABObject tmpTargetObject,List<ABObject> tmpHills, List<ABObject> tmpBlocks, List<ABObject> tmpPigs)
	{
		actionRobot = tmpActionRobot;
		tp = tmpTp;

		sling = tmpSling;
		birdOnSling = tmpBirdOnSling;

		releasePoint = tmpReleasePoint;
		releaseAngle = tp.getReleaseAngle(sling, releasePoint);

		targetPoint = tmpTargetPoint;				
		targetObject = tmpTargetObject;

		hillsAll = tmpHills;
		blocksAll = tmpBlocks;

		if (trajectoryPoints.size() == 0)
		{
			trajectoryUtility = 0xfffff000;
		}
	}

}
