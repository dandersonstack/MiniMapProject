# MiniMapProject
Quad Tree implementation with routing, uploaded for Barracuda to get a sneak peak.

By Daniel Anderson

### Files that I inherited:
*MapDBHandler (skeleton)

*MapServer (skeleton)

*TestParams (skeleton)

### Files Implemented from Scratch:
*Connector

*GraphDB

*Node

*GraphNode

*QuadTree

*QuadTreeNode

## Steps to get it working with command line osx:
1. brew install maven
2. mvn compile
3. mvn exec:java -Dexec.mainClass="MapServer"

## Steps to get it working with Intelij:
1. Import project from existing model (and select Maven)
2. Select 'Import Maven projects autimatically'
3. Set 'src/main/java' as sources root, 'src/static' directory as sources root,
and 'src/test/java' directory as test sources root
4. Do not add the course javalib to your IntelliJ library, it will cause conflicts.
5. Build the project by running MapServer.java, and navigate to localhost:4567

## To run the tests:
1.mvn test

## TODO for Heroku Deployment:
Overview:
Build the project, then assembly:assembly creates the jar, and heroku:deploy creates a slug from that jar and pushes it to the heroku repo, and then the webapp can be found at your heroku project's url. if you're missing the maven targets something is up with your project setup

Tasks left:
- deleted the target folder and built a new project
- modify a few methods to get my png and berkeley.osm files as resource streams