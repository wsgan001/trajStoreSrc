package edu.ecnu.idse.TrajStore.util

import edu.ecnu.idse.TrajStore.core.{Point, MultiLevelIndexTree, CellInfo}

import scala.util.control.Breaks._

/**
  * Created by zzg on 15-12-23.
  */

object SpatialUtilFuncs {

  def getLocatedRegion(lg:Float, la:Float, mlTree :MultiLevelIndexTree): Int ={
    var spaceID = -1
    val info =  mlTree.SpatialPointQuery(new Point(lg,la,0));
    if(info != null)
       spaceID = info.getID;
    spaceID
  }


  def getCellInfo(infoID:Int,mlTree :MultiLevelIndexTree): CellInfo ={
      mlTree.infoQuery(infoID)
  }

  def getLocatedRegion(lg:Float, la:Float, regions :Array[CellInfo]): Int ={

    var result  = -1
    breakable {
      for (i <- 0 until regions.length) {
        if (regions(i).getMBR.contains(lg, la)) {
          result = regions(i).cellId
          break
        }
      }
    }
    result
  }

  def getLocatedRegion(lg:Double, la:Double, regions :Array[CellInfo]): Int ={
    var result  = -1
    breakable {
      for (i <- 0 until regions.length) {
        if (regions(i).getMBR.contains(lg, la)) {
          result = regions(i).cellId
          break
        }
      }
    }
    result
  }

  def getCellInfo(infoID:Int,regions :Array[CellInfo]): CellInfo ={
      var cellInfo = regions(0)
    breakable{
      for(i <-0 until regions.length){
        if(regions(i).cellId ==infoID){
          cellInfo = regions(i)
          break
        }
      }
    }
    cellInfo
  }

}
