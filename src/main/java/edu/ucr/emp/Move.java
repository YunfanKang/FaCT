package edu.ucr.gmp;

public class Move{
    public Integer area;
    public Integer donorRegion;
    public Integer recipientRegion;
    public Move(int a, int d, int r){
        area = a;
        donorRegion = d;
        recipientRegion = r;
    }
}