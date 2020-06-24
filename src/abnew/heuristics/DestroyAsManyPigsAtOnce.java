package abnew.heuristics;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import ab.demo.other.ClientActionRobot;
import ab.planner.TrajectoryPlanner;
import ab.vision.ABObject;
import ab.vision.ABType;
import dl.utils.*;

/**
 * Destroy as many pigs as possible strategy does exactly what the label says,
 * tries to find a trajectory with as many pigs in the way as possible.
 * Interestingly enough, it is also good at going around obstacles as it tries
 * to find the least restrictive way.
 */
public class DestroyAsManyPigsAtOnce extends AbstractHeuristic {

	private boolean noHighShotFound = false;

	/**
	 * Basic constructor. It has to have all the information about the game scene,
	 * i.e. blocks, hills,pigs, birds, actionRobot, etc.
	 */
	public DestroyAsManyPigsAtOnce(SceneState currentState, ClientActionRobot actionRobot, TrajectoryPlanner tp,
			LogWriter log) {
		super(currentState, actionRobot, tp, log);
		// TODO Auto-generated constructor stub

		_utility = estimateUtility();
		if (_possibleDLTrajectories.size() == 0) {
			noHighShotFound = true;
			_utility = estimateUtility();
		}
	}

	/**
	 * Performs the calculation of the trajectory utility.
	 */
	@Override
	protected int estimateUtility() {
		_possibleDLTrajectories = new ArrayList<DLTrajectory>();

		// find all reachable targets and save them to _possibleDLTrajectories array
		for (ABObject tmpTargetObject : _currentState._pigs) {
			// get target point
			Point tmpTargetCenterPoint = tmpTargetObject.getCenter();
			estimateTrajectories(tmpTargetCenterPoint, tmpTargetObject, true);

			int whiteBirdCorrection = 0;

			if (_currentState._birdOnSling == ABType.WhiteBird) {
				whiteBirdCorrection = 3;
			}

			// search around the target
			for (int i = -1 - whiteBirdCorrection; i < 2 + whiteBirdCorrection; i += 2) {
				searchAroundTheTarget(i, tmpTargetCenterPoint, tmpTargetObject);
			}
		}

		if (_possibleDLTrajectories.size() == 0)
			return 0xffff0000;

		// sort available DLTrajectory possibilities by number of pigs in the way
		Collections.sort(_possibleDLTrajectories, new pigCountComparator());

		_selectedDLTrajectory = selectBestTrajectoryWRTAvailableBirds();

		return _selectedDLTrajectory.heuristicUtility;
	}

	/**
	 ** searches around the target for a better trajectory
	 **/
	private void searchAroundTheTarget(int i, Point tmpTargetCenterPoint, ABObject tmpTargetObject) {
		int radius = _currentState._birdOnSling.getBirdRadius();

		if (_currentState._birdOnSling == ABType.WhiteBird) {
			if (i == 0) {
				return;
			}
			radius = (int) (radius * 0.9);
		}

		Point tmpTargetPoint = new Point(tmpTargetCenterPoint.x + i * radius, tmpTargetCenterPoint.y);

		estimateTrajectories(tmpTargetPoint, tmpTargetObject, false);

		if (_currentState._birdOnSling != ABType.WhiteBird) {
			tmpTargetPoint = new Point(tmpTargetCenterPoint.x, tmpTargetCenterPoint.y + i * radius);

			estimateTrajectories(tmpTargetPoint, tmpTargetObject, false);
		}

	}

	/**
	 * Finds and calculates a trajectory and its utilities for a given target
	 * object. (pig)
	 */
	protected void estimateTrajectories(Point tmpTargetPoint, ABObject tmpTargetObject, boolean centerShot) {
		// create list with all objects
		ArrayList<ABObject> tmpHillsAndBlocks = new ArrayList<ABObject>(_currentState._hills);

		tmpHillsAndBlocks.addAll(_currentState._blocks);

		ArrayList<Point> pts = null;

		// estimate launch point
		if (noHighShotFound == false) {
			pts = _tp.estimateLaunchPoint(_currentState._sling, tmpTargetPoint, _currentState._hills,
					_currentState._blocks, tmpTargetObject, _currentState._birdOnSling);
		} else {
			pts = _tp.estimateLaunchPoint(_currentState._sling, tmpTargetPoint);
		}

		for (Point tmpReleasePoint : pts) {
			// create new instance of DLTrajectory
			DLTrajectory tmpDLTrajectory = new DLTrajectory(_actionRobot, _tp, _currentState._sling,
					_currentState._birdOnSling, tmpReleasePoint, tmpTargetPoint, tmpTargetObject, _currentState._hills,
					_currentState._blocks, _currentState._pigs);

			// compute heuristic utility
			for (ABObject tmp : tmpHillsAndBlocks) {
				double dist = distance(tmpTargetPoint, new Point((int) tmp.getCenterX(), (int) tmp.getCenterY()));

				tmpDLTrajectory.trajectoryUtility += (dist / 100.0)
						* tmpDLTrajectory._birdsBlocksDamageTrajectory[whichBird()][tmp.type.id];
				/*** Can't tell you everything... ;) ***/

				// preffer center shot
				/*** Can't tell you everything... ;) ***/

				// add trajectory to possible trajectories
				_possibleDLTrajectories.add(tmpDLTrajectory);
			}
		}
	}
	
	protected int whichBird() {
		int bird=0;
		switch (_currentState._birdOnSling) {
		case RedBird:
			bird=0;
			break; // start of trajectory
		case YellowBird:
			bird=1;
			break;
		case WhiteBird:
			bird=4;
			break; // 70-90% of the way
		case BlackBird:
			bird=3;
			break; // 70-90% of the way
		case BlueBird:
			bird=2;
			break; // 65-85% of the way
		default:
			bird = 0;
		}
		return bird;
	}

	/**
	 * This function calculates when to tap for a given bird and given target
	 * object. This information is later passed to the server.
	 * 
	 * @return the time in milliseconds.
	 */
	@Override
	protected int getTapInterval() {
		int tapInterval = 0;
		int collision = 100;

		if (_currentState._birdOnSling == ABType.YellowBird || _currentState._birdOnSling == ABType.BlueBird
				|| noHighShotFound == true) {
			collision = _selectedDLTrajectory
					.getPercentageOfTheTrajectoryWhenTheFirstObjectIsHit(_currentState._blocks);
		}

		switch (_currentState._birdOnSling) {
		case RedBird:
			tapInterval = 0;
			break; // start of trajectory
		case YellowBird:
			/*** Can't tell you everything... ;) ***/
			break;
		case WhiteBird:
			/*** Can't tell you everything... ;) ***/
			break; // 70-90% of the way
		case BlackBird:
			/*** Can't tell you everything... ;) ***/
			break; // 70-90% of the way
		case BlueBird:
			/*** Can't tell you everything... ;) ***/
			break; // 65-85% of the way
		default:
			tapInterval = 60;
		}

		if (_currentState._birdOnSling == ABType.WhiteBird && noHighShotFound == false) {
			/*** Can't tell you everything... ;) ***/
		}

		int ret = _tp.getTapTime(_currentState._sling, _selectedDLTrajectory.releasePoint,
				_selectedDLTrajectory.targetPoint, tapInterval);

		if (_currentState._birdOnSling == ABType.BlackBird) {
			/*** Can't tell you everything... ;) ***/
		}

		return ret;
	}

	/**
	 * @return ID of the heuristic for log purposes.
	 */
	@Override
	public int getHeuristicId() {
		return 2;
	}
}
