package edu.boun.edgecloudsim.network.topoGraph;

import java.util.Arrays;

public class GBN extends topoGraph{

    public void addHost(){
        Arrays.asList(17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32).forEach(graph::addVertex);
        this.dataCenters = new int[]{1,10,13,15};
        this.host = new int[]{17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32};
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

    public int[] getHotNode(){return new int[]{4,5,7};}

    public GBN() {
        instance = this;
        Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16).forEach(graph::addVertex);
        this.node = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        graph.addEdge(0, 2);
        graph.addEdge(0, 8);
        graph.addEdge(1, 2);
        graph.addEdge(1, 3);
        graph.addEdge(1, 4);
        graph.addEdge(2, 4);
        graph.addEdge(3, 4);
        graph.addEdge(3, 9);
        graph.addEdge(4, 8);
        graph.addEdge(4, 10);
        graph.addEdge(4, 9);
        graph.addEdge(5, 6);
        graph.addEdge(5, 8);
        graph.addEdge(6, 7);
        graph.addEdge(7, 8);
        graph.addEdge(7, 10);
        graph.addEdge(9, 10);
        graph.addEdge(9, 12);
        graph.addEdge(10, 11);
        graph.addEdge(10, 12);
        graph.addEdge(11, 13);
        graph.addEdge(12, 14);
        graph.addEdge(12, 16);
        graph.addEdge(13, 14);
        graph.addEdge(14, 15);
        graph.addEdge(15, 16);

        initialize(17, 26,16,4);
//        printBetweenness();
        addHost();

        buildWeightGraph();
        computeKRoutetoDc();
        computeKRouteToHost(25);
    }
}
