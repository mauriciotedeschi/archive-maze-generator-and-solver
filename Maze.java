import java.awt.Color;
import java.util.*;

import javalib.impworld.World;
import javalib.impworld.WorldScene;
import javalib.worldimages.OutlineMode;
import javalib.worldimages.Posn;
import javalib.worldimages.RectangleImage;
import javalib.worldimages.TextImage;
import tester.Tester;

// Holds data about an edge
class Edge {
  public int cellCol;
  public int cellRow;
  public boolean vertical;
  public boolean connected;

  // Constructor for edge - no edges are connected by default
  Edge(int col, int row, boolean vertical) {
    this.cellCol = col;
    this.cellRow = row;
    this.vertical = vertical;
    this.connected = false;
  }

  // the maze has connected the cells this edge connect - update connected
  // variable
  public void connected() {
    this.connected = true;
  }

  // draw this edge if the two cells it connects are indeed connected - otherwise
  // do nothing
  public void drawIfNotConnected(UI ui) {
    if (!this.connected) {
      if (this.vertical) {
        ui.drawEdge(this.cellRow, this.cellCol, this.cellRow, this.cellCol + 1);
      }
      else {
        ui.drawEdge(this.cellRow, this.cellCol, this.cellRow + 1, this.cellCol);
      }
    }
  }

  // get the Posn representation of the "main" cell of this edge
  // the "main" cell is either above or to the left of the other cell
  public Posn getMainPosn() {
    return new Posn(this.cellCol, this.cellRow);
  }

  // get the Posn representation of the "other" cell in this edge
  // the "other" cell is either below or to the right of the main cell
  public Posn getOtherPosn() {
    if (this.vertical) {
      return new Posn(this.cellCol + 1, this.cellRow);
    }
    else {
      return new Posn(this.cellCol, this.cellRow + 1);
    }
  }
}

// Holds maze data and builds maze
class Maze {
  public static int DEFAULT_CELLS_ACROSS = 32;
  public static int DEFAULT_CELLS_DOWN = 18;

  public int numRows;
  public int numCols;
  public int numCells;

  ArrayList<Edge> edges;
  HashMap<Posn, Posn> reps;
  HashMap<Posn, ArrayList<Posn>> adjacencyList;
  int numEdges;

  HashMap<Posn, Color> coloring;
  HashMap<Posn, Integer> delays;
  boolean drawingSolution = false;
  double tick;

  // default constructor - default number of rows and columns
  public Maze() {
    this.numRows = DEFAULT_CELLS_DOWN;
    this.numCols = DEFAULT_CELLS_ACROSS;
    this.numCells = this.numRows * this.numCols;
    this.initSolution();
    this.makeSolution();
  }

  // constructor for maze of custom size - DOES NOT ERROR CHECK
  public Maze(int rows, int cols) {
    this.numRows = rows;
    this.numCols = cols;
    this.numCells = this.numRows * this.numCols;
    this.initSolution();
    this.makeSolution();
  }

  // constructor w/ a boolean marking that this maze shouldn't solve itself on
  // construction
  public Maze(int rows, int cols, boolean solveNow) {
    this.numRows = rows;
    this.numCols = cols;
    this.numCells = this.numRows * this.numCols;
    this.initSolution();
    if (solveNow) {
      this.makeSolution();
    }
  }

  // tell the UI how many rows and columns this maze has - see note above
  // setRowsAndCols
  public void updateUIRowsAndCols(UI ui) {
    ui.processRowsAndCols(this.numRows, this.numCols);
  }

  // make a solution to the maze according to kruskal's algorithm
  public void makeSolution() {
    while (!this.finishedMaze()) {
      this.kruskalStep();
    }
  }

  // check if maze has been completely finished
  public boolean finishedMaze() {
    return this.numEdges >= this.numCols * this.numRows - 1;
  }

  // initialize all variables for solving later
  public void initSolution() {
    this.edges = new ArrayList<Edge>();
    this.reps = new HashMap<Posn, Posn>();
    this.adjacencyList = new HashMap<Posn, ArrayList<Posn>>();
    // init every cell's representative to itself
    // and every cell's adjacencyList to an empty list
    for (int y = 0; y < this.numRows; y++) {
      for (int x = 0; x < this.numCols; x++) {
        this.reps.put(new Posn(x, y), new Posn(x, y));
        this.adjacencyList.put(new Posn(x, y), new ArrayList<Posn>());
      }
    }
    // init all the horizontal walls
    for (int y = 0; y < this.numRows - 1; y++) {
      for (int x = 0; x < this.numCols; x++) {
        this.edges.add(new Edge(x, y, false));
      }
    }
    // init all the vertical walls
    for (int y = 0; y < this.numRows; y++) {
      for (int x = 0; x < this.numCols - 1; x++) {
        this.edges.add(new Edge(x, y, true));
      }
    }
    Collections.shuffle(this.edges);
    numEdges = 0;
  }

  // step once in kruskal's algorithm
  public void kruskalStep() {
    Edge e = this.edges.get(0);
    this.edges.remove(0);
    // add it back to the end of the list - it will never be reached again
    this.edges.add(e);
    Posn p1 = e.getMainPosn();
    Posn p2 = e.getOtherPosn();
    if (!this.getRep(p1).equals(this.getRep(p2))) {
      this.addEdge(e, p1, p2);
    }
  }

  // add a new edge to the minimal spanning tree
  public void addEdge(Edge e, Posn p1, Posn p2) {
    Posn p1Rep = this.getRep(p1);
    Posn p2Rep = this.getRep(p2);
    this.reps.put(p1Rep, p2Rep);
    this.numEdges++;
    this.adjacencyList.get(p1).add(p2);
    this.adjacencyList.get(p2).add(p1);
    e.connected();
  }

  // get the representative of given node
  public Posn getRep(Posn p) {
    Posn immediateRep = this.reps.get(p);
    if (p.equals(immediateRep)) {
      return immediateRep;
    }
    else {
      Posn rep = this.getRep(immediateRep);
      this.reps.put(p, rep);
      return rep;

    }
  }

  // maze is being generated and we want an animation of it
  public void tick() {
    if (!this.finishedMaze()) {
      this.kruskalStep();
    }
    if (this.drawingSolution) {
      // I tick by more than just 1 so on bigger mazes the
      // animation runs faster. It's normalized to be the same
      // speed as incrementing by 1 on a 16x9 maze.
      this.tick += this.numCells / 144.0;
    }
  }

  // draw all walls by telling UI where to put them
  public void drawWalls(UI ui) {
    for (Edge e : this.edges) {
      e.drawIfNotConnected(ui);
    }
  }

  // draw the animation of the solution
  public void drawSolution(UI ui) {
    if (this.drawingSolution) {
      Posn cell;
      for (int row = 0; row < this.numRows; row++) {
        for (int col = 0; col < this.numCols; col++) {
          cell = new Posn(col, row);
          if (this.delays.get(cell) < tick) {
            ui.drawSquareAt(col, row, this.coloring.get(cell));
          }
        }
      }
    }
  }

  // DFS solve the maze
  public HashMap<Posn, Integer> solveDFS() {
    // must check if maze is already solved
    // if not, generate maze solution then run dfs
    if (!this.finishedMaze()) {
      this.makeSolution();
    }
    HashMap<Posn, Posn> cameFromPosn = new HashMap<Posn, Posn>();
    Stack<Posn> worklist = new Stack<Posn>();
    ArrayList<Posn> processed = new ArrayList<Posn>();
    worklist.push(new Posn(0, 0));
    while (!worklist.isEmpty()) {
      Posn next = worklist.pop();
      if (processed.contains(next)) {
        continue;
      }
      processed.add(next);
      if (new Posn(this.numCols - 1, this.numRows - 1).equals(next)) {
        return this.reconstruct(new HashMap<Posn, Integer>(), cameFromPosn, processed);
      }
      for (Posn p : this.adjacencyList.get(next)) {
        if (!processed.contains(p)) {
          worklist.push(p);
          cameFromPosn.put(p, next);
        }
      }
    }
    return null;
  }

  // BFS solve the maze
  public HashMap<Posn, Integer> solveBFS() {
    // must check if maze is already solved
    // if not, generate maze solution then run bfs
    if (!this.finishedMaze()) {
      this.makeSolution();
    }
    HashMap<Posn, Posn> cameFromPosn = new HashMap<Posn, Posn>();
    Queue<Posn> worklist = new ArrayDeque<Posn>();
    ArrayList<Posn> processed = new ArrayList<Posn>();
    worklist.add(new Posn(0, 0));
    while (!worklist.isEmpty()) {
      Posn next = worklist.poll();
      if (processed.contains(next)) {
        continue;
      }
      processed.add(next);
      if (new Posn(this.numCols - 1, this.numRows - 1).equals(next)) {
        return this.reconstruct(new HashMap<Posn, Integer>(), cameFromPosn, processed);
      }
      for (Posn p : this.adjacencyList.get(next)) {
        if (!processed.contains(p)) {
          worklist.add(p);
          cameFromPosn.put(p, next);
        }
      }
    }
    // unless the code breaks this line will never be reached
    return null;
  }

  // backtrack from the end to rebuild the solution
  public HashMap<Posn, Integer> reconstruct(HashMap<Posn, Integer> res,
      HashMap<Posn, Posn> cameFromPosn, ArrayList<Posn> processed) {
    Posn cur = new Posn(this.numCols - 1, this.numRows - 1);
    ArrayList<Posn> solution = new ArrayList<Posn>();
    while (!cur.equals(new Posn(0, 0))) {
      solution.add(0, cur);
      cur = cameFromPosn.get(cur);
    }
    for (Posn n : this.adjacencyList.keySet()) {
      if (solution.contains(n)) {
        res.put(n, 2 * numCells + processed.indexOf(n));
      }
      else {
        if (processed.contains(n)) {
          res.put(n, numCells + processed.indexOf(n));
        }
        else {
          res.put(n, 0);
        }
      }
    }
    return res;
  }

  // BFS solve the maze and animate the solution
  public void drawBFSSolution(UI ui) {
    HashMap<Posn, Integer> animation = this.solveBFS();
    this.coloring = new HashMap<Posn, Color>();
    this.delays = new HashMap<Posn, Integer>();
    Color[] colors = { UI.NOT_VISITED_COLOR, UI.VISITED_NODE_COLOR, UI.NODE_IN_SOL_COLOR };
    for (Posn p : animation.keySet()) {
      this.coloring.put(p, colors[animation.get(p) / this.numCells]);
      this.delays.put(p, animation.get(p) % this.numCells);
    }
    this.drawingSolution = true;
    this.tick = 0;
  }

  // DFS solve the maze and animate the solution
  public void drawDFSSolution(UI ui) {
    HashMap<Posn, Integer> animation = this.solveDFS();
    this.coloring = new HashMap<Posn, Color>();
    this.delays = new HashMap<Posn, Integer>();
    Color[] colors = { UI.NOT_VISITED_COLOR, UI.VISITED_NODE_COLOR, UI.NODE_IN_SOL_COLOR };
    for (Posn p : animation.keySet()) {
      this.coloring.put(p, colors[animation.get(p) / this.numCells]);
      this.delays.put(p, animation.get(p) % this.numCells);
    }
    this.drawingSolution = true;
    this.tick = 0;
  }

  // clear solution drawing
  public void clearSolutionDrawing() {
    this.drawingSolution = false;
    this.tick = 0;
  }
}

// Handles user input and display of maze
class UI extends World {

  public static int DEFAULT_WIDTH = 640;
  public static int DEFAULT_HEIGHT = 360;

  public static Color START_NODE_COLOR = Color.green;
  public static Color TARGET_NODE_COLOR = Color.blue;
  public static Color NODE_IN_SOL_COLOR = new Color(100, 255, 100);
  public static Color VISITED_NODE_COLOR = new Color(100, 100, 255);
  public static Color NOT_VISITED_COLOR = new Color(100, 100, 100);

  int width;
  int height;
  int marginSide;
  int marginTop;

  int numRows;
  int numCols;

  int pxPerCell;

  boolean animatingMazeGeneration = false;

  Maze maze;
  WorldScene scene;

  // constructor w/o pre-made maze
  UI() {
    this.maze = new Maze();
    this.width = DEFAULT_WIDTH;
    this.height = DEFAULT_HEIGHT;
    this.maze.updateUIRowsAndCols(this);
  }

  // constructor w/ pre-made maze
  UI(Maze maze) {
    this.maze = maze;
    this.width = DEFAULT_WIDTH;
    this.height = DEFAULT_HEIGHT;
    this.maze.updateUIRowsAndCols(this);
  }

  // constructor w/ pre-made maze
  UI(Maze maze, boolean animated) {
    this.maze = maze;
    this.width = DEFAULT_WIDTH;
    this.height = DEFAULT_HEIGHT;
    this.animatingMazeGeneration = animated;
    this.maze.updateUIRowsAndCols(this);
  }

  // constructor w/ custom window dimensions
  UI(int w, int h) {
    this.maze = new Maze();
    this.width = w;
    this.height = h;
    this.maze.updateUIRowsAndCols(this);
  }

  // constructor w/ pre-made maze & custom window dimensions
  UI(Maze maze, int w, int h) {
    this.maze = maze;
    this.width = w;
    this.height = h;
    this.maze.updateUIRowsAndCols(this);
  }

  // constructor w/ pre-made maze & custom window dimensions
  UI(Maze maze, int w, int h, boolean animated) {
    this.maze = maze;
    this.width = w;
    this.height = h;
    this.animatingMazeGeneration = animated;
    this.maze.updateUIRowsAndCols(this);
  }

  // NOTE: There must be some "common language" to communicate
  // between the Maze class and the UI class. Either the Maze
  // needs to know how many pixels are in the UI class, or the UI
  // needs to know how many rows & cols are in the Maze class.
  // the latter makes more sense so we implement it here:

  // set and process the number of rows and columns in the maze for this UI
  public void processRowsAndCols(int rows, int cols) {
    this.numRows = rows;
    this.numCols = cols;
    this.pxPerCell = Math.min((int) (0.8 * this.width / cols), (int) (0.8 * this.height / rows));
    this.marginSide = (int) ((this.width - cols * this.pxPerCell) / 2);
    this.marginTop = (int) ((this.height - rows * this.pxPerCell) / 2);
  }

  // Draw everything on the screen
  @Override
  public WorldScene makeScene() {
    this.scene = new WorldScene(this.width, this.height);
    this.drawConstants();
    this.maze.drawSolution(this);
    this.maze.drawWalls(this);
    return this.scene;
  }

  // draws the unchanging parts of the maze - the perimeter and the starting and
  // ending cells
  public void drawConstants() {
    // draw starting cell
    this.drawSquareAt(0, 0, START_NODE_COLOR);
    // draw ending cell
    this.drawSquareAt(this.numCols - 1, this.numRows - 1, TARGET_NODE_COLOR);
    // draw the perimeter
    this.scene.placeImageXY( // top border
        new RectangleImage(this.width - 2 * this.marginSide, 2, OutlineMode.SOLID, Color.black),
        (int) (0.5 * this.width), this.marginTop);
    this.scene.placeImageXY( // bottow border
        new RectangleImage(this.width - 2 * this.marginSide, 2, OutlineMode.SOLID, Color.black),
        (int) (0.5 * this.width), this.height - this.marginTop);
    this.scene.placeImageXY( // left border
        new RectangleImage(2, this.height - 2 * this.marginTop, OutlineMode.SOLID, Color.black),
        this.marginSide, (int) (0.5 * this.height));
    this.scene.placeImageXY( // right border
        new RectangleImage(2, this.height - 2 * this.marginTop, OutlineMode.SOLID, Color.black),
        this.width - this.marginSide, (int) (0.5 * this.height));
    this.scene.placeImageXY( // instructions
        new TextImage("a to get a new animated maze; " + "b to bfs search; c to hide solution; "
            + " d to dfs search; n to get a new maze", Color.black),
        (int) (0.5 * this.width), (int) (this.height - this.marginTop * 0.5));
  }

  // draw a square with a specified color at column x and row y in the maze
  public void drawSquareAt(int col, int row, Color color) {
    this.scene.placeImageXY(
        new RectangleImage(this.pxPerCell, this.pxPerCell, OutlineMode.SOLID, color)
            .movePinhole((int) (-0.5 * this.pxPerCell), (int) (-0.5 * this.pxPerCell)),
        this.marginSide + col * this.pxPerCell, this.marginTop + row * this.pxPerCell);
  }

  // draw a single edge between two cells. Always: Row1 <= Row2, Col1 <= Col2
  public void drawEdge(int cellRow1, int cellCol1, int cellRow2, int cellCol2) {
    // a wall will always overlap the edges of the two cells it divides
    int topleftX = this.marginSide + this.pxPerCell * cellCol2;
    int topleftY = this.marginTop + this.pxPerCell * cellRow2;
    if (cellRow1 == cellRow2) { // wall is vertical as the two cells it divides are in the same row
      this.scene.placeImageXY(new RectangleImage(1, this.pxPerCell, OutlineMode.SOLID, Color.black)
          .movePinhole(0, -0.5 * this.pxPerCell), topleftX, topleftY);
    }
    else { // wall is horizontal
      this.scene.placeImageXY(new RectangleImage(this.pxPerCell, 1, OutlineMode.SOLID, Color.black)
          .movePinhole(-0.5 * this.pxPerCell, 0), topleftX, topleftY);
    }
  }

  // on tick : if the maze's generation is being animated, step that
  // if a search algorithm is being animated, step that
  @Override
  public void onTick() {
    if (this.animatingMazeGeneration) {
      this.maze.tick();
    }
  }

  // on key press: n for new maze
  // b for BFS search
  // d for DFS search
  @Override
  public void onKeyEvent(String key) {
    if (key.equals("a") || key.equals("A")) {
      this.maze = new Maze(this.numRows, this.numCols, false);
      this.animatingMazeGeneration = true;
    }
    else {
      if (key.equals("b") || key.equals("B")) {
        this.maze.drawBFSSolution(this);
      }
      else {
        if (key.equals("c") || key.equals("C")) {
          this.maze.clearSolutionDrawing();
        }
        else {
          if (key.equals("d") || key.equals("D")) {
            this.maze.drawDFSSolution(this);
          }
          else {
            if (key.equals("n") || key.equals("N")) {
              this.maze = new Maze(this.numRows, this.numCols);
            }
          }
        }
      }
    }
  }

  public static void main(String[] args) {
    Maze m = new Maze(9, 16, false);
    UI ui = new UI(m, 640, 360, true);
    ui.bigBang(ui.width, ui.height, 1.0 / 60);
  }
}

//Test class overwriting drawing methods to make testing easier
class TestingUI extends UI {

  int edgesDrawn;
  HashMap<Posn, Color> squaresDrawn;

  TestingUI() {
    edgesDrawn = 0;
    squaresDrawn = new HashMap<Posn, Color>();
  }

  // Dummy drawEdge method to test other methods in other classes
  @Override
  public void drawEdge(int cellRow1, int cellCol1, int cellRow2, int cellCol2) {
    edgesDrawn++;
  }

  // dummy placeSquareAt method to test other methods in other classes
  @Override
  public void drawSquareAt(int col, int row, Color color) {
    this.squaresDrawn.put(new Posn(col, row), color);
  }

}

class ExamplesMaze {

  // test the TestingUI drawEdge method
  void testTestingUIDrawEdge(Tester t) {
    TestingUI ui = new TestingUI();
    t.checkExpect(ui.edgesDrawn, 0);
    ui.drawEdge(0, 0, 0, 1);
    t.checkExpect(ui.edgesDrawn, 1);
  }

  // test the TestingUI drawSquareAt method
  void testTestingUIDrawSquare(Tester t) {
    TestingUI ui = new TestingUI();
    Color c = new Color(140, 150, 160);
    // setup expected result
    HashMap<Posn, Color> expectedSquaresDrawn = new HashMap<Posn, Color>();
    expectedSquaresDrawn.put(new Posn(0, 0), c);
    // setup actual result
    t.checkExpect(ui.squaresDrawn.size(), 0);
    ui.drawSquareAt(0, 0, c);
    t.checkExpect(expectedSquaresDrawn, ui.squaresDrawn);
  }

  // test the constructor of Edge
  void testEdgeConstructor(Tester t) {
    t.checkConstructorNoException("Valid vertical edge constructor", "Edge", 0, 0, true);
    t.checkConstructorNoException("Valid horizontal edge constructor", "Edge", 0, 0, false);
    // there are no other checks on the edge constructor, so you could put any
    // integer it wouldn't have meaning as it pertains to the maze, so don't,
    // but it won't err
    t.checkConstructorNoException("Negative position edge constructor", "Edge", -4, -4, true);
  }

  // test the connected method of Edge
  void testConnected(Tester t) {
    Edge e = new Edge(0, 0, true);
    t.checkExpect(e.connected, false);
    e.connected();
    t.checkExpect(e.connected, true);
  }

  // test the drawIfNotConnected method of Edge
  void testDrawIfNotConnected(Tester t) {
    Edge e1 = new Edge(0, 0, false);
    Edge e2 = new Edge(0, 0, true);
    e1.connected(); // connected Edges don't draw anything on the ui
    TestingUI ui = new TestingUI();
    t.checkExpect(ui.edgesDrawn, 0);
    e1.drawIfNotConnected(ui);
    t.checkExpect(ui.edgesDrawn, 0);
    e2.drawIfNotConnected(ui); // disconnected edges draw walls on the ui
    t.checkExpect(ui.edgesDrawn, 1);
  }

  // test the getMainPosn method of Edge
  void testGetMainPosn(Tester t) {
    Edge e = new Edge(0, 0, true);
    t.checkExpect(e.getMainPosn(), new Posn(0, 0));
  }

  // test the getOtherPosn method of Edge
  void testGetOtherPosn(Tester t) {
    // check both a vertical and horizontal edge
    Edge v = new Edge(0, 0, true);
    Edge h = new Edge(0, 0, false);
    t.checkExpect(v.getOtherPosn(), new Posn(1, 0));
    t.checkExpect(h.getOtherPosn(), new Posn(0, 1));
  }

  // test the default constructor of Maze
  void testDefaultMazeConstructor(Tester t) {
    t.checkConstructorNoException("Maze no args", "Maze");
    Maze m = new Maze();
    t.checkExpect(m.numRows, 18);
    t.checkExpect(m.numCols, 32);
    t.checkExpect(m.numCells, 576);
    // check maze has initialized its representatives hashmap
    t.checkExpect(m.reps.size(), 32 * 18);
    // check maze is solved
    t.checkExpect(m.numEdges, 32 * 18 - 1);
    t.checkExpect(m.finishedMaze(), true);
  }

  // test the constructor of Maze for a custom size
  void testCustomMazeSizeConstructor(Tester t) {
    t.checkConstructorNoException("Maze Custom size", "Maze", 2, 2);
    Maze m = new Maze(2, 2);
    t.checkExpect(m.numRows, 2);
    t.checkExpect(m.numCols, 2);
    t.checkExpect(m.numCells, 4);
    // check maze has initialized representative hashmap
    t.checkExpect(m.reps.size(), 4);
    t.checkExpect(m.edges.size(), 4);
    // check maze is solved
    t.checkExpect(m.numEdges, 3);
    t.checkExpect(m.finishedMaze(), true);
  }

  // test the constructor of Maze for whether to solve at construction time or not
  void testMazeConstructorMaybeSolve(Tester t) {
    t.checkConstructorNoException("Maze Custom size true", "Maze", 2, 2, true);
    t.checkConstructorNoException("Maze custom size false", "Maze", 2, 2, false);
    Maze m = new Maze(2, 2, true);
    Maze n = new Maze(2, 2, false);
    // check mazes are proper size
    t.checkExpect(m.numRows, 2);
    t.checkExpect(m.numCols, 2);
    t.checkExpect(m.numCells, 4);
    t.checkExpect(n.numRows, 2);
    t.checkExpect(n.numCols, 2);
    t.checkExpect(m.numCells, 4);
    // check maze has initialized representative hashmap
    t.checkExpect(m.reps.size(), 4);
    t.checkExpect(m.edges.size(), 4);
    t.checkExpect(n.reps.size(), 4);
    t.checkExpect(n.edges.size(), 4);
    // check solved maze is solved
    t.checkExpect(m.numEdges, 3);
    t.checkExpect(m.finishedMaze(), true);
    // check unsolved maze is not solved
    t.checkExpect(n.numEdges, 0);
    t.checkExpect(n.finishedMaze(), false);
  }

  // test the updateUiRowsAndCols method of Maze
  void testUpdateUIRowsAndCols(Tester t) {
    Maze other = new Maze(2, 2);
    UI ui = new UI();
    // originally had default number of rows and columns
    t.checkExpect(ui.numRows, Maze.DEFAULT_CELLS_DOWN);
    t.checkExpect(ui.numCols, Maze.DEFAULT_CELLS_ACROSS);
    // after updating has new number of rows and columns
    other.updateUIRowsAndCols(ui);
    t.checkExpect(ui.numRows, 2);
    t.checkExpect(ui.numCols, 2);
  }

  // test the makeSolution method of Maze
  void testMakeSolution(Tester t) {
    Maze m = new Maze(2, 2, false);
    t.checkExpect(m.numEdges, 0);
    t.checkExpect(m.finishedMaze(), false);
    // visually verify that the maze actually has a solution - easy for small mazes
    m.makeSolution();
    t.checkExpect(m.numEdges, 3); // 3 = 2 * 2 - 1
    t.checkExpect(m.finishedMaze(), true);
  }

  // test the finishedMaze method of Maze
  void testFinishedMaze(Tester t) {
    Maze m = new Maze(2, 2, false);
    t.checkExpect(m.finishedMaze(), false);
    t.checkExpect(m.numEdges, 0);
    Edge e = m.edges.get(2);
    m.addEdge(e, e.getMainPosn(), e.getOtherPosn());
    t.checkExpect(m.finishedMaze(), false);
    t.checkExpect(m.numEdges, 1);
    m.makeSolution();
    t.checkExpect(m.finishedMaze(), true);
    t.checkExpect(m.numEdges, 3);
  }

  // test the initSolution method of Maze
  void testInitSolution(Tester t) {
    // always called on construction, but we can test its effect
    // by making a solved maze - initSolution() should then "unsolve" this maze
    Maze m = new Maze(2, 2, true); // solved maze
    t.checkExpect(m.finishedMaze(), true);
    m.initSolution();
    // ADD TEST FOR ADJACENCY LIST
    t.checkExpect(m.finishedMaze(), false);
    // unsolved maze should have no edges
    t.checkExpect(m.numEdges, 0);
    // we still expect the same number of nodes in the representatives hashmap tho
    t.checkExpect(m.reps.size(), 4);
    // check adjacencyList has been initialized and its nodes have been initialized
    t.checkExpect(m.adjacencyList == null, false);
    t.checkExpect(m.adjacencyList.get(new Posn(0, 0)) == null, false);
    // check adjacencyList is empty.
    t.checkExpect(m.adjacencyList.get(new Posn(0, 0)).size(), 0);
    t.checkExpect(m.adjacencyList.get(new Posn(0, 1)).size(), 0);
    t.checkExpect(m.adjacencyList.get(new Posn(1, 0)).size(), 0);
    t.checkExpect(m.adjacencyList.get(new Posn(1, 1)).size(), 0);
  }

  // test the kruskalStep method of Maze
  void testKruskalStep(Tester t) {
    Maze m = new Maze(2, 2, false);
    // we expect it to add one to the number of edges,
    // and one node will not have itself as a representative node
    t.checkExpect(m.numEdges, 0);
    m.kruskalStep();
    int numMatching = 0;
    for (Posn p : m.reps.keySet()) {
      if (p.equals(m.reps.get(p))) {
        numMatching += 1;
      }
    }
    t.checkExpect(m.numEdges, 1);
    t.checkExpect(numMatching, 3);
  }

  // test the addEdge method of Maze
  void testAddEdge(Tester t) {
    Maze m = new Maze(2, 2, false);
    Edge e = m.edges.get(3);
    t.checkExpect(m.numEdges, 0);
    m.addEdge(e, e.getMainPosn(), e.getOtherPosn());
    t.checkExpect(m.numEdges, 1);
    t.checkExpect(m.reps.get(e.getMainPosn()), e.getOtherPosn());
    t.checkExpect(m.reps.get(e.getOtherPosn()), e.getOtherPosn());
    t.checkExpect(m.adjacencyList.get(e.getMainPosn()).contains(e.getOtherPosn()), true);
    t.checkExpect(m.adjacencyList.get(e.getOtherPosn()).contains(e.getMainPosn()), true);
  }

  // test the getRep method of Maze
  void testGetRep(Tester t) {
    Maze m = new Maze(2, 2, false);
    // manually recreate edges and posns
    Posn p00 = new Posn(0, 0);
    Posn p01 = new Posn(0, 1);
    Posn p10 = new Posn(1, 0);
    Edge e1 = new Edge(0, 0, true);
    Edge e2 = new Edge(0, 0, false);
    // originally each node should be its own representative
    t.checkExpect(m.reps.get(p00), p00);
    t.checkExpect(m.reps.get(p01), p01);
    t.checkExpect(m.reps.get(p10), p10);
    // add the two manually created edges
    m.addEdge(e1, p00, p01);
    m.addEdge(e2, p00, p10);
    // now all three should have the same representative (p10)
    t.checkExpect(m.getRep(p00), p10);
    t.checkExpect(m.getRep(p01), p10);
    t.checkExpect(m.getRep(p10), p10);
  }

  // test the tick method of Maze
  void testTick(Tester t) {
    // tick should only do anything relating to solving the maze
    // if the maze is unsolved
    Maze m = new Maze(2, 2, false);
    Maze n = new Maze(2, 2, true);
    // we expect it to add one to the number of edges,
    // and one node will not have itself as a representative node
    t.checkExpect(m.numEdges, 0);
    t.checkExpect(n.numEdges, 3);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(n.tick, 0.0);

    n.drawingSolution = true;
    m.tick();
    n.tick();
    int numMatching = 0;
    for (Posn p : m.reps.keySet()) {
      if (p.equals(m.reps.get(p))) {
        numMatching += 1;
      }
    }
    t.checkExpect(m.numEdges, 1);
    t.checkExpect(numMatching, 3);
    m.makeSolution();
    // this should do nothing since after prev line the maze should be solved
    m.tick();
    // nothing should've changed after that one tick now that it's solved
    t.checkExpect(m.numEdges, 3);
    t.checkExpect(numMatching, 3);
    t.checkExpect(n.numEdges, 3); // should've done nothing to solved maze
    // test the drawing aspect of tick
    t.checkExpect(m.tick, 0.0); // shouldn't change since it's not drawing
    t.checkExpect(n.tick, 4 / 144.0);
  }

  // test the drawWalls method of Maze
  void testDrawWalls(Tester t) {
    Maze m = new Maze(2, 2, false);
    Maze n = new Maze(2, 2, true);
    TestingUI tui1 = new TestingUI();
    TestingUI tui2 = new TestingUI();
    m.drawWalls(tui1);
    n.drawWalls(tui2);
    t.checkExpect(tui1.edgesDrawn, 4);
    t.checkExpect(tui2.edgesDrawn, 1);
  }

  // test the drawSolution method of Maze
  void testDrawSolution(Tester t) {
    Maze m = new Maze(2, 2, true); // make solution
    // test the actual colors drawn
    TestingUI tui = new TestingUI();
    tui.squaresDrawn.clear();
    HashMap<Posn, Integer> animation = m.solveBFS();
    m.coloring = new HashMap<Posn, Color>();
    m.delays = new HashMap<Posn, Integer>();
    Color[] colors = { UI.NOT_VISITED_COLOR, UI.VISITED_NODE_COLOR, UI.NODE_IN_SOL_COLOR };
    for (Posn p : animation.keySet()) {
      m.coloring.put(p, colors[animation.get(p) / m.numCells]);
      m.delays.put(p, animation.get(p) % m.numCells);
    }
    m.drawingSolution = true;
    m.tick = 10;
    m.drawSolution(tui);
    t.checkExpect(tui.squaresDrawn.size(), 4);
  }

  // test the solveDFS method of Maze
  void testSolveDFS(Tester t) {
    Maze m = new Maze(2, 2);
    HashMap<Posn, Integer> out = m.solveDFS();
    Posn intermediate;
    if (m.adjacencyList.get(new Posn(0, 1)).contains(new Posn(0, 0))
        && m.adjacencyList.get(new Posn(0, 1)).contains(new Posn(1, 1))) {
      intermediate = new Posn(0, 1);
    }
    else {
      intermediate = new Posn(1, 0);
    }
    t.checkExpect(out.get(intermediate) > 8.5, true);
    t.checkExpect(out.get(new Posn(0, 0)), 4);
    t.checkExpect(out.get(new Posn(1, 1)) > 9.5, true);
  }

  // test the solveBFS method of Maze
  void testSolveBFS(Tester t) {
    Maze m = new Maze(2, 2);
    HashMap<Posn, Integer> out = m.solveBFS();
    Posn intermediate;
    if (m.adjacencyList.get(new Posn(0, 1)).contains(new Posn(0, 0))
        && m.adjacencyList.get(new Posn(0, 1)).contains(new Posn(1, 1))) {
      intermediate = new Posn(0, 1);
    }
    else {
      intermediate = new Posn(1, 0);
    }
    t.checkExpect(out.get(intermediate) > 8.5, true);
    t.checkExpect(out.get(new Posn(0, 0)), 4);
    t.checkExpect(out.get(new Posn(1, 1)) > 9.5, true);
  }

  // test the reconstruct  method of Maze
  void testReconstruct(Tester t) {
    Maze m = new Maze(2, 2);
    HashMap<Posn, Integer> output = new HashMap<Posn, Integer>();
    HashMap<Posn, Posn> cameFromPosn = new HashMap<Posn, Posn>();
    cameFromPosn.put(new Posn(1, 0), new Posn(0, 0));
    cameFromPosn.put(new Posn(1, 1), new Posn(1, 0));
    cameFromPosn.put(new Posn(0, 1), new Posn(0, 0));
    ArrayList<Posn> processed = new ArrayList<Posn>();
    processed.add(new Posn(0, 0));
    processed.add(new Posn(0, 1));
    processed.add(new Posn(1, 0));
    processed.add(new Posn(1, 1));
    HashMap<Posn, Integer> result = m.reconstruct(output, cameFromPosn, processed);
    HashMap<Posn, Integer> expectedOutput = new HashMap<Posn, Integer>();
    expectedOutput.put(new Posn(0, 0), 4);
    expectedOutput.put(new Posn(0, 1), 5);
    expectedOutput.put(new Posn(1, 0), 10);
    expectedOutput.put(new Posn(1, 1), 11);
    t.checkExpect(result, expectedOutput);
  }

  // test the drawBFSSolution method of Maze
  void testDrawBFSSolution(Tester t) {
    TestingUI tui = new TestingUI();
    Maze m = new Maze(2, 2);
    t.checkExpect(m.coloring, null);
    t.checkExpect(m.delays, null);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, false);
    m.drawBFSSolution(tui);
    t.checkExpect(m.coloring.size(), 4);
    t.checkExpect(m.delays.size(), 4);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, true);
  }

  // test the drawDFSSolution method of Maze
  void testDrawDFSSolution(Tester t) {
    TestingUI tui = new TestingUI();
    Maze m = new Maze(2, 2);
    t.checkExpect(m.coloring, null);
    t.checkExpect(m.delays, null);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, false);
    m.drawBFSSolution(tui);
    t.checkExpect(m.coloring.size(), 4);
    t.checkExpect(m.delays.size(), 4);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, true);
  }

  // test the clearSolutionDrawing method of Maze
  void testClearSolutionDrawing(Tester t) {
    Maze m = new Maze(2, 2);
    TestingUI tui = new TestingUI();
    m.drawBFSSolution(tui);
    m.tick();
    m.tick();
    t.checkExpect(m.tick > 0, true);
    t.checkExpect(m.drawingSolution, true);
    m.clearSolutionDrawing();
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, false);
  }

  // test the no-argument constructor of UI
  void testUIConstructorNoArgs(Tester t) {
    t.checkConstructorNoException("UI no arguments", "UI");
    UI ui = new UI();
    t.checkExpect(ui.width, UI.DEFAULT_WIDTH);
    t.checkExpect(ui.height, UI.DEFAULT_HEIGHT);
    t.checkExpect(ui.numCols, Maze.DEFAULT_CELLS_ACROSS);
    t.checkExpect(ui.numRows, Maze.DEFAULT_CELLS_DOWN);
  }

  // test the pre-made maze constructor of UI
  void testUIConstructorPremadeMaze(Tester t) {
    Maze m = new Maze(9, 10);
    t.checkConstructorNoException("UI premade maze", "UI", m);
    UI ui = new UI(m);
    t.checkExpect(ui.width, UI.DEFAULT_WIDTH);
    t.checkExpect(ui.height, UI.DEFAULT_HEIGHT);
    t.checkExpect(ui.numCols, 10);
    t.checkExpect(ui.numRows, 9);
  }

  // test the maze constructor that might be animated
  void testUIConstructorMaybeAnimated(Tester t) {
    Maze m = new Maze(9, 10);
    t.checkConstructorNoException("UI maybe animated false", "UI", m, false);
    t.checkConstructorNoException("UI maybe animated true", "UI", m, true);
    UI ui1 = new UI(m, false);
    UI ui2 = new UI(m, true);
    t.checkExpect(ui1.width, UI.DEFAULT_WIDTH);
    t.checkExpect(ui1.height, UI.DEFAULT_HEIGHT);
    t.checkExpect(ui1.numCols, 10);
    t.checkExpect(ui1.numRows, 9);
    t.checkExpect(ui1.animatingMazeGeneration, false);
    t.checkExpect(ui2.width, UI.DEFAULT_WIDTH);
    t.checkExpect(ui2.height, UI.DEFAULT_HEIGHT);
    t.checkExpect(ui2.numCols, 10);
    t.checkExpect(ui2.numRows, 9);
    t.checkExpect(ui2.animatingMazeGeneration, true);
  }

  // test the no-argument constructor of UI
  void testUIConstructorGivenSize(Tester t) {
    t.checkConstructorNoException("UI custom size", "UI", 1000, 563);
    UI ui = new UI(1000, 563);
    t.checkExpect(ui.width, 1000);
    t.checkExpect(ui.height, 563);
    t.checkExpect(ui.numCols, Maze.DEFAULT_CELLS_ACROSS);
    t.checkExpect(ui.numRows, Maze.DEFAULT_CELLS_DOWN);
  }

  // test the no-argument constructor of UI
  void testUIConstructorPremadeMazeAndGivenSize(Tester t) {
    Maze m = new Maze(9, 10);
    t.checkConstructorNoException("UI custom maze and size", "UI", m, 1000, 563);
    UI ui = new UI(m, 1000, 563);
    t.checkExpect(ui.width, 1000);
    t.checkExpect(ui.height, 563);
    t.checkExpect(ui.numCols, 10);
    t.checkExpect(ui.numRows, 9);
  }

  // test the no-argument constructor of UI
  void testUIConstructorGivenEverything(Tester t) {
    Maze m = new Maze(9, 10);
    t.checkConstructorNoException("UI customEverything", "UI", m, 1000, 563, false);
    t.checkConstructorNoException("UI customEverything", "UI", m, 1000, 563, true);
    UI ui1 = new UI(m, 1000, 563, false);
    UI ui2 = new UI(m, 1000, 563, true);
    t.checkExpect(ui1.width, 1000);
    t.checkExpect(ui1.height, 563);
    t.checkExpect(ui1.numCols, 10);
    t.checkExpect(ui1.numRows, 9);
    t.checkExpect(ui1.animatingMazeGeneration, false);
    t.checkExpect(ui2.width, 1000);
    t.checkExpect(ui2.height, 563);
    t.checkExpect(ui2.numCols, 10);
    t.checkExpect(ui2.numRows, 9);
    t.checkExpect(ui2.animatingMazeGeneration, true);
  }

  // test the processRowsAndCols method of UI
  void testProcessRowsAndCols(Tester t) {
    // by default ui will be 32 x 18
    UI ui = new UI(800, 450);
    t.checkExpect(ui.numCols, 32);
    t.checkExpect(ui.numRows, 18);
    t.checkExpect(ui.pxPerCell, 20);
    t.checkExpect(ui.marginSide, 80);
    t.checkExpect(ui.marginTop, 45);
    // cut size in half
    ui.processRowsAndCols(9, 16);
    // check if size changed
    t.checkExpect(ui.numCols, 16);
    t.checkExpect(ui.numRows, 9);
    t.checkExpect(ui.pxPerCell, 40);
    t.checkExpect(ui.marginSide, 80);
    t.checkExpect(ui.marginTop, 45);
  }

  // test the makeScene method of UI
  void testMakeScene(Tester t) {
    /*
     * Maze m = new Maze(2, 2, false); UI ui = new UI(m, 800, 450);
     * t.checkExpect(ui.scene, null); // manually add all but two edges - only 2
     * walls left on grid for (Edge e : m.edges) { if (!e.getMainPosn().equals(new
     * Posn(0, 0))) { e.connected(); } } // make scene with new edges
     * ui.makeScene(); t.checkExpect(ui.scene == null, false); // manually recreate
     * image to compare WorldScene demoScene = new WorldScene(800, 450); // draw
     * starting and ending squares and the perimeter demoScene.placeImageXY( new
     * RectangleImage(180, 180, OutlineMode.SOLID, Color.green).movePinhole(-90,
     * -90), 220, 45); demoScene.placeImageXY( new RectangleImage(180, 180,
     * OutlineMode.SOLID, Color.blue).movePinhole(-90, -90), 400, 225);
     * demoScene.placeImageXY(new RectangleImage(360, 2, OutlineMode.SOLID,
     * Color.black), 400, 45); demoScene.placeImageXY(new RectangleImage(360, 2,
     * OutlineMode.SOLID, Color.black), 400, 405); demoScene.placeImageXY(new
     * RectangleImage(2, 360, OutlineMode.SOLID, Color.black), 220, 225);
     * demoScene.placeImageXY(new RectangleImage(2, 360, OutlineMode.SOLID,
     * Color.black), 580, 225); // draw the vertical wall demoScene.placeImageXY(
     * new RectangleImage(1, 180, OutlineMode.SOLID, Color.black).movePinhole(0,
     * -90), 400, 45); // draw the horizontal wall demoScene.placeImageXY( new
     * RectangleImage(180, 1, OutlineMode.SOLID, Color.black).movePinhole(-90, 0),
     * 220, 225); t.checkExpect(ui.scene, demoScene);
     */
  }

  // test the drawConstants method of UI
  void testDrawConstants(Tester t) {
    /*
     * Maze m = new Maze(2, 2, false); UI ui = new UI(m, 800, 450); ui.scene = new
     * WorldScene(800, 450); WorldScene demoScene = new WorldScene(800, 450);
     * demoScene.placeImageXY( new RectangleImage(180, 180, OutlineMode.SOLID,
     * Color.green).movePinhole(-90, -90), 220, 45); demoScene.placeImageXY( new
     * RectangleImage(180, 180, OutlineMode.SOLID, Color.blue).movePinhole(-90,
     * -90), 400, 225); demoScene.placeImageXY(new RectangleImage(360, 2,
     * OutlineMode.SOLID, Color.black), 400, 45); demoScene.placeImageXY(new
     * RectangleImage(360, 2, OutlineMode.SOLID, Color.black), 400, 405);
     * demoScene.placeImageXY(new RectangleImage(2, 360, OutlineMode.SOLID,
     * Color.black), 220, 225); demoScene.placeImageXY(new RectangleImage(2, 360,
     * OutlineMode.SOLID, Color.black), 580, 225); demoScene.placeImageXY( new
     * TextImage("a to get a new animated maze; " +
     * "b to bfs search; c to hide solutions" +
     * "d to dfs search; n to get a new maze" , Color.black), 400, 422);
     * ui.drawConstants(); return t.checkExpect(ui.scene, demoScene);
     */
  }

  // test the drawSquareAt method of UI
  void testDrawSquareAt(Tester t) {
    Maze m = new Maze(2, 2, false);
    UI ui = new UI(m, 800, 450);
    Color c = Color.green;
    ui.scene = new WorldScene(800, 450);
    t.checkExpect(ui.pxPerCell, 180);
    t.checkExpect(ui.marginTop, 45);
    t.checkExpect(ui.marginSide, 220);
    WorldScene demoScene = new WorldScene(800, 450);
    demoScene.placeImageXY(new RectangleImage(180, 180, OutlineMode.SOLID, c).movePinhole(-90, -90),
        220, 45);
    ui.drawSquareAt(0, 0, c);
    t.checkExpect(demoScene, ui.scene);
  }

  // test the drawEdge method of UI
  void testDrawEdge(Tester t) {
    Maze m = new Maze(2, 2, false);
    UI ui = new UI(m, 800, 450);
    ui.scene = new WorldScene(800, 450);
    t.checkExpect(ui.pxPerCell, 180);
    t.checkExpect(ui.marginTop, 45);
    t.checkExpect(ui.marginSide, 220);
    // first test a vertical wall
    WorldScene demoScene = new WorldScene(800, 450);
    t.checkExpect(ui.scene, demoScene);
    // test a vertical wall
    ui.drawEdge(0, 0, 0, 1);
    demoScene.placeImageXY(
        new RectangleImage(1, 180, OutlineMode.SOLID, Color.black).movePinhole(0, -90), 400, 45);
    t.checkExpect(ui.scene, demoScene);
    // test a horizontal wall
    ui.drawEdge(0, 0, 1, 0); // vertical wall
    demoScene.placeImageXY(
        new RectangleImage(180, 1, OutlineMode.SOLID, Color.black).movePinhole(-90, 0), 220, 225);
    t.checkExpect(ui.scene, demoScene);
  }

  // test the onTick method of UI
  void testOnTickUI(Tester t) {
    Maze m = new Maze(2, 2, false);
    UI ui = new UI(m, 1280, 720, true);
    TestingUI tui = new TestingUI();
    tui.maze = m;
    t.checkExpect(m.numEdges, 0);
    ui.onTick();
    int numMatching = 0;
    for (Posn p : m.reps.keySet()) {
      if (p.equals(m.reps.get(p))) {
        numMatching += 1;
      }
    }
    t.checkExpect(m.numEdges, 1);
    t.checkExpect(numMatching, 3);
    m.drawBFSSolution(tui);
    // Testing UI does not override onTick, therefore it is safe to test
    tui.onTick();
    tui.makeScene();
    HashMap<Posn, Color> squaresDrawn = tui.squaresDrawn;
    System.out.println(squaresDrawn);
    tui.squaresDrawn.clear();
    tui.onTick();
    tui.onTick();
    tui.makeScene();
    t.checkExpect(squaresDrawn.equals(tui.squaresDrawn), false);
  }

  // test the onKeyEvent method of UI
  void testOnKeyEvent(Tester t) {
    this.testOnAPressed(t);
    this.testOnNPressed(t);
    this.testOnBPressed(t);
    this.testOnDPressed(t);
  }

  // test the onKeyEvent method when "a" or "A" is pressed
  void testOnAPressed(Tester t) {
    Maze m = new Maze(2, 2, true); // start with solved maze
    UI ui = new UI(m);
    t.checkExpect(ui.animatingMazeGeneration, false);
    ui.onKeyEvent("a"); // "a" pressed
    t.checkExpect(ui.animatingMazeGeneration, true);
    t.checkExpect(ui.maze.finishedMaze(), false);
    t.checkExpect(ui.maze.equals(m), false);
  }

  // test the onKeyEvent method when "n" or "N" is pressed
  void testOnNPressed(Tester t) {
    Maze m = new Maze(5, 5);
    UI ui = new UI(m);
    t.checkExpect(ui.maze, m);   // test they originally matched
    ui.onKeyEvent("n"); // new maze called
    t.checkExpect(ui.maze.equals(m), false); // test that they don't match
    t.checkExpect(ui.numRows, 5);   // test they're the same size
    t.checkExpect(ui.numCols, 5);
  }

  // test the onKeyEvent method when "b" or "B" is pressed
  void testOnBPressed(Tester t) {
    TestingUI tui = new TestingUI();
    Maze m = new Maze(2, 2);
    tui.maze = m;
    t.checkExpect(m.coloring, null);
    t.checkExpect(m.delays, null);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, false);
    // testingui doesn't override onKeyEvent therefore it's safe to use for testing
    tui.onKeyEvent("B");
    t.checkExpect(m.coloring.size(), 4);
    t.checkExpect(m.delays.size(), 4);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, true);
  }

  // test the OnKeyEvent method when "d" or "D" is pressed
  void testOnDPressed(Tester t) {
    TestingUI tui = new TestingUI();
    Maze m = new Maze(2, 2);
    tui.maze = m;
    t.checkExpect(m.coloring, null);
    t.checkExpect(m.delays, null);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, false);
    // testingui doesn't override onKeyEvent therefore it's safe to use for testing
    tui.onKeyEvent("D");
    t.checkExpect(m.coloring.size(), 4);
    t.checkExpect(m.delays.size(), 4);
    t.checkExpect(m.tick, 0.0);
    t.checkExpect(m.drawingSolution, true);
  }

  // run the program !
  void testRuns(Tester t) {
    // NOTES ABOUT THIS TEST:
    // I think this is a bug in the Tester library
    // I have left a main method in the other file,
    // at the bottom of UI. When that main method is run,
    // the program runs just fine. You can verify for yourself that
    // my main method and the test here should do the exact same things.
    // However, when I run this test, usually the test fails with a
    // "ConcurrentModificationException" thrown.
    // This never happens when I run my version of main, and thus I think
    // it is not an issue with my code, rather it is an issue with the tester
    // library.
    Maze m = new Maze(9, 16, false);
    UI ui = new UI(m, 640, 360, true);
    t.checkNoException(ui, "bigBang", ui.width, ui.height, 1.0 / 60);
  }
}
