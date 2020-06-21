package abnew.heuristics;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;

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

	}

	/**
	 * This function calculates when to tap for a given bird and given target
	 * object. This information is later passed to the server.
	 * 
	 * @return the time in milliseconds.
	 */
	@Override
	protected int getTapInterval() {
		return 0;
	}

	/**
	 * @return ID of the heuristic for log purposes.
	 */
	@Override
	public int getHeuristicId() {
		return 2;
	}

}
