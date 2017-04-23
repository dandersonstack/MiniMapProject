/**
 * Created by Danderson on 4/9/16.
 */
import java.io.File;
import java.util.ArrayList;
import java.util.PriorityQueue;

public class QuadTreeNode implements Comparable{

    private final static int MaxDepth = 7;
    private PriorityQueue<QuadTreeNode> owner;


    public long fileImage;
    protected double left;
    protected double top;
    protected double right;
    protected double bottom;
    public File theData;

    protected int depth;

    private QuadTreeNode Q1;
    private QuadTreeNode Q2;
    private QuadTreeNode Q3;
    private QuadTreeNode Q4;

    /**
     * "each square node will be called a tile interchangeably,
     * and is defined by its upper left and lower right points"
     * (or you could create a Point class like Alan did to be a little neater,
     * might be more efficient...), and for me, the last one is a File object,
     * which refers to the individual image files
     */
    public QuadTreeNode(double l, double t, double r, double b, long image, int d, PriorityQueue<QuadTreeNode> q) {
        top = t;
        bottom = b;
        right = r;
        left = l;
        depth = d;
        owner = q;
        fileImage = image;
        if(image == 0) {
            theData = new File("img/root.png");
        }
        else {
            theData = new File("img/" + fileImage + ".png");
        }
        owner.add(this);

    }



    /**Check to see if the file for the children exists or if it is a leaf node
     * */
    public void generateChildren (int depth) {
        if (depth <= 7) {
            double height = Math.abs(top - bottom);
            double width = Math.abs(right - left);

            Q1 = new QuadTreeNode(left, top, (right - (width / 2)),
                    (bottom + (height / 2)) , ((fileImage * 10) + 1), depth, owner);
            Q1.generateChildren(depth + 1);

            Q2 = new QuadTreeNode((left + (width / 2)), top, right, (bottom + (height / 2))
                    , ((fileImage * 10) + 2), depth, owner);
            Q2.generateChildren(depth + 1);

            Q3 = new QuadTreeNode( left, (top - (height / 2)), (right - (width / 2)), bottom,
                    ((fileImage * 10) + 3), depth, owner);
            Q3.generateChildren(depth + 1);

            Q4 = new QuadTreeNode((left + (width / 2)), (top - (height / 2)), right
                    , bottom, ((fileImage * 10) + 4), depth, owner);
            Q4.generateChildren(depth + 1);
        }
    }

    @Override
    public int compareTo(Object o){
        if(this.fileImage > ((QuadTreeNode)o).fileImage){
            return 1;
        } else if (this.fileImage < ((QuadTreeNode)o).fileImage){
            return -1;
        } else {
            return 0;
        }
    }

}
