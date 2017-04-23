import com.sun.corba.se.impl.orbutil.graph.Graph;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by Danderson on 4/18/16.
 */
public class GraphNode {

    protected long id;
    protected double lat;
    protected double lon;
    protected String name;
    HashSet<GraphNode> edges;
    public GraphNode(String ID, String Lat, String Lon) {
        this.id = Long.valueOf(ID);
        this.lat = Double.valueOf(Lat);
        this.lon = Double.valueOf(Lon);
        edges = new HashSet<GraphNode>();
    }
    @Override
    public int hashCode(){
        return (int)id;
    }
    @Override
    public boolean equals(Object o){
        if(((GraphNode)o).id == this.id) {
            return true;
        } else {
            return false;
        }
    }

    public void connect(GraphNode gnAdjacent) {
        edges.add(gnAdjacent);
        gnAdjacent.edges.add(this);
    }

    public void setName(String Name) {
        this.name = Name;
    }


    public void addEdge(GraphNode gn){
        edges.add(gn);
    }

}
