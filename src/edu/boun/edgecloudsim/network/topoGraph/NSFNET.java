package edu.boun.edgecloudsim.network.topoGraph;

import java.util.Arrays;

public class NSFNET extends topoGraph{

    public void addHost(){
        Arrays.asList(14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29).forEach(graph::addVertex);
//        this.dataCenters = new int[]{3, 5, 7, 10};
        this.dataCenters = new int[]{5, 6, 10, 13};
        this.host = new int[]{14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29};
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
//    public int[] getHotNode(){return new int[]{6,9,11,13};}
    public int[] getHotNode(){return new int[]{0,5};}

    public NSFNET() {
        instance = this;

        Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13).forEach(graph::addVertex);
        this.node = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        // 添加边
        graph.addEdge(0, 1);
        graph.addEdge(0, 2);
        graph.addEdge(0, 3);
        graph.addEdge(1, 2);
        graph.addEdge(1, 7);
        graph.addEdge(2, 5);
        graph.addEdge(3, 8);
        graph.addEdge(3, 4);
        graph.addEdge(4, 5);
        graph.addEdge(4, 6);
        graph.addEdge(5, 12);
        graph.addEdge(5, 13);
        graph.addEdge(6, 7);
        graph.addEdge(7, 10);
        graph.addEdge(8, 9);
        graph.addEdge(8, 11);
        graph.addEdge(9, 10);
        graph.addEdge(9, 12);
        graph.addEdge(10, 11);
        graph.addEdge(10, 13);
        graph.addEdge(11, 12);

        initialize(14, 21,16,4);
//        printBetweenness();
        addHost();

        buildWeightGraph();
        computeKRoutetoDc();
        computeKRouteToHost(25);

    }
}
