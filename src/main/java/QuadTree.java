/**
 * Created by Danderson on 4/9/16.
 */
import java.io.File;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Iterator;

public class QuadTree {


    public static void main(String[] args) {
        //QuadTree q = new QuadTree( -122.2998046875, 37.892195547244356, -122.2119140625, 37.82280243352756);
        //System.out.println(q.depth(1, 256));
        //System.out.println(q.depth(0.0000858406884765625, 256));
//        LinkedList<QuadTreeNode> helper = q.rastedImages(-122.2898046875, 37.852195547244356,
//                -122.2319140625, 37.83280243352756, q.depth(0.00004291534423828125, 256));
    }

    public PriorityQueue<QuadTreeNode> nodes;
    private QuadTreeNode root;

    /**
    * Generates a new quadtree senting the root and then populating the Quadtree with the image files
    * */
    public QuadTree(double l, double t, double r, double b) {
        nodes = new PriorityQueue<QuadTreeNode>();
        root = new QuadTreeNode(l, t, r, b, 0, 0, nodes);
        root.generateChildren(1);
        //System.out.println("Node Tree Completed");
    }

    /**This method will return the lowest subtree that will still completely encompass
     * the image we are trying to rasta.
     * FOR OPTIMIZATION ONLY!
     * */
    public QuadTreeNode subTree(double l, double t, double r, double b) {
        return root;
    }


    public PriorityQueue<Node> rastedImages(double l, double t, double r, double b, int depth) {
        Iterator itr = nodes.iterator();
        LinkedList<QuadTreeNode> temp = new LinkedList<QuadTreeNode>();
        while(itr.hasNext()) {
            QuadTreeNode qtr = (QuadTreeNode) itr.next();
            if (qtr.depth == depth) {
                //Case where it is directly in the middle
                if (qtr.top < t && qtr.bottom > b && qtr.left > l && qtr.right < r) {
                    temp.add(qtr);
                }
                else if (qtr.top > t && qtr.bottom < t && qtr.left > l && qtr.right < r) {
                    temp.add(qtr);
                }
                else if (qtr.top > b && qtr.bottom < b && qtr.left > l && qtr.right < r) {
                        temp.add(qtr);
                }
                else if (qtr.top < t && qtr.bottom > b && qtr.left < l && qtr.right > l) {
                    temp.add(qtr);
                }
                else if (qtr.top < t && qtr.bottom > b && qtr.left < r && qtr.right >= r) {
                    temp.add(qtr);
                }
                else if (qtr.top > t && qtr.bottom < t && qtr.left < l && qtr.right > l) {
                    temp.add(qtr);
                }
                else if (qtr.top > t && qtr.bottom < t && qtr.left < r && qtr.right >= r) {
                    temp.add(qtr);
                }
                else if (qtr.top > b && qtr.bottom < b && qtr.left < l && qtr.right > l) {
                    temp.add(qtr);
                }
                else if (qtr.top > b && qtr.bottom < b && qtr.left < r && qtr.right >= r) {
                    temp.add(qtr);
                }
            }
        }
        if(temp.isEmpty()){
            temp.add(root);
        }
        PriorityQueue<Node> pqHelper = new PriorityQueue<Node>();
        for(QuadTreeNode qtn : temp){
            pqHelper.add(new Node(qtn));
        }
        return pqHelper;
    }



    /**
     * This method will calculate the depth of images that you will require
     * so that your final image with have the correct optical zoom required.
     */
    public int depth(double widthPerPixelRequired, int tilePixelSize) {
        QuadTreeNode curr = root;
        double currentWidthPerPixel = Math.abs((curr.top - curr.bottom)) / tilePixelSize;
        int currDepth = 0;
        while(currentWidthPerPixel > widthPerPixelRequired) {
            currentWidthPerPixel = currentWidthPerPixel / 2;
            currDepth++;
        }
        if(currDepth > 7){
            return 7;
        } else {
            return currDepth;
        }
    }

}
