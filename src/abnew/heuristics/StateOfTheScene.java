package abnew.heuristics;

import java.awt.Point;
import java.awt.Rectangle;

import java.util.List;

import ab.vision.ABObject;
import ab.vision.ABType;
/**
**  this class encapsulates the info about the scene that is used later in the different strategies -
*   position of birds, hills, pigs, blocks, etc.
**/

public class StateOfTheScene {
	
	//ABObject = type, area, shape, hollow or not , id, angle 
	
	public final List<ABObject> _birds;

    public final List<ABObject> _pigs;
    
    public final List<ABObject> _hills;
    
    public final List<ABObject> _blocks;
    
    public final Rectangle _sling;

    public final List<ABObject> _TNTs; 
    
    public final ABType _birdOnSling;
    
    public Point _prevTarget;
    
    public boolean _firstShot;
    
    public StateOfTheScene(List<ABObject> pigs,List<ABObject> hills, List<ABObject> blocks,  Rectangle sling, List<ABObject> TNTs, Point prevTarget, boolean firstShot, List<ABObject> birds, ABType birdOnSling)
    {
    	_birds = birds;
        _birdOnSling = birdOnSling;      

        _pigs = pigs;
        _hills = hills;
        _blocks = blocks;
        _sling = sling;
        _TNTs = TNTs;

        _prevTarget = prevTarget;
        _firstShot = firstShot;
    }

}
