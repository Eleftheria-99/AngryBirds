package abnew.heuristics;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.Color;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Collections;
import java.util.Comparator;

import java.io.*;

import ab.demo.other.ClientActionRobot;
import ab.demo.other.ClientActionRobotJava;

import ab.demo.other.Shot;

import ab.planner.TrajectoryPlanner;

import ab.utils.StateUtil;
import ab.utils.ABUtil;

import ab.vision.ABObject;
import ab.vision.ABType;
import ab.vision.GameStateExtractor.GameState;
import ab.vision.Vision;
import ab.vision.VisionUtils;

import dl.utils.*;
/**
*	Building heuristic finds a recursively connected block structure that is nearby a pig or that contains a pig. The block inside a given building is considered suitable to be the target object if it satisfies all of the following conditions: 
*	reachable,
*	flat or straight,
*	has at least two supporters,
*	not high,
*	is not a square.
*	All the selected blocks are then sorted based on their type and relative position in the building. The best block for a given bird on sling is then selected. 
*	We also differentiate between three types of buildings:
*	Pyramid
*	Rectangle
*	Skyscraper
*	These three different kinds of structures then also have three different aiming policies. In the pyramid, the goal is to shoot as low as possible with the pig’s intention to destroy the block. Whereas in the skyscraper the bird tries to move with the whole structure and turn it over. Therefore it aims at the top. The last structure - rectangle - is somewhere in between. Therefore the pig tries to aim both at the top trying to turn the building over, or at the bottom trying to destroy the whole structure so that it corrupts.
*/

public class DestroyBuilding extends AbstractHeuristic
{
	private List<Building> buildings;

	 /**
     *   Basic constructor. It has to have all the information about the game scene, i.e. blocks, hills,pigs, birds, actionRobot, etc.
    */
 	public DestroyBuilding(SceneState currentState, ClientActionRobot actionRobot,  TrajectoryPlanner tp, LogWriter lg)
	{
		super(currentState, actionRobot, tp, lg);

		_utility = estimateUtility();

  	}
 
    /**
    *	Performs the calculation of the trajectory utility.
    */  
	@Override
	protected int estimateUtility()
	{
		
		buildings = DLUtils.findBuildings(_currentState._blocks);

		if (_currentState._birdOnSling == ABType.WhiteBird)
		{
			return 0xffff0000;
		}
		
		List<ABObject> piggies = new LinkedList<ABObject> (_currentState._pigs);
		//leaves only the buildings that are bigger than 2 blocks and that also contain a pig in close proximity
  	  	for (int i=0;i < buildings.size();++i)
		{		
			if ( buildings.get(i).findPigsInsideBuilding(piggies) == false)
			{
				buildings.remove(i);
				--i;
				continue;
			}
    	}

    	for (int i=0;i < buildings.size();++i)
		{
			Building bld = buildings.get(i);

			bld.findPigsNearby(piggies);

			if ( bld.pigs.size() == 0 && bld.rightPigs.size() == 0)
			{
				buildings.remove(i);
				--i;
			}

    	}
    
		_possibleDLTrajectories = new ArrayList<DLTrajectory>();

		//finds the best shot for every building
		for (Building bld : buildings)
    	{

			DLTrajectory tmp = getShotOneBuilding(bld);
			if (tmp != null)
			{
				_possibleDLTrajectories.add(tmp);
			}
    	}

    	//finds the best building to aim at based on the target object type 
    	if( _possibleDLTrajectories.size() == 0 )
    	{
			for (Building bld : buildings)
    		{

				DLTrajectory tmp = getSpareShotOneBuilding(bld);
				if (tmp != null)
				{
					_possibleDLTrajectories.add(tmp);
				}
    		}
    	}
    	
		Collections.sort(_possibleDLTrajectories, 
			new Comparator<DLTrajectory>() 
    		{
			    public int compare(DLTrajectory a, DLTrajectory b) 
    			{
			        return a.targetPoint.x - b.targetPoint.x;
			    }
			});
      	
      	_selectedDLTrajectory = selectBestTrajectoryWRTAvailableBirds();

      	if (_selectedDLTrajectory == null)
      		return 0xffff0000;
      	else 
			return _selectedDLTrajectory.heuristicUtility;
	}
	/**
	*	Counts the one best shot for a given building.
	*	@return the best trajectory in the building.
	*/
	private DLTrajectory getShotOneBuilding(Building biggest)
	{
     	    

		List<DLTrajectory> joints = biggest.findJoints(_actionRobot, _tp, _currentState._sling, _currentState._birdOnSling, _currentState._hills, _currentState._blocks, _currentState._pigs, false);

		if(joints.size() == 0)
		{
			return null;
		}

		return  joints.get(0);
	}

	/**
	*	If the building does not have any sufficient blocks to aim at, then this function tries to find a different, a little bit worse than the average, block.
	*	@return the best trajectory in the building.
	*/
	private DLTrajectory getSpareShotOneBuilding(Building biggest)
	{
     	    
		List<DLTrajectory> joints = biggest.findJoints(_actionRobot, _tp, _currentState._sling, _currentState._birdOnSling, _currentState._hills, _currentState._blocks, _currentState._pigs, true);

		if(joints.size() == 0)
		{
			return null;
		}

		return  joints.get(0);
	}
	
  	/**
	*	Writes information about a particular building to a log file.
	*	BuildingDominantType,BuildingProportions,BuildingTopX,BuildingTopY,BuildingWidth,BuildingHeight,BuildingArea,BuildingBlocksSize,BuildingIsBunkerIncluded,BuildingPigsInTheBunker,BuildingFurthestBlockFromPig,BuildingIceCount,BuildingWoodCount,BuildingStoneCount
  	*/

	private void logBuilding(Building bld)
	{
		
		log.append(bld.getDominantType());
		log.append(bld.getProportions());
		log.append(bld.x);
		log.append(bld.y);
		log.append(bld.getBoundingRect().width);
		log.append(bld.getBoundingRect().height);
		log.append(bld.getBoundingRect().height * bld.getBoundingRect().width);
		
		log.append(bld.blocks.size());

		int count = 0;
		boolean isBunkerIncluded = false;
		ABObject rect = new ABObject(bld.getBoundingRect(),ABType.Unknown);
		System.out.println(rect._shiftar);
		for (ABObject pig : _currentState._pigs)
		{
			if (pig.touches(rect))
			{
				++count;
			}
				
		}

		log.append("OMITTED");
		log.append(count);


		if( bld.distances != null && bld.distances.size() > 0)
		{
			Collections.sort(bld.distances, 
			new Comparator<Integer>() 
    		{
			    public int compare(Integer a, Integer b) 
    			{
			        return b - a;
			    }
			});
			log.append(bld.distances.get(0));
		}
		else
		{
			log.append(-1);
		}

		
		for (int tmp = 10; tmp < 13; ++tmp )
			log.append(bld.getTypes()[tmp]);
		
	}
 	/**
    *	Writes to a log file information about the particular heuristic.
    */

	@Override
	public void writeToLog()
	{
		
		//BuildingsCount,BuildingDominantType,BuildingProportions,BuildingTopX,BuildingTopY,BuildingWidth,BuildingHeight,BuildingArea,BuildingBlocksSize,BuildingIsBunkerIncluded,BuildingPigsInTheBunker,BuildingFurthestBlockFromPig,BuildingIceCount,BuildingWoodCount,BuildingStoneCount
		log.append(buildings.size());
		int i;
		for (i=0; i < buildings.size() && i < 5;++i)
		{
			logBuilding(buildings.get(i));
		}
		log.fillWithBlanks(5-i,14);

	} 
    /**
   	*	@return ID of the heuristic for log purposes.
    */
    @Override
    public int getHeuristicId()
    {
        return 0;
    }
}