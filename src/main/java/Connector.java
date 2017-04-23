import java.util.HashSet;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Stack;


/**
 * Created by Danderson on 4/19/16.
 */
public class Connector {

    public class Wrapper implements Comparable<Wrapper> {
        protected GraphNode graphNode;
        protected double distanceFromStart;
        private Stack<GraphNode> previousNodes;
        public Wrapper(GraphNode gN, Stack<GraphNode> prev, double dist) {
            distanceFromStart = dist;
            previousNodes = prev;
            graphNode = gN;
        }

        public double getPriority() {
            return distanceFromStart + Connector.euclidHeuristic(graphNode, finish);
        }

        @Override
        public int compareTo(Wrapper w1) {
            if (this.getPriority() < w1.getPriority()) {
                return -1;
            } else if (this.getPriority() > w1.getPriority()) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    public static Double euclidHeuristic(GraphNode g1, GraphNode g2) {
        double latDist = Math.abs(g1.lat - g2.lat);
        double lonDist = Math.abs(g1.lon - g2.lon);
        return Math.sqrt(latDist * latDist + lonDist * lonDist);
    }


    protected HashSet<GraphNode> visited;
    protected HashMap<GraphNode, Double> dist;
    protected HashMap<GraphNode, GraphNode> prev;
    protected PriorityQueue<Wrapper> fringe;
    protected GraphNode start;
    protected GraphNode finish;
    HashMap<Long,GraphNode> hashMap;

    public Connector(GraphNode s, GraphNode f, HashMap<Long,GraphNode> hM){
        start = s;
        finish = f;
        hashMap = hM;

    }

    /**
     * visited <- HashSet of nodes, used to track nodes we have already visited
     dist <- HashMap of node to path distance to that node
     prev <- HashMap of previous pointers
     fringe <- PriorityQueue comparing on dist+heuristic
     fringe.add(start)
     dist(start) = 0
     while fringe is not empty:
     v <- fringe.dequeue()
     if visited(v): continue // Notice that this is immediately after dequeue
     visited <- v      // Add v to visited set
     if v == t: break // Found destination vertex and have valid prev pointers
     for child c of v: // Do not check if your children have been visited
     if (c not in dist) or (dist(c) > dist(v) + edge(v, c)): // Important
     dist(c) = dist(v) + edge(v, c) // Update distance
     fringe.add(c)                  // Update pqueue
     prev(c) = v                    // Update back-pointers*/

    public LinkedList<Long> aStar() {
        Wrapper initialWrapper = new Wrapper(start, new Stack<GraphNode>(),
                Connector.euclidHeuristic(start, finish));
        PriorityQueue<Wrapper> minPQ = new PriorityQueue<Wrapper>();
        Wrapper currentWrapper;

        HashMap<GraphNode,Wrapper> hashMap = new HashMap<GraphNode, Wrapper>();
        Stack<GraphNode> prevMoves = initialWrapper.previousNodes;
        minPQ.add(initialWrapper);
        hashMap.put(start, initialWrapper);

        while(!minPQ.isEmpty()) {
            currentWrapper = minPQ.poll();
            if(currentWrapper.graphNode.equals(finish)){
                prevMoves = currentWrapper.previousNodes;
                prevMoves.push(currentWrapper.graphNode);
                break;
            } else {
                for(GraphNode gn: currentWrapper.graphNode.edges) {
                    double currDistance = currentWrapper.distanceFromStart +
                            Connector.euclidHeuristic(currentWrapper.graphNode, gn);
                    if(!hashMap.containsKey(gn)) {
                        prevMoves = (Stack<GraphNode>) currentWrapper.previousNodes.clone();
                        prevMoves.push(currentWrapper.graphNode);
                        Wrapper wrapperX = new Wrapper(gn, prevMoves, currDistance);
                        hashMap.put(gn, wrapperX);
                        minPQ.add(wrapperX);
                    } else {
                        if (hashMap.get(gn).distanceFromStart > currDistance) {
                            Wrapper temp = hashMap.get(gn);
                            prevMoves = (Stack<GraphNode>) currentWrapper.previousNodes.clone();
                            prevMoves.push(currentWrapper.graphNode);
                            temp.previousNodes = prevMoves;
                            temp.distanceFromStart = currDistance;
                        }
                    }
                }
            }
        }

        LinkedList<Long> path = new LinkedList<Long>();
        for (GraphNode gn : prevMoves) {
            path.add(Long.valueOf(gn.id));
        }
        return path;
    }


}
