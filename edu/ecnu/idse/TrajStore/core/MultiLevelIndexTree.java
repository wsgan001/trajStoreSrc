package edu.ecnu.idse.TrajStore.core;

import org.apache.commons.collections.map.HashedMap;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

/**
 * Created by zzg on 16-2-21.
 */
public class MultiLevelIndexTree implements Serializable{
    private int HASH_NUM;
    private static int DEFAULT_HASH_NUM =3;

    private static int Qurd = 4;
    /*
     the root the tree
     */
    public MLITInternalNode root = null;

    /*
        the map of infoID and info;
        add this, is for the query like this, given a info id, query the information about the CellInfo
     */
    public Map<Integer,CellInfo> maps = new HashedMap(1);

    public MultiLevelIndexTree(CellInfo cellInfo){
        this(DEFAULT_HASH_NUM,cellInfo);
    }

    public MultiLevelIndexTree(int hash_num, CellInfo cellInfo){
        HASH_NUM = hash_num;
        this.root = new MLITInternalNode();
        this.root.setInfo(cellInfo);
        this.root.creatChird();
    }

    /*
        add a file to the tree!
        first find the leaf node, then get the BPlusTree and update the tree
     */
    public void addValue(int leafCellID,int hashID,Long beginTime,String filePath){
        /*instead of a DFS or BFS search of the whole tree;
            we first find the CellInfo it belongs to, and get the central point of the cellInfo;
            the DFS find the LeafNode it belongs to.
        */
        CellInfo cellInfo = maps.get(leafCellID);
        Point cp = cellInfo.getCenterPoint();
        MLITLeafNode leaf = this.root.SpatialPointQueryWithLeafNode(cp);
        if(leaf == null){
            throw new RuntimeException("所提供的带查询叶子节点编号:"+ leafCellID+" 不存在!");
        }

        BPlusTree<Long,String> bpTree = leaf.forests[hashID];
        if(bpTree ==null){
            bpTree = new BPlusTree<>();
            leaf.forests[hashID] = bpTree;
        }
        bpTree.insert(beginTime,filePath);
    }

    /*
        traverse all the leaf node and get the values;
     */
    public void traverseLeaves(){
        recursiveTraverseLeaves(this.root);
    }

    public void recursiveTraverseLeaves(MLITNode p){
        if(p instanceof MLITLeafNode){
            System.out.println(p.info);
            ((MLITLeafNode) p).traverseForest();
        }else{
            for(int i = 0; i < 4; i++ ){
                recursiveTraverseLeaves(((MLITInternalNode)p).children[i]);
            }
        }
    }

    public MLITLeafNode SpatialPointQuery(Point p){
        MLITLeafNode result = this.root.SpatialPointQuery(p);
        return result;
    }



    public List<String> SpatialTemporalPointQuery(Point p){
        MLITLeafNode leafNode = this.SpatialPointQuery(p);
        List<String> aList = new ArrayList<>();
        BPlusTree<Long,String> []forest = leafNode.forests;
        if(forest == null){
            System.out.println("构建森林失败,没有给叶子节点构建Bplustree");
        }
        for(BPlusTree<Long,String> tree : forest){
            if(tree != null){
                String res =  tree.lowerBoundQuery((long)(p.getZ()));
                aList.add(res);
            }
        }

        return aList;
        /*找出该时刻所对应的lowerbound 和 upperbound
        lowerbound 包含该点所对应的文件
            两者都可能不存在
         */
    }

    public CellInfo SpatialQuery(Point point){
        MLITLeafNode result = this.root.SpatialPointQuery(point);
        return  result.getCellInfo();
    }

    /*
        @param rect:  the query spatial range
     */
    public CellInfo[] SpatialRangeQuery(Rectangle rect){
        List<MLITLeafNode> leafNodes = new ArrayList<>();
        this.root.SpatialRangeQuery(rect,leafNodes);
        CellInfo [] results = new CellInfo[leafNodes.size()];
        for(int i = 0; i < leafNodes.size(); i++){
            results[i] = leafNodes.get(i).getCellInfo();
        }
        return  results;
    }


    public List<String> SpatialTemporalRangeQuery(Rectangle rect,long minTime,long maxTime){
        List<MLITLeafNode> leafNodes = new ArrayList<>();
        this.root.SpatialRangeQuery(rect,leafNodes);

        List<String> aList = new ArrayList<>();
        for(MLITLeafNode leafNode:leafNodes){
            BPlusTree<Long,String> []forest = leafNode.forests;
            if(forest == null){
                System.out.println("构建森林失败,没有给叶子节点构建Bplustree");
            }
            for(BPlusTree<Long,String> tree : forest){
                if(tree != null){
                    List<String> tmpResult =  tree.RangeSearchOnLowerBoundKey(minTime,maxTime);
                    aList.addAll(tmpResult);
                }
            }
        }

        return  aList;
    }


    public void insertCell(CellInfo info){
        this.maps.put(info.cellId,info);
        this.root.insert(info);
    }

    public void addNode(CellInfo info){
        insertMLITNode(this.root,info);
    }

    public void printTree(){
        DFSTraversIndex(this.root);
/*
        System.out.println(this.root.info);
        if(((MLITInternalNode)this.root).children !=null){
            for(MLITNode child: ((MLITInternalNode)this.root).children){
                DFSTraversIndex(child);
            }
        }
*/

    }

    //给定infoID查找该info所对应的信息，为Block test函数服务!!!!
    public CellInfo infoQuery(int infoID){
        return maps.get(infoID);
    }

    public void DFSTraversIndex(MLITNode p){
        if(p != null){
            System.out.println(p.info);
            if(p instanceof MLITInternalNode){
                for(MLITNode child: ((MLITInternalNode)p).children){
                    DFSTraversIndex(child);
                }
            }
        }
    }

    public void BFSprintTree(){
        System.out.println("Broad First Search");

        Queue<MLITNode> myQueue = new LinkedList<MLITNode>();
        myQueue.add(root);
        MLITNode pnode = null;
        while(!myQueue.isEmpty()){
            pnode = myQueue.poll();
            System.out.println(pnode.info+"\t"+pnode.layer);

            if(pnode instanceof MLITInternalNode){
                MLITInternalNode internalNode = (MLITInternalNode) pnode;
                for(int i=0;i<4;i++){
                    myQueue.add(internalNode.children[i]);
                }
            }
        }
    }

    public void insertMLITNode(MLITNode node, CellInfo cellInfo){

        //判断已添加的节点是否已经村存在，若是则 修改该节点所对应的ID
        if(node.cellEqual(cellInfo)){
            node.info.set(cellInfo);
            System.out.println(cellInfo.toString() +"  add successfully!");
        }else{//不相同 ,则 该节点变为中间节点，同时使其 重新划分成四个节点
            //找出该节点 为与其副节点的那个方向
            MLITNode parent  = node.parent;
            Point cp = cellInfo.getCenterPoint();

            if(node instanceof MLITLeafNode){

                MLITInternalNode newParent = new MLITInternalNode(node.info,node.layer);
                newParent.creatChird();     //增加4个叶子节点
                newParent.parent = parent;
                int dir = -1;
                for (int i = 0; i < 4; i++) {
                    if(newParent.children[i].contains(cp)){
                        dir = i;
                        break;
                    }
                }
                node = newParent;
                insertMLITNode(((MLITInternalNode)node).children[dir],cellInfo);
            }else {
                //internal node, insert into one of his child
                int dir = -1;
                for (int i = 0; i < 4; i++) {
                    if(((MLITInternalNode)node).children[i].contains(cp)){
                        dir = i;
                        break;
                    }
                }
                insertMLITNode(((MLITInternalNode)node).children[dir],cellInfo);
            }
        }

    }


    class MLITNode implements Serializable{
        public CellInfo info;
        public byte layer;

        public MLITNode parent;

        public MLITNode(){
            info = null;
            parent = null;
            layer = 0;
        }

        public MLITNode(CellInfo cellInfo,byte lay){
            this.info = new CellInfo(cellInfo);
            this.layer = lay;
            parent = null;
        }

        public boolean contains(Point p){
            return info.contains(p);
        }

        public void setInfo(CellInfo cellInfo) {
            if(this.info ==null){
                this.info = new CellInfo(cellInfo);
                ;
            }else{
                this.info.set(cellInfo);
            }
        }

        public void setID(int id){
            this.info.cellId = id;
        }

        public int getID(){
            return this.info.cellId;
        }

        public CellInfo getCellInfo(){
            return  this.info;
        }

        public boolean isIntersected(Rectangle rect){
            return info.isIntersected(rect);
        }

        public boolean cellEqual(CellInfo q) {
            double m1 = this.info.x1;
            double m2 = this.info.y1;
            double m3 = this.info.x2;
            double m4 = this.info.y2;
            double n1 = q.x1;
            double n2 = q.y1;
            double n3 = q.x2;
            double n4 = q.y2;
            double err = 0.000001;

            if ((Math.abs(n1 - m1) < err) && (Math.abs(n2 - m2) < err) && (Math.abs(n3 - m3) < err)
                    && (Math.abs(n4 - m4) < err)) {
                return true;
            } else {
                return false;
            }
        }

    /*    public MLITNode SpatialPointQuery(Point p){
            return new MLITNode();
        }

        public MLITNode SpatialRangeQuery(Rectangle rect){
            return  new MLITNode();

        }
        public MLITNode SpatialTemporalRangeQuery(Rectangle rect,int minTime,int maxTime){
            return  new MLITNode();
        }*/

    }

    class MLITInternalNode extends MLITNode {
        public MLITNode[] children = null;

        public MLITInternalNode() {
            this(new CellInfo(), (byte) 0);
        }

        public MLITInternalNode(CellInfo cellInfo, byte layer) {

            super(cellInfo, layer);
            this.creatChird();
            parent = null;
        }

        public MLITNode[] getChildren() {
            return children;
        }

        public void creatChird() {

            double x1 = this.info.x1;
            double x2 = this.info.x2;
            double y1 = this.info.y1;
            double y2 = this.info.y2;
            double width = (x2 - x1) / 2;
            double height = (y2 - y1) / 2;
            this.children = new MLITNode[Qurd];
            this.children[0] = new MLITLeafNode(new CellInfo(-1, x1, y1 + height, x1 + width, y2), (byte) (this.layer + 1));
            this.children[1] = new MLITLeafNode(new CellInfo(-1, x1 + width, y1 + height, x2, y2), (byte) (this.layer + 1));
            this.children[2] = new MLITLeafNode(new CellInfo(-1, x1, y1, x1 + width, y1 + height), (byte) (this.layer + 1));
            this.children[3] = new MLITLeafNode(new CellInfo(-1, x1 + width, y1, x2, y1 + height), (byte) (this.layer + 1));
            for (int i = 0; i < Qurd; i++) {
                this.children[i].parent = this;
            }
        }

        public void insert(CellInfo cellInfo) {
            if (this.cellEqual(cellInfo)) {
                this.info.set(cellInfo);
            } else {
                int dir = -1;
                Point cp = cellInfo.getCenterPoint();
                for (int i = 0; i < 4; i++) {
                    if (this.children[i].contains(cp)) {
                        dir = i;
                        break;
                    }

                }
                if (this.children[dir] instanceof MLITInternalNode) {
                    ((MLITInternalNode) this.children[dir]).insert(cellInfo);
                } else {
                    ((MLITLeafNode) this.children[dir]).insert(cellInfo, dir);
                }
            }
        }

        public MLITLeafNode SpatialPointQueryWithLeafNode(Point p) {
            if (this.contains(p)) {
                int dir = -1;
                for (int i = 0; i < 4; i++) {
                    if (this.children[i].contains(p)) {
                        dir = i;
                    }
                }
                MLITNode child = this.children[dir];
                if (child instanceof MLITInternalNode) {
                    return ((MLITInternalNode) child).SpatialPointQueryWithLeafNode(p);
                } else {
                    return ((MLITLeafNode) child);
                }
            }
            //该节点不包含 带查询的点
            return null;
        }

        public MLITLeafNode SpatialPointQuery(Point p) {
            if (this.contains(p)) {
                int dir = -1;
                for (int i = 0; i < 4; i++) {
                    if (this.children[i].contains(p)) {
                        dir = i;
                    }
                }
                MLITNode child = this.children[dir];
                if (child instanceof MLITInternalNode) {
                    return ((MLITInternalNode) child).SpatialPointQuery(p);
                } else {
                    return ((MLITLeafNode) child);
                }
            }
            //该节点不包含 带查询的点
            return null;
        }

        public void SpatialRangeQuery(Rectangle rect, List<MLITLeafNode> result) {
            if (this.isIntersected(rect)) {
                for (int i = 0; i < 4; i++) {
                    MLITNode child = this.children[i];
                    if (child.isIntersected(rect)) {
                        // Rectangle joinedRect = child.info.IntersectedRecangle(rect);
                        if (child instanceof MLITInternalNode) {
                            ((MLITInternalNode) child).SpatialRangeQuery(rect, result);
                        } else {
                            result.add((MLITLeafNode) child);
                        }
                    }
                }
            }
        }
  /*      public CellInfo[]  SpatialRangeQuery(Rectangle rect){
            return new MLITLeafNode();
        }

        public CellInfo[] SpatialTemporalRangeQuery(Rectangle rect,int minTime,int maxTime){
            return new MLITLeafNode();
        }*/
    }


    class MLITLeafNode extends MLITNode{
        public BPlusTree<Long, String>[] forests = null;

        public void insert(CellInfo cellInfo,int direction){
            if(this.cellEqual(cellInfo)){
                this.setInfo(cellInfo);
            }else{
                MLITInternalNode parent  = (MLITInternalNode)this.parent;

                MLITInternalNode newParent = new MLITInternalNode(this.info,this.layer);
                newParent.creatChird();     //增加4个叶子节点
                newParent.parent = parent;

                parent.children[direction] = newParent;

                int dir = -1;
                Point cp = cellInfo.getCenterPoint();
                for (int i = 0; i < 4; i++) {
                    if(newParent.children[i].contains(cp)){
                        dir = i;
                        break;
                    }
                }
                //     System.out.println("test print");
                //     printTree();
                this.setInfo(newParent.children[dir].info);
                this.layer = newParent.children[dir].layer;
                this.parent = newParent;

                newParent.children[dir] = this;
                //      System.out.println("test1 print");
                //       printTree();
                this.insert(cellInfo,dir);
            }
        }

        public MLITLeafNode() {
            this(null);
        }

        public MLITLeafNode(CellInfo cellInfo) {
            this(cellInfo, (byte) 0);
        }

        public MLITLeafNode(CellInfo cellInfo, byte layer) {
            info = cellInfo;
            this.layer = layer;
            parent = null;
            forests = new BPlusTree[HASH_NUM];
        }



        public CellInfo SpatialPointQuery(Point p) {

            return this.info;
        }

    /*    public MLITNode SpatialRangeQuery(Rectangle rect) {
            return new MLITLeafNode();
        }

        public MLITNode SpatialTemporalRangeQuery(Rectangle rect, int minTime, int maxTime) {
            return new MLITLeafNode();
        }*/

        public void traverseForest(){
            if(forests == null){
                System.out.println("leaf node construction failure!");
                return;
            }
            for(int i=0;i<forests.length;i++){
                if(forests[i]!=null){
                    System.out.println("the "+i+" tree");
                    forests[i].BFSTraverse();
                }
            }

        }


    }

    /*
    public static  void localPathTest() throws IOException{
        File f = new File("/home/zzg/Downloads/32.txt");
        BufferedReader br = new BufferedReader(new FileReader(f));
        String line = null;
        String[] tokens = null;
        CellInfo rootInfo = new CellInfo(1,115.750000,39.500000,117.200000,40.500000);        ///

        MultiLevelIndexTree mlitree = new MultiLevelIndexTree(3,rootInfo);
        while((line = br.readLine())!=null){
            tokens = line.split(" ");
            System.out.println(line);
            CellInfo info = new CellInfo(Integer.parseInt(tokens[0]),new Rectangle( Double.parseDouble(tokens[2]),
                    Double.parseDouble(tokens[3]),
                    Double.parseDouble(tokens[4]),
                    Double.parseDouble(tokens[5]) ));
            mlitree.insertCell(info);
            //       System.out.println("main print");
            //      mlitree.printTree();
        }

        mlitree.printTree();

        // construct the temporal index from the file list.

        Configuration hadoopConf = new Configuration();
        FileSystem hdfs =  FileSystem.get(hadoopConf);
        String pre = "file:///home/zzg/Blocks/";
        Path namePath = new Path(pre);

        FileStatus[] fStatus = hdfs.listStatus(namePath);
        FSDataInputStream in = null;
        for(int i=0;i<fStatus.length;i++){
            String tmpName = fStatus[i].getPath().getName();
            System.out.println(tmpName);
            if(tmpName.startsWith("-"))
                continue;
            tokens = tmpName.split("-");
            mlitree.addValue(Integer.parseInt(tokens[0]),Integer.parseInt(tokens[1]),Long.parseLong(tokens[2]),pre + tmpName);

            try{
                in = hdfs.open(new Path(pre+tmpName));

                Block bloc = new Block();
                bloc.readFields(in);
                bloc.print();
            }catch (Exception e){
                e.printStackTrace();
                System.out.println(tmpName+" read error!");
            }finally {
                in.close();
            }


            //    mlitree.addValue();
        }
        mlitree.traverseLeaves();
    }
*/
    public static void singleFileReadTest() throws IOException{
       /* String pre = "file:///home/zzg/Blocks/33-2-0-86272";
        Configuration hadoopConf = new Configuration();
        FileSystem hdfs =  FileSystem.get(hadoopConf);
        FSDataInputStream in = hdfs.open(new Path(pre));
        Block bloc = new Block();
        bloc.readFields(in);
        bloc.print();
        in.close();*/
    }

    public static void main(String[] args) throws IOException{
     //  localPathTest();
        //   singleFileReadTest();
    }

}
