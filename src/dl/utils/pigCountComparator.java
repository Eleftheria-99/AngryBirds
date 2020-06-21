package dl.utils;
/*****************************************************************************
 ** ANGRYBIRDS AI AGENT FRAMEWORK
 ** Copyright (c) 2015, Team DataLab Birds: 
 ** Karel Rymes, Radim Spetlik, Tomas Borovicka
 ** All rights reserved.
 **This work is licensed under the Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License. 
 **To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-sa/3.0/ 
 *or send a letter to Creative Commons, 444 Castro Street, Suite 900, Mountain View, California, 94041, USA.
 *****************************************************************************/

import java.util.Comparator;

import ab.vision.ABObject;
/**
**  Compares two trajectories and chooses the one with more utility,
** then with more pigs in the way, and lastly with the target point that is closer 
** 
**/
public class pigCountComparator implements Comparator<DLTrajectory>
{
	public int compare(DLTrajectory a, DLTrajectory b) 
    {
    	if (a.trajectoryUtility == b.trajectoryUtility)
    	{
    		if (a.numberOfPigsInTheWay == b.numberOfPigsInTheWay)			    
    		{
    			if (a.targetPoint.x == b.targetPoint.x)
    			{
    				return (int)(a.releaseAngle - b.releaseAngle);
    			}
    			else
    			{
    				return a.targetPoint.x - b.targetPoint.x;
    			}
    		}
    		else
    		{
    			return b.numberOfPigsInTheWay - a.numberOfPigsInTheWay;
    		}
    	}

        return b.trajectoryUtility - a.trajectoryUtility;
    }
}