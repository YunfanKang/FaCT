package edu.ucr.gmp;

import java.util.List;
import java.util.Map;

public class RegionCollection{
    private int max_p;
    private int[] labels;
    private Map<Integer, List<Integer>> regionList;
    private Map<Integer, Long> regionSpatialAttr;
    public RegionCollection(int m, int[] l, Map<Integer, List<Integer>> rl, Map<Integer, Long> rsa){
        max_p = m;
        labels = l;
        regionList = rl;
        regionSpatialAttr = rsa;
    }
    public int getMax_p(){
        return max_p;
    }
    public int[] getLabels() {
        return labels;
    }
    public Map<Integer, List<Integer>> getRegionList(){
        return regionList;
    }
    public Map<Integer, Long> getRegionSpatialAttr(){
        return regionSpatialAttr;
    }
}
class Area{
    private List<Integer> labeledID;
    private long spatialAttrTotal;
    public Area(List<Integer> l, long s){
        labeledID = l;
        spatialAttrTotal = s;
    }
    public List<Integer> getLabeledID(){
        return labeledID;
    }
    public long getSpatialAttriTotal(){
        return spatialAttrTotal;
    }

}