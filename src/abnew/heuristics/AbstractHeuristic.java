/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2015, Team DataLab Birds: 
 ** Karel Rymes, Radim Spetlik, Tomas Borovicka
 ** All rights reserved.
 **This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. 
 **To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ 
 *or send a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 *****************************************************************************/
package abnew.heuristics;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.Color;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

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
import abnew.utils.PlanTheTrajectory;

import ab.utils.*;
/**
*   This is the framework for all the heuristics from which the agent will later choose.
*   The right sequence of function calls (most of these functions have to be implemented separately in every heuristic) for a given heuristic is:
*   1) constructor
*   2) estimate utility
*   3) selectBestTrajectoryWRTAvailableBirds
*   (3.5) getUtility - decision purposes)
*   4) getShot
*   5) getTapInterval  
*/
public class AbstractHeuristic
{
    protected final StateOfTheScene _currentState;

    protected final Random _randomGenerator;
    
    protected final TrajectoryPlanner _tp;

    protected final ClientActionRobot _actionRobot;

    protected int _utility = -1;
    
    protected ArrayList<PlanTheTrajectory> _possibleDLTrajectories = null;

    protected PlanTheTrajectory _selectedDLTrajectory = null;
    
    
    /**
    *    Basic constructor. It has to have all the information about the game scene, i.e. blocks, hills,pigs, birds, actionRobot, etc.
    */
    public AbstractHeuristic(StateOfTheScene currentState, ClientActionRobot actionRobot,  TrajectoryPlanner tp)
    {
        _currentState = currentState;

        _actionRobot = actionRobot;        
        _tp = tp;

        _randomGenerator = new Random();
    }
    /**
    *   Does not have to be implemented in child classes.
    *   @return the utility of a given heuristic
    */

    public int getUtility()
    {
        return _utility;
    }

    /**
    *   Has to be implemented in child classes!
    *   Performs the calculation of the trajectory utility.
    */  
    protected int estimateUtility()
    {
        return 42;
    }

    /**
    *   Does not have to be implemented in child classes but if the heuristic works only for a few birds then it should be implemented.
    *   This function calculates when to tap for a given bird and given target object. This information is later passed to the server.
    *   @return the time in milliseconds.
    */
    protected int getTapInterval()
    {
        /*** Can't tell you everything... ;) ***/

        return 0;
    } 
    
    /**
    *   Has to be implemented in child classes.
    *   Writes to a log file information about the particular heuristic.
    */
    public int getHeuristicId()
    {
        return -1;
    }
    
    /**
    *   Does not have to be implemented in child classes.
    *   Calculates the distance between two objects on the screenshot.
    */
    protected double distance(Point p1, Point p2) {
        return Math
        .sqrt((double) ((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y)
                        * (p1.y - p2.y)));
    }
}