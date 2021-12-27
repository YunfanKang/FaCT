package edu.ucr.emp;

import py4j.GatewayServer;

public class EMPGateway {
    public static String returnTrue(){
        return "TRUE!";
    }
    public static void EMP_set_Input(String fileName,
                                     String minAttrName,
                                     Double minAttrLow,
                                     Double minAttrHigh,
                                     String maxAttrName,
                                     Double maxAttrLow,
                                     Double maxAttrHigh,
                                     String avgAttrName,
                                     Double avgAttrLow,
                                     Double avgAttrHigh,
                                     String sumAttrName,
                                     Double sumAttrLow,
                                     Double sumAttrHigh,
                                     Double countLow,
                                     Double countHigh,
                                     String distAttrName){
        try {
            EMP.set_input(fileName,
                    minAttrName,
                    minAttrLow,
                    minAttrHigh,
                    maxAttrName,
                    maxAttrLow,
                    maxAttrHigh,
                    avgAttrName,
                    avgAttrLow,
                    avgAttrHigh,
                    sumAttrName,
                    sumAttrLow,
                    sumAttrHigh,
                    countLow,
                    countHigh,
                    distAttrName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String args[]){
        GatewayServer gatewayServer = new GatewayServer(new EMPGateway());
        gatewayServer.start();
        System.out.println("Gateway Server Started");
    }
}
