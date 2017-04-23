/**
 * Created by Danderson on 4/16/16.
 */
public class Node implements Comparable {
    public QuadTreeNode qtn;

    public Node(QuadTreeNode qtn1) {
        qtn = qtn1;
    }

    @Override
    public int compareTo(Object o) {
        if (this.qtn.top < ((Node) o).qtn.top) {
            return 1;
        } else if (this.qtn.top > ((Node) o).qtn.top) {
            return -1;
        } else {
            if(this.qtn.left > ((Node) o).qtn.left){
                return 1;
            }else if(this.qtn.left < ((Node) o).qtn.left){
                return -1;
            }else {
                return 0;
            }
        }
    }
}
