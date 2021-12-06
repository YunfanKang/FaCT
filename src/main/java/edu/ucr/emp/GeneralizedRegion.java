package edu.ucr.emp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class GeneralizedRegion {
    List<Integer> areaList;
    int id;
    int numOfAreas;
    double average, max, min, sum;
    double acceptLow, acceptHigh, thresholdHigh, thresholdLow;
    Set<Integer> areaNeighborSet;

    //Set<Integer>
    GeneralizedRegion(double thresholdHigh, double thresholdLow, int id){
        numOfAreas = 0;
        average = 0;
        this.id = id;
        this.acceptLow = thresholdLow;
        this.acceptHigh = thresholdHigh;
        this.thresholdHigh = thresholdHigh;
        this.thresholdLow = thresholdLow;
        this.areaNeighborSet = new HashSet<Integer>();
        this.areaList = new ArrayList<Integer>();
        this.sum = 0;
        this.max = -Double.POSITIVE_INFINITY;
        this.min = Double.POSITIVE_INFINITY;
    }
    public boolean addArea(Integer id, int minAttrVal, int maxAttrVal, int avgAttrVal, int sumAttrVal, SpatialGrid sg){
        if (areaList.contains(id)){
            System.out.println("Area is already contained in the current area!");
            return false;
        }else{
            areaList.add(id);
            areaNeighborSet.remove(id);
            for(Integer a: sg.getNeighbors(id)){
                if (!areaList.contains(a)){
                    areaNeighborSet.add(a);
                }
            }
            average = (average * numOfAreas + avgAttrVal) / (numOfAreas + 1);
            numOfAreas ++;
            sum += sumAttrVal;
            if (min < minAttrVal){
                min = minAttrVal;
            }
            if (max > maxAttrVal){
                max = maxAttrVal;
            }
            acceptLow = thresholdLow * (numOfAreas + 1) - average * numOfAreas;
            acceptHigh = thresholdHigh * (numOfAreas +1) - average * numOfAreas;
            return true;
        }

    }
    public boolean removeArea(Integer id, ArrayList<Integer> minAttr, ArrayList<Integer> maxAttr, ArrayList<Integer> avgAttr, ArrayList<Integer>sumAttr, SpatialGrid sg){
        if (!areaList.contains(id)){
            System.out.println("Area to be removed is not in the region");
            return false;
        }else{
            areaList.remove(id);
            areaNeighborSet.add(id);
            numOfAreas --;
            this.average = (this.average * (this.numOfAreas + 1) - avgAttr.get(id) )/this.numOfAreas;
            this.sum -= sumAttr.get(id);
            this.max = -Double.POSITIVE_INFINITY;
            this.min = Double.POSITIVE_INFINITY;
            this.areaNeighborSet.clear();
            for(Integer area: this.areaList){
                if(this.max < maxAttr.get(area)){
                    this.max = maxAttr.get(area);
                }
                if(this.min > minAttr.get(area)){
                    this.min = minAttr.get(area);
                }
                for(Integer a: sg.getNeighbors(id)){
                    if (!areaList.contains(a)){
                        areaNeighborSet.add(a);
                    }
                }

            }
            return true;
        }
    }
    public double getAverage(){
        return this.average;
    }
    public Set<Integer> getAreaNeighborSet(){
        return areaNeighborSet;
    }
    public Set<Integer> getRegionNeighborSet(int[] labels){
        Set <Integer> regionNeighborSet = new HashSet<Integer>();
        for(Integer a: this.areaNeighborSet){
            if(labels[a] == 0)
                continue;
            regionNeighborSet.add(labels[a]);
        }
        return regionNeighborSet;
    }
    public void updateId(int newId, int[] labels){
        this.id = newId;
        for(Integer i: areaList){
            labels[i] = newId;
        }
    }
    public List<Integer> getAreaList(){
        return this.areaList;
    }
    public double getAcceptLow(){
        return acceptLow;
    }
    public double getAcceptHigh(){
        return acceptHigh;
    }

    public int getId() {
        return this.id;
    }

    public Region mergeWith(Region expandR, int labels[], SpatialGrid sg) {
        Region tmpR = new Region(this.thresholdHigh, this.thresholdLow, -1);
        for(Integer a: this.areaList){
            tmpR.addArea(a, labels[a], sg);
        }
        for(Integer a: expandR.getAreaList()){
            tmpR.addArea(a, labels[a], sg);
        }
        return tmpR;
    }
}