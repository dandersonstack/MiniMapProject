# MiniMapProject
Quad Tree implementation with routing, uploaded for Barracuda to get a sneak peak.

By Daniel Anderson

Files that I inherited:
*MapDBHandler (skeleton)
*MapServer (skeleton)
*TestParams (skeleton)

Files Implemented from Scratch:
*Connector
*GraphDB
*Node
*GraphNode
*QuadTree
*QuadTreeNode


#Steps to get it working with Intelij:

##This project uses Apache Maven as its build system; therefore it is highly recommended to use Intellij
##go to new >import project from Existing Sources then...

1. Import project from existing model (and select Maven)
2. Select 'Import Maven projects autimatically'
3. Set 'src/main/java' as sources root, 'src/static' directory as sources root,
and 'src/test/java' directory as test sources root
4. Do not add the course javalib to your IntelliJ library, it will cause conflicts.

5. Build the project by running MapServer.java, and navigate to localhost:4567


#Steps to get it working with command line osx:
1. brew install maven
2. mvn compile
3. mvn exec:java -Dexec.mainClass="MapServer"

To run the tests:
1.mvn test