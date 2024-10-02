package edu.boun.edgecloudsim.network.topoGraph;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.scoring.BetweennessCentrality;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.alg.shortestpath.YenKShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.*;

public abstract class topoGraph {
    protected int nodeCount; // 节点数
    protected int edgeCount; // 边数
    protected int dataCenterCount;//数据中心数
    protected int hostCount;//主机数
    protected int[] dataCenters;
    protected int[] host;
    protected int[] node;
    YenKShortestPath<Integer, DefaultEdge> kShortestPaths;

    protected Graph<Integer, DefaultEdge> graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
    Graph<Integer, DefaultWeightedEdge> wGraph;
    public Map<Integer,List<List<DefaultEdge>>> nodeToDcmap = new HashMap<>();
    public static topoGraph instance = null;
    public abstract void addHost();
    public abstract int getHostDatecenter(int host);
    public abstract int[] getHotNode();
    public int[] getHost(){return this.host;}

    public Map<String,List<List<DefaultEdge>>> KShotestRoute=new HashMap<>();
    public Map<Integer,List<List<DefaultEdge>>> KRoutetoHost = new HashMap<>();
    public int k=4;

    public topoGraph getInstance() {
        return instance;
    }

    private void printDeg(){
        for (Integer vertex : graph.vertexSet()) {//输出节点度
            int degree = graph.degreeOf(vertex);
            if(degree>2)
                System.out.println("Vertex " + vertex + " has degree: " + degree);
        }
    }

    public void printBetweenness(){// 计算中间中心性
        BetweennessCentrality<Integer, DefaultEdge> betweennessCentrality = new BetweennessCentrality<>(graph);
        Map<Integer, Double> centralityMap = betweennessCentrality.getScores();
        // 输出每个节点的中间中心性
        for (Map.Entry<Integer, Double> entry : centralityMap.entrySet()) {
            System.out.println("Node " + entry.getKey() + " betweenness centrality: " + entry.getValue());
        }

//        List<Map.Entry<Integer, Double>> sortedEntries = centralityMap.entrySet().stream()
//                .sorted(Map.Entry.comparingByValue()) // 自然顺序（升序）
//                .collect(Collectors.toList());
//        for (Map.Entry<Integer, Double> entry : sortedEntries) {
//            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
//        }

    }

    protected void initialize(int nodeCount, int edgeCount,int hostCount,int dataCenterCount) {
        this.nodeCount = nodeCount;
        this.edgeCount = edgeCount;
        this.hostCount = hostCount;
        this.dataCenterCount = dataCenterCount;
//        printBetweenness();
    }


    public void buildWeightGraph(){
        wGraph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.vertexSet().forEach(wGraph::addVertex);
        Set<DefaultEdge> edges = graph.edgeSet();
        for (DefaultEdge originalEdge : edges) {
            Integer src = graph.getEdgeSource(originalEdge);
            Integer target = graph.getEdgeTarget(originalEdge);
            DefaultWeightedEdge weightedEdge = wGraph.addEdge(src, target);
            wGraph.setEdgeWeight(weightedEdge, 1);
        }
    }

    public boolean isEdges(int nodeA, int nodeB) {
        if(graph.containsEdge(nodeA, nodeB)) return true;
        return false;
    }

    public int getHostFromIndex(int dCindex, int hostindex){
        return nodeCount+dCindex*4+hostindex;
    }

    public int[] getDataCenters(){return dataCenters;}

    public List<Integer> getNeighbors(int v){// 获取相邻的顶点
        Set<DefaultEdge> edges = graph.edgesOf(v);
        List<Integer> list = new ArrayList<>();

        for (DefaultEdge edge : edges) list.add(graph.getEdgeTarget(edge));
        return list;
    }

    public DefaultEdge getDcEdge(int s, int d){return graph.getEdge(dataCenters[s],d);}

    public List<DefaultEdge> getHostEdges(){
        List<DefaultEdge> result = new ArrayList<>();
        int[] hosts = getHost();
        for (int i=0;i<hostCount;i++){
            Set<DefaultEdge> edges = graph.edgesOf(hosts[i]);
            for(DefaultEdge edge :edges) result.add(edge);
        }
        return result;
    }

    public Set<DefaultEdge> getEdges(){
        return graph.edgeSet();
    }

    public int getHostCount(){
        return hostCount;
    }

    public int getNodeCount(){
        return nodeCount;
    }
    public int getEdgeCount(){
        return edgeCount;
    }

    public int[] getRouteNode(){
        int[] array = new int[nodeCount];
        for (int i = 0; i < nodeCount; i++) array[i] = i;
        return array;
    }


    public int getEdgeHost(DefaultEdge e){
        return graph.getEdgeTarget(e)>=nodeCount?graph.getEdgeTarget(e):graph.getEdgeSource(e);
    }

    public List<DefaultEdge> getShotestPath(int source,int target){
        DijkstraShortestPath<Integer, DefaultEdge> dijkstra = new DijkstraShortestPath<>(graph);
        GraphPath<Integer, DefaultEdge> path = dijkstra.getPath(source, target);
        if(path==null)
            System.out.println(1);
        List<DefaultEdge> edgeList = path.getEdgeList();
        return edgeList;
    }

    public List<DefaultEdge> getShotestPath(int source,double[] weight){
        int t = 0;
        for (DefaultWeightedEdge originalEdge : wGraph.edgeSet()){
            System.out.println(originalEdge.toString());
            wGraph.setEdgeWeight(originalEdge, weight[t++]);
        }

        DijkstraShortestPath<Integer, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(wGraph);

        double min=Double.MAX_VALUE;
        List<DefaultWeightedEdge> minPath=null;
        int[] host = getHost();
        for(int i=0;i<host.length;i++){
            GraphPath<Integer, DefaultWeightedEdge> path = dijkstra.getPath(source, host[i]);
            List<DefaultWeightedEdge> edgeList = path.getEdgeList();
            double pathWeight = 0;
            for(DefaultWeightedEdge edge:edgeList) pathWeight+=wGraph.getEdgeWeight(edge);
            if(pathWeight<min){
                min = pathWeight;
                minPath = edgeList;
            }
        }
        List<DefaultEdge> result = new ArrayList<>();
        for(DefaultWeightedEdge edge:minPath){
            int src = wGraph.getEdgeSource(edge),dst = wGraph.getEdgeTarget(edge);
            result.add(graph.getEdge(src,dst));
        }
        return result;
    }

    public List<DefaultEdge> getShotestPath(int source,Map<String, Double> weight){
        int t = 0;
        for (DefaultWeightedEdge originalEdge : wGraph.edgeSet()){
//            wGraph.setEdgeWeight(originalEdge, weight[t++]);
            wGraph.setEdgeWeight(originalEdge, weight.get(originalEdge.toString()));
        }

        DijkstraShortestPath<Integer, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(wGraph);

        double min=Double.MIN_VALUE;
        List<DefaultWeightedEdge> minPath=null;
        int[] host = getHost();
        for(int i=0;i<host.length;i++){
            GraphPath<Integer, DefaultWeightedEdge> path = dijkstra.getPath(source, host[i]);
            List<DefaultWeightedEdge> edgeList = path.getEdgeList();
            double pathWeight = 0;
            for(DefaultWeightedEdge edge:edgeList) pathWeight+=wGraph.getEdgeWeight(edge);
            pathWeight/=edgeList.size();
            if(pathWeight>min){//max
                min = pathWeight;
                minPath = edgeList;
            }
        }
        List<DefaultEdge> result = new ArrayList<>();
        for(DefaultWeightedEdge edge:minPath){
            int src = wGraph.getEdgeSource(edge),dst = wGraph.getEdgeTarget(edge);
            result.add(graph.getEdge(src,dst));
        }
        return result;
    }

//到数据中心的全局最短路
    public void computeKRoutetoDc(){//计算全图
        int[] routeNode = getRouteNode();
         kShortestPaths = new YenKShortestPath<>(graph);

        for(int i=0;i<dataCenters.length;i++){//获取数据中心到路由节点的最短路
            for(int j=0;j<routeNode.length;j++){
                int source = routeNode[j],target = dataCenters[i];
                String key = Integer.toString(source)+":"+Integer.toString(target);
                List<List<DefaultEdge>> result = new ArrayList<>();
                List<GraphPath<Integer, DefaultEdge>> paths = kShortestPaths.getPaths(source, target, k);
                if(source==target)
                    for(int k=0;k<4;k++) result.add(new ArrayList<>());//对齐数据
                else
                    for(GraphPath<Integer, DefaultEdge> path : paths) result.add(path.getEdgeList());

                KShotestRoute.put(key,result);
            }
        }

        for(int node=0;node<nodeCount;node++){//获取
            List<List<DefaultEdge>> result = new ArrayList<>();
            for(int i=0;i<dataCenters.length;i++){
                String key = Integer.toString(node)+":"+Integer.toString(dataCenters[i]);
                List<List<DefaultEdge>> paths = KShotestRoute.get(key);
                for(List<DefaultEdge> path : paths) result.add(path);
            }
            nodeToDcmap.put(node,result);
        }
        System.out.println(1);
    }

    public List<List<DefaultEdge>> getNodesKRoute(int v){//<节点v到所有数据中心的k条<边组成的路由>>
        return nodeToDcmap.get(v);
    }//从节点v出发到所有数据中心的最短路

    public void computeKRouteToHost(int k){
        for(int j=0;j<nodeCount;j++){
            int source = node[j];
            List<List<DefaultEdge>> listOfLists = new ArrayList<>();

            if(k<hostCount){
                for(int i=0;i<this.hostCount;i++){//节点到每一个主机的最短路，选择最短的k条
                    List<DefaultEdge> route = getShotestPath(source,host[i]);
                    listOfLists.add(route);
                }
            }else{//节点到每一个主机k1条最短路，选择最短的k2条
                for(int i=0;i<this.hostCount;i++){
                    int target = host[i];
                    List<GraphPath<Integer, DefaultEdge>> paths = kShortestPaths.getPaths(source, target, 3);
                    for(GraphPath<Integer, DefaultEdge> path : paths) listOfLists.add(path.getEdgeList());
                }

            }

            PriorityQueue<List<DefaultEdge>> minFourLists = new PriorityQueue<>(Comparator.comparingInt(List::size));

            for (List<DefaultEdge> list : listOfLists) minFourLists.offer(list);
            List<List<DefaultEdge>> list = new ArrayList<>(minFourLists);
            List<List<DefaultEdge>> result = new ArrayList<>();

            for(int i=0;i<k;i++) result.add(list.get(i));

            KRoutetoHost.put(source,result);
        }
    }

    public List<List<DefaultEdge>> getKRouteToHost(int source){return KRoutetoHost.get(source);}

    public List<List<DefaultEdge>> getKRoute(int source,int k){
        List<List<DefaultEdge>> listOfLists = new ArrayList<>();
        for(int i=0;i<this.hostCount;i++){
            int target = host[i];
            List<GraphPath<Integer, DefaultEdge>> paths = kShortestPaths.getPaths(source, target, 5);
            for(GraphPath<Integer, DefaultEdge> path : paths) listOfLists.add(path.getEdgeList());
        }
        PriorityQueue<List<DefaultEdge>> minFourLists = new PriorityQueue<>(Comparator.comparingInt(List::size));

        for (List<DefaultEdge> list : listOfLists) minFourLists.offer(list);
        List<List<DefaultEdge>> list = new ArrayList<>(minFourLists);
        List<List<DefaultEdge>> result = new ArrayList<>();

        for(int i=0;i<k;i++) result.add(list.get(i));
        return result;
    }

    public List<List<DefaultEdge>>  getAllHostPath(int source){//计算源到所有主机的最短路
        int min = 99;
        List<List<DefaultEdge>> routeList = new ArrayList<>();
        for(int i=0;i<this.hostCount;i++){
            List<DefaultEdge> route = getShotestPath(source,host[i]);
            if(route.size()<min){
                min = route.size();
                routeList.clear();
                routeList.add(route);
            }else if(route.size()==min){
                routeList.add(route);
            }
        }
        return routeList;
    }

    public List<DefaultEdge> getLatestHost(int source){
        List<List<DefaultEdge>> routeList = getAllHostPath(source);
        return routeList.get(new Random().nextInt(routeList.size()));
    }

    public List<Double> getNodesNearestHost(int v,int num){
        List<Double> nearestEdgeHostId = new ArrayList<>();
        List<List<DefaultEdge>> routes = getAllHostPath(v);
        for(int i=0;i<4;i++){//只取4个
            List<DefaultEdge> route = routes.get(i);
            nearestEdgeHostId.add((double)getEdgeHost(route.get(route.size()-1))/num);
        }
        return nearestEdgeHostId;
    }

    public List<List<DefaultEdge>> getECMP(int source,int target){
        List<List<DefaultEdge>> listOfLists = new ArrayList<>();
        List<List<DefaultEdge>> routeList = new ArrayList<>();
        List<GraphPath<Integer, DefaultEdge>> paths = kShortestPaths.getPaths(source, target, 5);
        for(GraphPath<Integer, DefaultEdge> path : paths) listOfLists.add(path.getEdgeList());
        int min = 99;
        for(List<DefaultEdge> route : listOfLists){
            if(route.size()<min){
                min = route.size();
                routeList.clear();
                routeList.add(route);
            }else if(route.size()==min){
                routeList.add(route);
            }
        }//2.7%,19%,10%
        return routeList;
    }
    public List<List<DefaultEdge>> getEcmp(int source){
        List<List<DefaultEdge>> routeList = new ArrayList<>();
        List<List<DefaultEdge>> routeAllList = new ArrayList<>();
        for(int i=0;i<this.hostCount;i++){
            List<List<DefaultEdge>> listOfLists = getECMP(source,host[i]);
            for(List<DefaultEdge> route:listOfLists) routeAllList.add(route);
        }
        int min = 99;
        for(List<DefaultEdge> route : routeAllList){
            if(route.size()<min){
                min = route.size();
                routeList.clear();
                routeList.add(route);
            }else if(route.size()==min){
                routeList.add(route);
            }
        }
        return routeList;
    }

    public static final double[] experimentalDelay = {
            /*1 Client*/ 88040.279 /*(Kbps)88M*/,
            /*2 Clients*/ 45150.982 /*(Kbps)*/,
            /*3 Clients*/ 30303.641 /*(Kbps)*/,
            /*4 Clients*/ 27617.211 /*(Kbps)*/,
            /*5 Clients*/ 24868.616 /*(Kbps)*/,
            /*6 Clients*/ 22242.296 /*(Kbps)*/,
            /*7 Clients*/ 20524.064 /*(Kbps)*/,
            /*8 Clients*/ 18744.889 /*(Kbps)*/,
            /*9 Clients*/ 17058.827 /*(Kbps)*/,
            /*10 Clients*/ 15690.455 /*(Kbps)*/,
            /*11 Clients*/ 14127.744 /*(Kbps)*/,
            /*12 Clients*/ 13522.408 /*(Kbps)*/,
            /*13 Clients*/ 13177.631 /*(Kbps)*/,
            /*14 Clients*/ 12811.330 /*(Kbps)*/,
            /*15 Clients*/ 12584.387 /*(Kbps)*/,
            /*15 Clients*/ 12135.161 /*(Kbps)*/,
            /*16 Clients*/ 11705.638 /*(Kbps)*/,
            /*17 Clients*/ 11276.116 /*(Kbps)*/,
            /*18 Clients*/ 10846.594 /*(Kbps)*/,
            /*19 Clients*/ 10417.071 /*(Kbps)*/,
            /*20 Clients*/ 9987.549 /*(Kbps)*/,
            /*21 Clients*/ 9367.587 /*(Kbps)*/,
            /*22 Clients*/ 8747.625 /*(Kbps)*/,
            /*23 Clients*/ 8127.663 /*(Kbps)*/,
            /*24 Clients*/ 7907.701 /*(Kbps)*/,
            /*25 Clients*/ 7887.739 /*(Kbps)*/,
            /*26 Clients*/ 7690.831 /*(Kbps)*/,
            /*27 Clients*/ 7393.922 /*(Kbps)*/,
            /*28 Clients*/ 7297.014 /*(Kbps)*/,
            /*29 Clients*/ 7100.106 /*(Kbps)*/,
            /*30 Clients*/ 6903.197 /*(Kbps)*/,
            /*31 Clients*/ 6701.986 /*(Kbps)*/,
            /*32 Clients*/ 6500.776 /*(Kbps)*/,
            /*33 Clients*/ 6399.565 /*(Kbps)*/,
            /*34 Clients*/ 6098.354 /*(Kbps)*/,
            /*35 Clients*/ 5897.143 /*(Kbps)*/,
            /*36 Clients*/ 5552.127 /*(Kbps)*/,
            /*37 Clients*/ 5207.111 /*(Kbps)*/,
            /*38 Clients*/ 4862.096 /*(Kbps)*/,
            /*39 Clients*/ 4517.080 /*(Kbps)*/,
            /*40 Clients*/ 4172.064 /*(Kbps)*/,
            /*41 Clients*/ 4092.922 /*(Kbps)*/,
            /*42 Clients*/ 4013.781 /*(Kbps)*/,
            /*43 Clients*/ 3934.639 /*(Kbps)*/,
            /*44 Clients*/ 3855.498 /*(Kbps)*/,
            /*45 Clients*/ 3776.356 /*(Kbps)*/,
            /*46 Clients*/ 3697.215 /*(Kbps)*/,
            /*47 Clients*/ 3618.073 /*(Kbps)*/,
            /*48 Clients*/ 3538.932 /*(Kbps)*/,
            /*49 Clients*/ 3459.790 /*(Kbps)*/,
            /*50 Clients*/ 3380.649 /*(Kbps)*/,
            /*51 Clients*/ 3274.611 /*(Kbps)*/,
            /*52 Clients*/ 3168.573 /*(Kbps)*/,
            /*53 Clients*/ 3062.536 /*(Kbps)*/,
            /*54 Clients*/ 2956.498 /*(Kbps)*/,
            /*55 Clients*/ 2850.461 /*(Kbps)*/,
            /*56 Clients*/ 2744.423 /*(Kbps)*/,
            /*57 Clients*/ 2638.386 /*(Kbps)*/,
            /*58 Clients*/ 2532.348 /*(Kbps)*/,
            /*59 Clients*/ 2426.310 /*(Kbps)*/,
            /*60 Clients*/ 2320.273 /*(Kbps)*/,
            /*61 Clients*/ 2283.828 /*(Kbps)*/,
            /*62 Clients*/ 2247.383 /*(Kbps)*/,
            /*63 Clients*/ 2210.939 /*(Kbps)*/,
            /*64 Clients*/ 2174.494 /*(Kbps)*/,
            /*65 Clients*/ 2138.049 /*(Kbps)*/,
            /*66 Clients*/ 2101.604 /*(Kbps)*/,
            /*67 Clients*/ 2065.160 /*(Kbps)*/,
            /*68 Clients*/ 2028.715 /*(Kbps)*/,
            /*69 Clients*/ 1992.270 /*(Kbps)*/,
            /*70 Clients*/ 1955.825 /*(Kbps)*/,
            /*71 Clients*/ 1946.788 /*(Kbps)*/,
            /*72 Clients*/ 1937.751 /*(Kbps)*/,
            /*73 Clients*/ 1928.714 /*(Kbps)*/,
            /*74 Clients*/ 1919.677 /*(Kbps)*/,
            /*75 Clients*/ 1910.640 /*(Kbps)*/,
            /*76 Clients*/ 1901.603 /*(Kbps)*/,
            /*77 Clients*/ 1892.566 /*(Kbps)*/,
            /*78 Clients*/ 1883.529 /*(Kbps)*/,
            /*79 Clients*/ 1874.492 /*(Kbps)*/,
            /*80 Clients*/ 1865.455 /*(Kbps)*/,
            /*81 Clients*/ 1833.185 /*(Kbps)*/,
            /*82 Clients*/ 1800.915 /*(Kbps)*/,
            /*83 Clients*/ 1768.645 /*(Kbps)*/,
            /*84 Clients*/ 1736.375 /*(Kbps)*/,
            /*85 Clients*/ 1704.106 /*(Kbps)*/,
            /*86 Clients*/ 1671.836 /*(Kbps)*/,
            /*87 Clients*/ 1639.566 /*(Kbps)*/,
            /*88 Clients*/ 1607.296 /*(Kbps)*/,
            /*89 Clients*/ 1575.026 /*(Kbps)*/,
            /*90 Clients*/ 1542.756 /*(Kbps)*/,
            /*91 Clients*/ 1538.544 /*(Kbps)*/,
            /*92 Clients*/ 1534.331 /*(Kbps)*/,
            /*93 Clients*/ 1530.119 /*(Kbps)*/,
            /*94 Clients*/ 1525.906 /*(Kbps)*/,
            /*95 Clients*/ 1521.694 /*(Kbps)*/,
            /*96 Clients*/ 1517.481 /*(Kbps)*/,
            /*97 Clients*/ 1513.269 /*(Kbps)*/,
            /*98 Clients*/ 1509.056 /*(Kbps)*/,
            /*99 Clients*/ 1504.844 /*(Kbps)*/,
            /*100 Clients*/ 1500.631 /*(Kbps)*/
    };
}
