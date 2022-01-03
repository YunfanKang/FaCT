package edu.ucr.emp;

import java.util.Map;
import java.util.Set;

public class IndexedSpatialGrid extends  SpatialGrid{
    public void setNeighbors(Map<Integer, Set<Integer>> n){
        this.neighbors = n;
    }
}
