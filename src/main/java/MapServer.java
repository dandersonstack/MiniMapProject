import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/* Maven is used to pull in these dependencies. */
import com.google.gson.Gson;
import sun.awt.image.ImageWatched;

import javax.imageio.ImageIO;

import static spark.Spark.*;

/**
 * This MapServer class is the entry point for running the JavaSpark web server for the BearMaps
 * application project, receiving API calls, handling the API call processing, and generating
 * requested images and routes.
 * @author Alan Yao
 */
public class MapServer {
    /**
     * The root upper left/lower right longitudes and latitudes represent the bounding box of
     * the root tile, as the images in the img/ folder are scraped.
     * Longitude == x-axis; latitude == y-axis.
     */
    public static final double ROOT_ULLAT = 37.892195547244356, ROOT_ULLON = -122.2998046875,
            ROOT_LRLAT = 37.82280243352756, ROOT_LRLON = -122.2119140625;
    /** Each tile is 256x256 pixels. */
    public static final int TILE_SIZE = 256;
    /** HTTP failed response. */
    private static final int HALT_RESPONSE = 403;
    /** Route stroke information: typically roads are not more than 5px wide. */
    public static final float ROUTE_STROKE_WIDTH_PX = 5.0f;
    /** Route stroke information: Cyan with half transparency. */
    public static final Color ROUTE_STROKE_COLOR = new Color(108, 181, 230, 200);
    /** The tile images are in the IMG_ROOT folder. */
    private static final String IMG_ROOT = "img/";
    /**
     * The OSM XML file path. Downloaded from <a href="http://download.bbbike.org/osm/">here</a>
     * using custom region selection.
     **/
    private static final String OSM_DB_PATH = "berkeley.osm";
    /**
     * Each raster request to the server will have the following parameters
     * as keys in the params map accessible by,
     * i.e., params.get("ullat") inside getMapRaster(). <br>
     * ullat -> upper left corner latitude,<br> ullon -> upper left corner longitude, <br>
     * lrlat -> lower right corner latitude,<br> lrlon -> lower right corner longitude <br>
     * w -> user viewport window width in pixels,<br> h -> user viewport height in pixels.
     **/
    private static final String[] REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat",
        "lrlon", "w", "h"};
    /**
     * Each route request to the server will have the following parameters
     * as keys in the params map.<br>
     * start_lat -> start point latitude,<br> start_lon -> start point longitude,<br>
     * end_lat -> end point latitude, <br>end_lon -> end point longitude.
     **/
    private static final String[] REQUIRED_ROUTE_REQUEST_PARAMS = {"start_lat", "start_lon",
        "end_lat", "end_lon"};
    /* Define any static variables here. Do not define any instance variables of MapServer. */
    private static GraphDB g1;

    /**
     * Place any initialization statements that will be run before the server main loop here.
     * Do not place it in the main function. Do not place initialization code anywhere else.
     * This is for testing purposes, and you may fail tests otherwise.
     **/
    public static void initialize() {
        g1 = new GraphDB(OSM_DB_PATH);
        LinkedList<Long> currRoute = new LinkedList<Long>();
        System.out.println("For testing purposese: in initialize");
    }
    private static LinkedList<Long> currentRoute;

    public static void main(String[] args) {
        initialize();
        staticFileLocation("/page");
        /* Allow for all origin requests (since this is not an authenticated server, we do not
         * care about CSRF).  */
        before((request, response) -> {
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Request-Method", "*");
            response.header("Access-Control-Allow-Headers", "*");
        });


        /* Define the raster endpoint for HTTP GET requests. I use anonymous functions to define
         * the request handlers. */
        get("/raster", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_RASTER_REQUEST_PARAMS);
            /* The png image is written to the ByteArrayOutputStream */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            /* getMapRaster() does almost all the work for this API call */
            Map<String, Object> rasteredImgParams = getMapRaster(params, os);
            /* On an image query success, add the image data to the response */
            if (rasteredImgParams.containsKey("query_success")
                    && (Boolean) rasteredImgParams.get("query_success")) {
                String encodedImage = Base64.getEncoder().encodeToString(os.toByteArray());
                rasteredImgParams.put("b64_encoded_image_data", encodedImage);
            }
            /* Encode response to Json */
            Gson gson = new Gson();
            return gson.toJson(rasteredImgParams);
        });

        /* Define the routing endpoint for HTTP GET requests. */
        get("/route", (req, res) -> {
            HashMap<String, Double> params =
                    getRequestParams(req, REQUIRED_ROUTE_REQUEST_PARAMS);
            LinkedList<Long> route = findAndSetRoute(params);
            currentRoute = route;
            return !route.isEmpty();
        });

        /* Define the API endpoint for clearing the current route. */
        get("/clear_route", (req, res) -> {
            clearRoute();
            return true;
        });

        /* Define the API endpoint for search */
        get("/search", (req, res) -> {
            Set<String> reqParams = req.queryParams();
            String term = req.queryParams("term");
            Gson gson = new Gson();
            /* Search for actual location data. */
            if (reqParams.contains("full")) {
                List<Map<String, Object>> data = getLocations(term);
                return gson.toJson(data);
            } else {
                /* Search for prefix matching strings. */
                List<String> matches = getLocationsByPrefix(term);
                return gson.toJson(matches);
            }
        });

        /* Define map application redirect */
        get("/", (request, response) -> {
            response.redirect("/map.html", 301);
            return true;
        });
    }

    /**
     * Validate & return a parameter map of the required request parameters.
     * Requires that all input parameters are doubles.
     * @param req HTTP Request
     * @param requiredParams TestParams to validate
     * @return A populated map of input parameter to it's numerical value.
     */
    private static HashMap<String, Double> getRequestParams(
            spark.Request req, String[] requiredParams) {
        Set<String> reqParams = req.queryParams();
        HashMap<String, Double> params = new HashMap<>();
        for (String param : requiredParams) {
            if (!reqParams.contains(param)) {
                halt(HALT_RESPONSE, "Request failed - parameters missing.");
            } else {
                try {
                    params.put(param, Double.parseDouble(req.queryParams(param)));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    halt(HALT_RESPONSE, "Incorrect parameters - provide numbers.");
                }
            }
        }
        return params;
    }

    /**
     * Handles raster API calls, queries for tiles and rasters the full image. <br>
     * <p>
     *     The rastered photo must have the following properties:
     *     <ul>
     *         <li>Has dimensions of at least w by h, where w and h are the user viewport width
     *         and height.</li>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *         <li>If a current route exists, lines of width ROUTE_STROKE_WIDTH_PX and of color
     *         ROUTE_STROKE_COLOR are drawn between all nodes on the route in the rastered photo.
     *         </li>
     *     </ul>
     *     Additional image about the raster is returned and is to be included in the Json response.
     * </p>
     * @param params Map of the HTTP GET request's query parameters - the query bounding box and
     *               the user viewport width and height.
     * @param os     An OutputStream that the resulting png image should be written to.
     * @return A map of parameters for the Json response as specified:
     * "raster_ul_lon" -> Double, the bounding upper left longitude of the rastered image <br>
     * "raster_ul_lat" -> Double, the bounding upper left latitude of the rastered image <br>
     * "raster_lr_lon" -> Double, the bounding lower right longitude of the rastered image <br>
     * "raster_lr_lat" -> Double, the bounding lower right latitude of the rastered image <br>
     * "raster_width"  -> Double, the width of the rastered image <br>
     * "raster_height" -> Double, the height of the rastered image <br>
     * "depth"         -> Double, the 1-indexed quadtree depth of the nodes of the rastered image.
     * Can also be interpreted as the length of the numbers in the image string. <br>
     * "query_success" -> Boolean, whether an image was successfully rastered. <br>
     * @see #REQUIRED_RASTER_REQUEST_PARAMS
     * REQUIRED_RASTER_REQUEST_PARAMS = {"ullat", "ullon", "lrlat", "lrlon", "w", "h"};
     */
    public static Map<String, Object> getMapRaster(Map<String, Double> params, OutputStream os) {
        HashMap<String, Object> rasteredImageParams = new HashMap<>();
        double startTop = params.get("ullat");
        double startLeft = params.get("ullon");
        double startBottom = params.get("lrlat");
        double startRight = params.get("lrlon");
        QuadTree quadTree = new QuadTree(ROOT_ULLON, ROOT_ULLAT, ROOT_LRLON, ROOT_LRLAT);
        //smaller than left, greater than right
        //smaller than top, greater than bottom
        double QueryDPP = Math.abs((params.get("lrlon")-params.get("ullon")))/params.get("w");
        double heightPerPixelNeeded = Math.abs(startTop - startBottom)/ params.get("h");
        //int depth = quadTree.depth(widthPerPixelNeeded, TILE_SIZE);

        int depth = 0;
        double TileDPP = Math.abs((ROOT_LRLON - ROOT_ULLON)) / (Math.pow(2.0,depth) * TILE_SIZE);

        while (TileDPP > QueryDPP) {
            if(depth == 7) {
                break;
            }
            depth++;
            TileDPP = (ROOT_LRLON - ROOT_ULLON) / ((Math.pow(2.0,depth) * TILE_SIZE));
        }

        PriorityQueue<Node> splitImages = quadTree.rastedImages(startLeft, startTop, startRight, startBottom, depth);
        //System.out.println(splitImages.size());

        double Y1 = splitImages.peek().qtn.top;
        double Y2 = splitImages.peek().qtn.bottom;
        double X1 = splitImages.peek().qtn.left;
        double X2 = splitImages.peek().qtn.right;
        for(Node qtn1: splitImages) {
            X1 = Math.min(X1, qtn1.qtn.left);
            Y1 = Math.max(Y1, qtn1.qtn.top);
            X2 = Math.max(X2, qtn1.qtn.right);
            Y2 = Math.min(Y2, qtn1.qtn.bottom);
        }
        double tH = Math.abs((ROOT_LRLAT - ROOT_ULLAT)) / (Math.pow(2,depth));
        double tW = Math.abs(ROOT_LRLON - ROOT_ULLON) / Math.pow(2,depth);
        int width = ((int)  Math.ceil((Math.abs(X1 - X2) / tW)));
        int height =splitImages.size() / width;
        //int height = (int) Math.ceil(Math.abs(Y1 - Y2) / tH);

        //int height = (int) (Math.abs(Y1 - Y2) / tH);
        if(height == 0){
            height = 1;
        }

        //int width = ((int) (Math.abs(X1 - X2) / tW));

        //System.out.print(depth);
        try {
            int x = 0; int y = 0;
            BufferedImage result = new BufferedImage(width * TILE_SIZE,  height * TILE_SIZE, BufferedImage.TYPE_INT_RGB );
            Graphics g = result.getGraphics();
            while (!splitImages.isEmpty()) {
                Node n = splitImages.poll();
                //System.out.println(n.qtn.fileImage);
                BufferedImage bi = ImageIO.read(n.qtn.theData);
                g.drawImage(bi, x, y, null);
                x += 256;
                if (x > result.getWidth() - 1) {
                    //System.out.println("New line");
                    x = 0;
                    y += bi.getHeight();
                }
            }
            ((Graphics2D) g).setStroke(new BasicStroke(MapServer.ROUTE_STROKE_WIDTH_PX, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            ((Graphics2D) g).setColor(MapServer.ROUTE_STROKE_COLOR);
            //X1 = "raster_ul_lon", Y1 = "raster_ul_lat"
            if(currentRoute != null) {
                if (!(currentRoute.size() < 2)) {
                    Long l = currentRoute.getFirst();
                    for (int i = 1; i < currentRoute.size(); i++) {
                        GraphNode start = g1.hashMap.get(l);
                        GraphNode end = g1.hashMap.get(currentRoute.get(i));
//                        if ((start.lon > X1 && start.lon < X2 && start.lat < Y1 && start.lat > Y2) ||
//                            (end.lon > X1 && end.lon < X2 && end.lat < Y1 && end.lat > Y2)) {
                                int left = (int) ((start.lon - X1) / QueryDPP);
                                int top = (int) ((Y1 - start.lat ) / heightPerPixelNeeded);
                                int right = (int) ((end.lon - X1) / QueryDPP);
                                int bottom = (int) ((Y1 - end.lat) / heightPerPixelNeeded);
                                g.drawLine(left, top, right, bottom);
                        //}
                        l = end.id;

                    }
                }
            }

            ImageIO.write(result,"png",os);
            //System.out.println(result);
        } catch (IOException e) {
            System.out.println("File wasn't found, must have fucked up");
        }
//        System.out.println("{"+"depth:" + depth + ",raster_ul_lon:"+ X1 + ",raster_ul_lat:"
//                + Y1 + ",raster_lr_lon:" + X2+",raster_lr_lat:" + Y2 + ",raster_width:"
//                + TILE_SIZE * width + ",raster_height:" + TILE_SIZE * height +"}");
        rasteredImageParams.put("depth", depth);
        rasteredImageParams.put("raster_ul_lon", X1);
        rasteredImageParams.put("raster_ul_lat", Y1);
        rasteredImageParams.put("raster_lr_lon", X2);
        rasteredImageParams.put("raster_lr_lat", Y2);
        rasteredImageParams.put("raster_width", TILE_SIZE * width);
        rasteredImageParams.put("raster_height", TILE_SIZE * height);
        rasteredImageParams.put("query_success", true);

        return rasteredImageParams;
    }

    /**
     * Searches for the shortest route satisfying the input request parameters, sets it to be the
     * current route, and returns a <code>LinkedList</code> of the route's node ids for testing
     * purposes. <br>
     * The route should start from the closest node to the start point and end at the closest node
     * to the endpoint. Distance is defined as the euclidean between two points (lon1, lat1) and
     * (lon2, lat2).
     * @param params from the API call described in REQUIRED_ROUTE_REQUEST_PARAMS
     * @return A LinkedList of node ids from the start of the route to the end.
     */
    public static LinkedList<Long> findAndSetRoute(Map<String, Double> params) {
        LinkedList<Long> ll = g1.shortestPath(params.get("start_lat"),
                params.get("start_lon"), params.get("end_lat"), params.get("end_lon"));
        return ll;
    }

    /**
     * Clear the current found route, if it exists.
     */
    public static void clearRoute() {
        currentRoute = null;
    }

    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public static List<String> getLocationsByPrefix(String prefix) {
        return new LinkedList<>();
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" -> Number, The latitude of the node. <br>
     * "lon" -> Number, The longitude of the node. <br>
     * "name" -> String, The actual name of the node. <br>
     * "id" -> Number, The id of the node. <br>
     */
    public static List<Map<String, Object>> getLocations(String locationName) {
        return new LinkedList<>();
    }
}
