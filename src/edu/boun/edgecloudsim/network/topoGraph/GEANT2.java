package edu.boun.edgecloudsim.network.topoGraph;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class GEANT2 extends topoGraph{

    public void addHost(){
        Arrays.asList(24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39).forEach(graph::addVertex);
        this.dataCenters = new int[]{8, 10, 20,23};//dc
        this.host = new int[]{24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39};
        int t = this.nodeCount;
        for(int dataCenter : dataCenters){
            for(int i=0;i<4;i++){
                graph.addEdge(dataCenter,t);
                t+=1;
            }
        }
    }

    public int getHostDatecenter(int host){
        return host/4;
    }

    public int[] getHotNode(){return new int[]{2,7,13};}//hc

    public GEANT2() {
        instance = this;
        Set<Integer> nodes = new HashSet<>(Arrays.asList(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23
        ));
        nodes.forEach(graph::addVertex);
        this.node = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
        // 添加边
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 6);
        graph.addEdge(1, 9);
        graph.addEdge(2, 3);
        graph.addEdge(2, 4);
        graph.addEdge(3, 6);
        graph.addEdge(4, 7);
        graph.addEdge(5, 3);
        graph.addEdge(5, 8);
        graph.addEdge(6, 9);
        graph.addEdge(6, 8);
        graph.addEdge(7, 11);
        graph.addEdge(7, 8);
        graph.addEdge(8, 11);
        graph.addEdge(8, 20);
        graph.addEdge(8, 17);
        graph.addEdge(8, 18);
        graph.addEdge(8, 12);
        graph.addEdge(9, 10);
        graph.addEdge(9, 13);
        graph.addEdge(9, 12);
        graph.addEdge(10, 13);
        graph.addEdge(11, 20);
        graph.addEdge(11, 14);
        graph.addEdge(12, 13);
        graph.addEdge(12, 19);
        graph.addEdge(12, 21);
        graph.addEdge(14, 15);
        graph.addEdge(15, 16);
        graph.addEdge(16, 17);
        graph.addEdge(17, 18);
        graph.addEdge(18, 21);
        graph.addEdge(19, 23);
        graph.addEdge(21, 22);
        graph.addEdge(22, 23);
        initialize(24, 37,16,4);
//        printBetweenness();
        addHost();

        buildWeightGraph();
        computeKRoutetoDc();
        computeKRouteToHost(25);
    }


}
