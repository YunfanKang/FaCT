package edu.ucr.gmp;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.*;

public class MaxP {
    public static void main(String[] args) throws Exception {
        int[] tList = {1000, 5000, 10000, 15000, 20000, 25000, 30000, 35000, 40000}; //List of threshold
        for(int i = 0; i < tList.length; i++){
            System.out.println(tList[i]+ "： ");
            set_input(tList[i]);
        }
    }
    public static void  set_input(int threshold) throws Exception {
        double startTime = System.currentTimeMillis()/1000.0;
        //File file = new File("data/larger_datasets/Archive/10K/10K.shp");
        File file = new File("data/SCA/SouthCal_noisland.shp");     //  Specify the dataset
        Map<String, Object> map = new HashMap<>();
        map.put("url", file.toURI().toURL());

        DataStore dataStore = DataStoreFinder.getDataStore(map);
        String typeName = dataStore.getTypeNames()[0];

        FeatureSource<SimpleFeatureType, SimpleFeature> source =
                dataStore.getFeatureSource(typeName);
        Filter filter = Filter.INCLUDE; // ECQL.toFilter("BBOX(THE_GEOM, 10,20,30,40)")
        ArrayList<Long> population = new ArrayList<>();
        ArrayList<Long> income = new ArrayList<>();
        ArrayList<SimpleFeature> fList = new ArrayList<>();
        ArrayList<Integer> idList = new ArrayList<>();
        FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
        double maxX = - Double.POSITIVE_INFINITY, maxY = -Double.POSITIVE_INFINITY;
        try (FeatureIterator<SimpleFeature> features = collection.features()) {
            while (features.hasNext()) {
                SimpleFeature feature = features.next();
                //System.out.print(feature.getID());
                //System.out.print(": ");
                population.add(Long.parseLong(feature.getAttribute("pop2010").toString())); //Specify the spatially extensive attribute
                income.add(Long.parseLong(feature.getAttribute("households").toString()));  //Specify the dissimilarity attribute
                fList.add(feature);
                //System.out.println(feature.getID());
                idList.add(Integer.parseInt(feature.getID().split("\\.")[1]) - 1);
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                double cminx = geometry.getEnvelope().getCoordinates()[0].getX();
                double cminy = geometry.getEnvelope().getCoordinates()[0].getY();
                double cmaxx = geometry.getEnvelope().getCoordinates()[2].getX();
                double cmaxy = geometry.getEnvelope().getCoordinates()[2].getY();
                if (minX > cminx){
                    minX = cminx;
                }
                if (minY > cminy){
                    minY = cminy;
                }
                if (maxX < cmaxx){
                    maxX = cmaxx;
                }
                if (maxY < cmaxy){
                    maxY = cmaxy;
                }
            }
        }
        double dataLoadTime = System.currentTimeMillis()/1000.0;
        System.out.println("Time for data loading: " + (dataLoadTime - startTime));
        //System.out.println(income.size());
        //System.out.println(population.size());
        /*int [][] distanceMatrix = pdist(income);
        for(int i = 0; i < 10; i++){
            for (int j = 0; j < 10; j++){
                System.out.print(distanceMatrix[i][j] + " ");
            }
            System.out.println();
        }*/
        SpatialGrid sg = new SpatialGrid(minX, minY, maxX, maxY);
        sg.createIndex(45, fList);
        sg.calculateContiguity(fList);
        //int[] arr = new int[fList.size()];

        FileWriter fw = new FileWriter("Rook.txt", true);
        PrintWriter out = new PrintWriter(fw);
        for(int i = 0; i < fList.size(); i++){
           out.println(sg.getNeighbors(i));
        //arr[i] = i;
        }
        out.close();
        //for(int i = 0; i < idList.size(); i++){
        //System.out.println(idList.get(i) + " " + idList.get(idList.size()-i-1));
        //}


        double rookTime = System.currentTimeMillis()/1000.0;
        System.out.println("Time for rook calculation: " + (rookTime - dataLoadTime));
        RegionCollection rc = construction_phase(population, income, 100, sg, idList, threshold);
        //double constructionTime = System.currentTimeMillis()/1000.0;
        //System.out.println("Time for construction phase:" + (constructionTime - rookTime));
        int max_p = rc.getMax_p();
        System.out.println("MaxP: " + max_p);
        Map<Integer, Long> regionSpatialAttr = rc.getRegionSpatialAttr();
        /*System.out.println("regionSpatialAttr after construction_phase:");
        for(Map.Entry<Integer, Integer> entry: regionSpatialAttr.entrySet()){
            Integer rid = entry.getKey();
            Integer rval = entry.getValue();
            System.out.print(rid + ": ");
            System.out.print(rval + " ");
            //System.out.println();
        }*/

        int totalWDS = calculateWithinRegionDistance(rc.getRegionList(), pdist((income)));
        System.out.println("totalWithinRegionDistance before tabu: " + totalWDS);
        int tabuLength = income.size();
        int max_no_move = income.size();
        double constructionTime = System.currentTimeMillis()/1000.0;
        System.out.println("Time for construction phase:" + (constructionTime - rookTime));
        performTabu(rc.getLabels(), rc.getRegionList(), regionSpatialAttr, population, sg, pdist((income)), threshold, tabuLength, max_no_move);

        double endTime = System.currentTimeMillis()/1000.0;
        System.out.println("Time for tabu(ms): " + (endTime - constructionTime));
        System.out.println("total time: " +(endTime - startTime));

        //long endTime = System.currentTimeMillis();
    }
    public static RegionCollection construction_phase(ArrayList threshold_attr, ArrayList dis_attr, int max_it, SpatialGrid r, ArrayList<Integer> idList, int spatialThre){
        //Map<Integer, Integer> labels_list = new HashMap<Integer, Integer>();

        /*for(int i = 0; i < idList.size(); i++){
            labels_list.put(idList.get(i), 0);
        }*/

        //Distance Matrix and Pairwise dist
        int max_p = 0;
        RegionCollection rc = null;
        long[][] distanceMatrix = pdist((dis_attr));

        for (int i = 0; i < max_it  ; i++){
            //System.out.println("i: " + i + " max_p: " + max_p);
            int[] labels = new int[dis_attr.size()];
            int cId = 0;
            List<Integer> enclave = new ArrayList<Integer>();
            Map<Integer, List<Integer>> regionList = new HashMap<Integer, List<Integer>>();
            Map<Integer, Long> regionSpatialAttr = new HashMap<Integer, Long>();
            //List<> regionList = {};
            Collections.shuffle(idList,new Random());
            for (int arr_index = 0; arr_index < threshold_attr.size(); arr_index++){
                Integer p = idList.get(arr_index);
                if(labels[p] != 0){
                    continue;
                }
                List<Integer> neighbotPolys = new ArrayList<Integer>();
                for (Integer j: r.getNeighbors(p)){
                    neighbotPolys.add(j);
                }
                if (neighbotPolys.size() < 1){
                    labels[p] = -1;
                }else{
                    cId += 1;
                    Area a = growClusterForPoly(labels, threshold_attr, p, neighbotPolys, cId, r, distanceMatrix, spatialThre);
                    if (a.getSpatialAttriTotal() < spatialThre){
                        for(Integer j: a.getLabeledID()){
                            enclave.add(j);
                        }
                    }else{
                        regionList.put(cId, a.getLabeledID());
                        regionSpatialAttr.put(cId, a.getSpatialAttriTotal());
                    }
                }
            }
            int num_regions = regionList.size();
            /*for(int j = 0; j < labels.length; j++){
                System.out.print(labels[j] + " ");
            }
            //System.out.println(labels);
            for(Map.Entry<Integer, List<Integer>> entry: regionList.entrySet()){
                Integer rid = entry.getKey();
                List<Integer> alist = entry.getValue();
                System.out.print(rid + ": ");
                for(Integer j: alist){
                    System.out.print(j + " ");
                }
                System.out.println();
            }*/
            //System.out.println(regionList);
            if (num_regions < max_p){
                continue;
            }else{
                max_p = num_regions;
                rc = assignEnclave(max_p, enclave, labels, regionList, regionSpatialAttr, threshold_attr, r, distanceMatrix);
            }
        }
        return rc;







    }
    public static Area growClusterForPoly(int[] labels, ArrayList<Long> spatially_extensive_attr, Integer p, List<Integer> neighborPolys, Integer c, SpatialGrid r, long[][] distanceMatrix, int spatialThre){
        labels[p] = c;
        List<Integer> labeledID = new ArrayList<Integer>();
        labeledID.add(p);
        long spatialAttriTotal = spatially_extensive_attr.get(p);

        while(true){
            if (spatialAttriTotal >= spatialThre || neighborPolys.size() == 0)
                break;
            /*remainMatrix = distanceMatrix[labeledID, :][:, NeighborPolys]
            columnSum = np.sum(remainMatrix, axis=0)
            curIndex = columnSum.argmin()
            closestUnit = NeighborPolys[curIndex]  */
            //long minColumnSum = 1000000000;
            double minColumnSum = Double.POSITIVE_INFINITY;
            int closestUnitIndex = -1;
            for(int i = 0; i < neighborPolys.size(); i++){
                long columnSum = 0;
                for (Integer j: labeledID){
                    columnSum = columnSum + distanceMatrix[j][neighborPolys.get(i)];
                }
                if (columnSum < minColumnSum){
                    closestUnitIndex = i;
                    minColumnSum = columnSum;
                }

            }
            int closestUnit = neighborPolys.get(closestUnitIndex);
            if (labels[closestUnit] == 0){
                labels[closestUnit] = c;
                labeledID.add(closestUnit);
                spatialAttriTotal += spatially_extensive_attr.get(closestUnit);
                if (spatialAttriTotal < spatialThre){
                    List<Integer> PnNeighborPolys = new ArrayList<>(r.getNeighbors(closestUnit));
                    for(Integer pnn: PnNeighborPolys){
                        if(labels[pnn] == 0 && !neighborPolys.contains(pnn)){
                            neighborPolys.add(pnn);
                        }
                    }
                    neighborPolys.remove(closestUnitIndex);
                }else{
                    break;
                }
            }else{
                neighborPolys.remove(closestUnitIndex);
            }
        }
        return new Area(labeledID, spatialAttriTotal);
    }
    public static RegionCollection assignEnclave(int max_p, List<Integer> enclave, int[] labels, Map<Integer, List<Integer>> regionList,Map<Integer, Long> regionSpatialAttr, List<Long> spatially_extensive_attr, SpatialGrid r, long[][] distanceMatrix){
        int enclave_index = 0;
        while(enclave.size() > 0){
            //System.out.println(enclave.size());
            if(enclave_index == enclave.size()){
                //System.out.println("End of enclave set");
                //System.out.println(enclave_index);
                enclave_index = 0;

            }
            int ec = enclave.get(enclave_index);
            List<Integer> ecNeighbors = new ArrayList<>(r.getNeighbors(ec));
            //System.out.println(ecNeighbors.size());
            double minDistance = Double.POSITIVE_INFINITY;
            int assignedRegion = 0;
            List <Integer> ecNeighborsList = new ArrayList<Integer>();
            List<Integer> ecTopNeighborsList = new ArrayList<Integer>();

            for (Integer ecn: ecNeighbors){

                if(enclave.contains(ecn))
                    continue;
                List<Integer> rm = regionList.get(labels[ecn]);
                int totalDistance = 0;
                for(Integer i: rm){
                    totalDistance += distanceMatrix[ec][i];
                }
                if (totalDistance < minDistance){
                    minDistance = totalDistance;
                    assignedRegion = labels[ecn];
                }
            }
            if (assignedRegion == 0){
                //System.out.println("Island");
                //System.out.println(ec + " " +    ecNeighbors + " " + labels[ecNeighbors.get(0)]);

                enclave_index += 1;
            }else{
                labels[ec] = assignedRegion;
                regionList.get(assignedRegion).add(ec);
                long newAttr = regionSpatialAttr.get(assignedRegion) + spatially_extensive_attr.get(ec);
                regionSpatialAttr.put(assignedRegion, newAttr);
                enclave.remove(enclave_index);
                enclave_index = 0;
            }
        }
        return new RegionCollection(max_p, labels, regionList, regionSpatialAttr);
    }
    public static void performTabu(int[] initLabels,
                                   Map<Integer, List<Integer>> initRegionList,
                                   Map<Integer, Long> initRegionSpatialAttr,
                                   List<Long> spatially_extensive_attr,
                                   SpatialGrid r,
                                   long[][] distanceMatrix,
                                   int threshold,
                                   int tabuLength,
                                   int max_no_move){
        int ni_move_ct = 0;
        boolean make_move_flag = false;
        List<Move> tabuList = new ArrayList<Move>();
        List<Integer> potentialAreas = new ArrayList<Integer>();
        Move potentialMove = null;
        //potentialMove
        int[] labels = Arrays.copyOf(initLabels, initLabels.length);
        int[] bestLabels = Arrays.copyOf(initLabels, initLabels.length);
        Map<Integer, List<Integer>> regionList = initRegionList;
        Map<Integer, Long> regionSpatialAttrs = initRegionSpatialAttr;
        int withinRegionDistance = calculateWithinRegionDistance(initRegionList, distanceMatrix);
        int bestWDS = withinRegionDistance;

        while (ni_move_ct <= max_no_move){
            if (make_move_flag || potentialAreas.size() == 0){
                potentialAreas = pickMoveArea(labels, initRegionList, regionSpatialAttrs,
                        spatially_extensive_attr,
                        r,
                        distanceMatrix,
                        threshold);
                Double maxDiff = -1.0 / 0.0;
                for(Integer poa: potentialAreas){
                    int donorRegion = labels[poa];
                    List<Integer> poaNeighbor = new ArrayList<>(r.getNeighbors(poa));
                    int lostDistance = 0;
                    for(Integer rn: initRegionList.get(donorRegion)){
                        lostDistance += distanceMatrix[poa][rn];
                    }
                    for (Integer poan: poaNeighbor){
                        int recipientRegion = labels[poan];
                        if(recipientRegion == 0){
                            System.out.println("True");
                        }
                        if(recipientRegion == donorRegion){
                            continue;
                        }else{
                            int addedDistance = 0;
                            for(Integer rn: initRegionList.get(recipientRegion)){
                                addedDistance += distanceMatrix[poa][rn];
                            }
                            int diff = lostDistance - addedDistance;
                            if(diff > maxDiff){
                                maxDiff = diff / 1.0;
                                potentialMove = new Move(poa, donorRegion, recipientRegion);
                            }
                        }
                    }
                }
                if (potentialMove == null){
                    break;
                }
                if (!tabuList.contains(potentialMove)){
                    make_move_flag = true;
                    labels[potentialMove.area] = potentialMove.recipientRegion;
                    regionList.get(potentialMove.donorRegion).remove(potentialMove.area);
                    regionList.get(potentialMove.recipientRegion).add(potentialMove.area);
                    regionSpatialAttrs.put(potentialMove.donorRegion, regionSpatialAttrs.get(potentialMove.donorRegion) - spatially_extensive_attr.get(potentialMove.area));
                    regionSpatialAttrs.put(potentialMove.recipientRegion, regionSpatialAttrs.get(potentialMove.recipientRegion) + spatially_extensive_attr.get(potentialMove.area));
                    withinRegionDistance -= maxDiff;
                    if(withinRegionDistance < bestWDS){
                        bestLabels = Arrays.copyOf(labels, labels.length);
                        bestWDS = withinRegionDistance;
                        if(tabuList.size() == tabuLength){
                            tabuList.remove(0);
                        }
                        tabuList.add(potentialMove);
                        ni_move_ct = 0;
                    }else{
                        ni_move_ct += 1;
                    }
                }else{
                    if(withinRegionDistance - maxDiff < bestWDS){
                        withinRegionDistance = withinRegionDistance - maxDiff.intValue();
                        make_move_flag = true;
                        labels[potentialMove.area] = potentialMove.recipientRegion;
                        regionList.get(potentialMove.donorRegion).remove(potentialMove.area);
                        regionList.get(potentialMove.recipientRegion).add(potentialMove.area);
                        regionSpatialAttrs.put(potentialMove.donorRegion, regionSpatialAttrs.get(potentialMove.donorRegion) - spatially_extensive_attr.get(potentialMove.area));
                        regionSpatialAttrs.put(potentialMove.recipientRegion, regionSpatialAttrs.get(potentialMove.recipientRegion) + spatially_extensive_attr.get(potentialMove.area));

                        bestLabels = Arrays.copyOf(labels, labels.length);
                        bestWDS = withinRegionDistance;
                        if(tabuList.size() == tabuLength){
                            tabuList.remove(0);
                        }
                        tabuList.add(potentialMove);
                        ni_move_ct = 0;
                    }else{
                        ni_move_ct += 1;
                        make_move_flag = false;
                    }
                }

            }
        }

        System.out.println("totalWithinRegionDDistance after Tabu: " + bestWDS);


    }
    public static List<Integer> pickMoveArea(int[] labels, Map<Integer, List<Integer>> regionLists, Map<Integer, Long> regionSpatialAttrs, List<Long> spatially_extensive_attr, SpatialGrid r, long[][] distanceMatrix, int threshold){
        List<Integer> potentialAreas = new ArrayList<Integer>();
        for(Map.Entry<Integer, Long> e: regionSpatialAttrs.entrySet()){
            List<Integer> rla = regionLists.get(e.getKey());
            List<Integer> pas_indices = new ArrayList<Integer>();
            //for(Integer i: rla){
                //rasa.add(spatially_extensive_attr.get(i));
            //}
            for(int i = 0; i < rla.size(); i++){
                if (e.getValue() - spatially_extensive_attr.get(rla.get(i)) > threshold)
                    pas_indices.add(i);
            }
            if (pas_indices.size() > 0){
                for(Integer pasi: pas_indices){
                    List<Integer> pasin = new ArrayList<>(r.getNeighbors(rla.get(pasi)));
                    boolean neighborFromAnotherR = false;
                    for(Integer a: pasin){
                        if(neighborFromAnotherR)
                            break;
                        if(labels[a] != labels[rla.get(pasi)]){
                            neighborFromAnotherR = true;
                        }
                    }
                    if(neighborFromAnotherR){
                        List<Integer> leftAreas = new ArrayList<Integer>();
                        for(Integer i: rla){
                            leftAreas.add(i);
                        }
                        leftAreas.remove(pasi);
                        List<Integer> connectedNeighbor = new ArrayList<Integer>();
                        boolean[] visited =new boolean[leftAreas.size()];
                        for(Integer i: r.getNeighbors(leftAreas.get(0))){
                            connectedNeighbor.add(i);
                        }
                        visited[0] = true;
                        boolean grow = true;
                        while (grow){
                            grow = false;
                            for(int i = 1; i < leftAreas.size(); i++){
                                if(visited[i] == false && connectedNeighbor.contains(leftAreas.get(i))){
                                    visited[i] = true;
                                    for(Integer j: r.getNeighbors(leftAreas.get(i))){
                                        connectedNeighbor.add(j);
                                    }
                                    grow = true;
                                }
                            }
                        }
                        boolean onecc = true;
                        for(int i = 0; i < visited.length; i++){
                            if(visited[i] == false){
                                onecc = false;
                            }
                        }
                        if (onecc){
                            potentialAreas.add(rla.get(pasi));
                        }


                    }
                }
            }else{
                continue;
            }


        }
        return potentialAreas;
    }
    public static int calculateWithinRegionDistance(Map<Integer, List<Integer>> regionList, long[][] distanceMatrix){
        int totalWithinRegionDistance = 0;
        for(Map.Entry<Integer, List<Integer>> entry: regionList.entrySet()){
            int regionDistance = 0;
            for(Integer i: entry.getValue()){
                for(Integer j:entry.getValue()){
                    regionDistance += distanceMatrix[i][j];
                }
            }
            totalWithinRegionDistance += regionDistance / 2;
        }
        return totalWithinRegionDistance;
    }
    
    public static long[][] pdist(ArrayList<Long> attr){
        int attr_size = attr.size();
        long [][] distanceMatrix = new long[attr_size][attr_size];
        for (int i = 0; i < attr_size; i++){
            for (int j = 0; j < attr_size; j++){
                distanceMatrix[i][j] = Math.abs(attr.get(i) - attr.get(j));

            }
        }
        return distanceMatrix;
    }

}

