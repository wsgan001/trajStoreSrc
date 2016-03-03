package edu.ecnu.idse.TrajStore.core;

/**
 * Created by zzg on 16-2-28.
 */
public interface SpatialTemporalQuery {
    public MultiLevelIndexTree.MLITLeafNode SpatialPointQuery(Point p);
//    public CellInfo[] SpatialRangeQuery(Rectangle rect);
//    public CellInfo[] SpatialTemporalRangeQuery(Rectangle rect,int minTime,int maxTime);
}
