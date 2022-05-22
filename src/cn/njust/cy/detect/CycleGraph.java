package cn.njust.cy.detect;

import java.io.*;
import java.util.*;
import java.util.LinkedList;
  
// This class represents a directed graph using adjacency list
// representation
public class CycleGraph{
    private int V;   // No. of vertices
    private LinkedList<Integer> adj[]; //Adjacency List
    private static ArrayList<Integer> ans = new ArrayList<Integer>();
	private static ArrayList<ArrayList<Integer>> ansList = new ArrayList<ArrayList<Integer>>();

    public CycleGraph(int v){
        V = v;
        adj = new LinkedList[v];
        for (int i=0; i<v; ++i)
            adj[i] = new LinkedList();
    }

    void addEdge(int v, int w)  { adj[v].add(w); }

    void DFSUtil(int v,boolean visited[]){
        visited[v] = true;
//        System.out.print(v + " ");
        ans.add(new Integer(v));
  
        int n;

        Iterator<Integer> i =adj[v].iterator();
        while (i.hasNext()){
            n = i.next();
            if (!visited[n])
                DFSUtil(n,visited);
        }
    }

    CycleGraph getTranspose()
    {
        CycleGraph g = new CycleGraph(V);
        for (int v = 0; v < V; v++)
        {
            // Recur for all the vertices adjacent to this vertex
            Iterator<Integer> i =adj[v].listIterator();
            while(i.hasNext())
                g.adj[i.next()].add(v);
        }
        return g;
    }
  
    void fillOrder(int v, boolean visited[], Stack stack)
    {
        // Mark the current node as visited and print it
        visited[v] = true;
  
        // Recur for all the vertices adjacent to this vertex
        Iterator<Integer> i = adj[v].iterator();
        while (i.hasNext())
        {
            int n = i.next();
            if(!visited[n])
                fillOrder(n, visited, stack);
        }
  
        // All vertices reachable from v are processed by now,
        // push v to Stack
        stack.push(new Integer(v));
    }
  
    // The main function that finds and prints all strongly
    // connected components
    void printSCCs()
    {
        Stack stack = new Stack();
  
        // Mark all the vertices as not visited (For first DFS)
        boolean visited[] = new boolean[V];
        for(int i = 0; i < V; i++)
            visited[i] = false;
  
        // Fill vertices in stack according to their finishing
        // times
        for (int i = 0; i < V; i++)
            if (visited[i] == false)
                fillOrder(i, visited, stack);
  
        // Create a reversed graph
        CycleGraph gr = getTranspose();
  
        // Mark all the vertices as not visited (For second DFS)
        for (int i = 0; i < V; i++)
            visited[i] = false;
  
        // Now process all vertices in order defined by Stack
        while (stack.empty() == false)
        {
            // Pop a vertex from stack
            int v = (int)stack.pop();
  
            // Print Strongly connected component of the popped vertex
            if (visited[v] == false)
            {
                gr.DFSUtil(v, visited);
//                System.out.println();
                if(ans!=null) ansList.add(ans);
				ans = new ArrayList<Integer>();
            }
        }
    }
  
    public ArrayList<ArrayList<Integer>> getList(){
    	return ansList;
    }
    
    // Driver method
    public static void main(String args[])
    {
        // Create a graph given in the above diagram
        CycleGraph g = new CycleGraph(5);
        g.addEdge(1, 0);
        g.addEdge(0, 2);
        g.addEdge(2, 1);
        g.addEdge(0, 3);
        g.addEdge(3, 4);
        g.printSCCs();
        System.out.println("Following are strongly connected components "+
                           "in given graph ");
        
        for(ArrayList<Integer> l:ansList) {
        	for(Integer i:l) {
        		System.out.print(i+" ");
        	}
        	System.out.println();
        }
    }
}