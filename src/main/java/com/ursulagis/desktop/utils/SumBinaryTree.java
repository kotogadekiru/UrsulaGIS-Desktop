package com.ursulagis.desktop.utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Vector;

import org.locationtech.jts.geom.Geometry;

public class SumBinaryTree {
	// Represents a node of an n-ary tree
	static class Node 
	{
	    Geometry key;
	    Vector<Node> child;
	};
	 
	// Utility function to create a new tree node
	static Node newNode(Geometry key)
	{
	    Node temp = new Node();
	    temp.key = key;
	    temp.child = new Vector<>();
	    return temp;
	}
	
    public Geometry sumOfLeaves(Node root) {
        if (root == null) {
            return null;
        }
        if (root.child == null) {
            return root.key; // Leaf node
        }
        Geometry ret = null;
        for(Node c : root.child) {
        	if(ret==null) {
        		ret=sumOfLeaves(c);
        	} else { 
        		ret=ret.union(sumOfLeaves(c));
        	}
        }
        return ret;
    }
	 
	// Function to compute the sum
	// of all elements in generic tree
	static Geometry sumNodes(Node root)
	{
	    // initialize the sum variable
		Geometry sum = null;
	 
	    if (root == null)
	        return null;
	 
	    // Creating a queue and pushing the root
	    Queue<Node> q = new LinkedList<>();
	    q.add(root);
	 
	    while (!q.isEmpty()) {
	        int n = q.size();
	 
	        // If this node has children
	        while (n > 0)   {
	 
	            // Dequeue an item from queue and
	            // add it to variable "sum"
	            Node p = q.peek();
	            q.remove();
	            if(sum==null) {
	            	sum=p.key;
	            } else {
	            	sum=sum.union(p.key);
	            }
	            //sum += p.key;
	 
	            // Enqueue all children of the dequeued item
	            for (int i = 0; i < p.child.size(); i++)
	                q.add(p.child.get(i));
	            n--;
	        }
	    }
	    return sum;
	}
	 
	// Driver program
	public static void main(String[] args)	{
//	    // Creating a generic tree
//	    Node root = newNode(20);
//	    (root.child).add(newNode(2));
//	    (root.child).add(newNode(34));
//	    (root.child).add(newNode(50));
//	    (root.child).add(newNode(60));
//	    (root.child).add(newNode(70));
//	    (root.child.get(0).child).add(newNode(15));
//	    (root.child.get(0).child).add(newNode(20));
//	    (root.child.get(1).child).add(newNode(30));
//	    (root.child.get(2).child).add(newNode(40));
//	    (root.child.get(2).child).add(newNode(100));
//	    (root.child.get(2).child).add(newNode(20));
//	    (root.child.get(0).child.get(1).child).add(newNode(25));
//	    (root.child.get(0).child.get(1).child).add(newNode(50));
//	 
//	    System.out.print(sumNodes(root) +"\n");
	}
}

//This code is contributed by Arnab Kundu