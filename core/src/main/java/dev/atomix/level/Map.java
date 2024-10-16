package dev.atomix.level;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector3;
import dev.atomix.Game;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Map {

    public enum TileType {
        NONE, WALL, FLOOR
    }

    private final TileType[][] m_Tiles;
    private final int m_Width, m_Height;
    private final TextureRegion m_WallTexture, m_EmptyTexture, m_FloorTexture;

    private Vector3 m_BaseColor;
    private Vector3 m_AccentColor;

    public Map(int width, int height, TextureRegion wall, TextureRegion empty, TextureRegion floor) {
        m_Width = width;
        m_Height = height;
        m_Tiles = new TileType[m_Width][m_Height];
        m_WallTexture  = new TextureRegion(wall);
        m_EmptyTexture = new TextureRegion(empty);
        m_FloorTexture = new TextureRegion(floor);

        ThreadLocalRandom rand = ThreadLocalRandom.current();
        m_BaseColor = new Vector3(rand.nextFloat(), rand.nextFloat(), rand.nextFloat());
        m_AccentColor = new Vector3(m_BaseColor.x, m_BaseColor.y, rand.nextFloat());

        generateMap();
    }

    public void render(SpriteBatch batch, int tileSize) {
        for (int x = 0; x < m_Width; ++x)
            for (int y = 0; y < m_Height; ++y)
                renderTiles(batch, x, y, tileSize, m_BaseColor, m_AccentColor);

        batch.setColor(Color.WHITE);
    }

    private void renderTiles(SpriteBatch batch, int x, int y, int tileSize, Vector3 base, Vector3 accent) {
        switch (m_Tiles[x][y]) {
            case WALL:
                batch.setColor(base.x, base.y, base.z, 1.0f);
                batch.draw(m_WallTexture, x * tileSize, y * tileSize, tileSize, tileSize); // Wall
                break;
            case FLOOR:
                batch.setColor(accent.x, accent.y, accent.z, 1.0f);
                batch.draw(m_FloorTexture, x * tileSize, y * tileSize, tileSize, tileSize); // Floor
                break;
            default:
                batch.setColor(base.x, base.y, base.z, 1.0f);
                batch.draw(m_EmptyTexture, x * tileSize, y * tileSize, tileSize, tileSize); // Empty
                break;
        }
    }

    private void generateMap() {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        // Phase 1: Fill the map with NONE tiles
        Future<Void> fillFuture = executor.submit(() -> {
            fillWithNoneTiles();
            return null; // No result needed
        });

        // Phase 2: Create rooms in a pseudo-random fashion
        Future<List<Room>> roomsFuture = executor.submit(() -> {
            List<Room> rooms = new ArrayList<>();
            int roomCount = ThreadLocalRandom.current().nextInt(5, 11);
            for (int i = 0; i < roomCount; i++) {
                Room room = createRoom();
                if(room == null) continue;

                if (!isTooCloseToExistingRooms(room, rooms)) {
                    carveRoom(room);
                    rooms.add(room);
                }
            }
            return rooms; // Return the list of rooms created
        });

        try {
            // Wait for Phase 1 to finish
            fillFuture.get();
            // Wait for Phase 2 to finish and get the rooms
            List<Room> rooms = roomsFuture.get();

            // Phase 3: Connect rooms with corridors using A* pathfinding
            Future<Void> connectFuture = executor.submit(() -> {
                connectRooms(rooms);
                return null; // No result needed
            });

            // Wait for Phase 3 to finish
            connectFuture.get();

            // Phase 4: Set walls based on floor tile proximity
            Future<Void> wallFuture = executor.submit(() -> {
                setWalls();
                return null; // No result needed
            });

            // Wait for Phase 4 to finish
            wallFuture.get();

        } catch (Exception e) {
            Game.LOGGER.debug(e.getMessage(), e);
        } finally {
            executor.shutdown(); // Shut down the executor service
        }
    }

    private void fillWithNoneTiles() {
        for (int x = 0; x < m_Width; ++x)
            for (int y = 0; y < m_Height; ++y)
                m_Tiles[x][y] = TileType.NONE; // Start with all empty tiles
    }

    private Room createRoom() {
        // Randomly decide room shape
        int shapeType = ThreadLocalRandom.current().nextInt(0, 3); // 0: Rectangle, 1: L-shape, 2: Polygon
        return switch (shapeType) {
            case 1 -> // L-shape
                createLShapedRoom();
            case 2 -> // Random polygon
                createPolygonRoom();
            default -> // Rectangle
                createRectangularRoom();
        };
    }

    private Room createRectangularRoom() {
        int roomWidth = ThreadLocalRandom.current().nextInt(4, 10); // Room width between 3 and 8
        int roomHeight = ThreadLocalRandom.current().nextInt(4, 10); // Room height between 3 and 8
        int roomX = ThreadLocalRandom.current().nextInt(1, m_Width - roomWidth - 1); // Ensure room fits in map
        int roomY = ThreadLocalRandom.current().nextInt(1, m_Height - roomHeight - 1);

        return new Room(roomX, roomY, roomWidth, roomHeight);
    }

    private Room createLShapedRoom() {
        // Define dimensions for L-shaped rooms
        int roomWidth1 = 5;
        int roomHeight1 = 4;
        int roomWidth2 = 4;
        int roomHeight2 = 3;

        int roomX = ThreadLocalRandom.current().nextInt(1, m_Width - roomWidth1 - roomWidth2 - 1); // Ensure room fits in map
        int roomY = ThreadLocalRandom.current().nextInt(1, m_Height - Math.max(roomHeight1, roomHeight2) - 1);
        int orientation = ThreadLocalRandom.current().nextInt(0, 4); // Random orientation

        return new LRoom(roomX, roomY, roomWidth1, roomHeight1, roomWidth2, roomHeight2, orientation);
    }

    private Room createPolygonRoom() {
        // Randomly decide the number of vertices for the polygon (between 3 and 8)
        int vertexCount = ThreadLocalRandom.current().nextInt(3, 8);
        List<Point> vertices = new ArrayList<>();

        // Generate random angles and distances to create the polygon shape
        double[] angles = new double[vertexCount];
        double[] distances = new double[vertexCount];

        for (int i = 0; i < vertexCount; i++) {
            angles[i] = ThreadLocalRandom.current().nextDouble(0, Math.PI * 2); // Random angle
            distances[i] = ThreadLocalRandom.current().nextDouble(3, 6); // Random distance
        }

        // Create vertices based on polar coordinates
        for (int i = 0; i < vertexCount; i++) {
            int x = (int) (distances[i] * Math.cos(angles[i]));
            int y = (int) (distances[i] * Math.sin(angles[i]));
            vertices.add(new Point(x, y));
        }

        // Offset the polygon to ensure it fits within the bounds of the map
        int roomX = ThreadLocalRandom.current().nextInt(1, m_Width - 10);
        int roomY = ThreadLocalRandom.current().nextInt(1, m_Height - 10);
        for (Point p : vertices) {
            p.x += roomX;
            p.y += roomY;
        }

        // Create and return the PolygonRoom
        return new PolygonRoom(roomX, roomY, vertices);
    }

    private boolean isTooCloseToExistingRooms(Room newRoom, List<Room> existingRooms) {
        for (Room room : existingRooms)
            if (newRoom.intersects(room)) return true; // Rooms are too close

        return false; // No overlap with existing rooms
    }

    private void carveRoom(Room room) {
        if (room instanceof PolygonRoom polygonRoom) {
            // Carve the polygon room using the custom carving method
            polygonRoom.carvePolygon(m_Tiles, polygonRoom.vertices);
        } else if (room instanceof LRoom lRoom) {
            // Carve the L-shaped room
            lRoom.carveLShape(m_Tiles);
        } else {
            // Default rectangular room carving
            for (int x = room.x; x < room.x + room.width; ++x) {
                for (int y = room.y; y < room.y + room.height; ++y) {
                    m_Tiles[x][y] = TileType.FLOOR; // Carve out the floor
                }
            }
        }
    }

    private void connectRooms(List<Room> rooms) {
        try(ExecutorService executor = Executors.newFixedThreadPool(rooms.size())) {
            List<Future<Boolean>> futures = new ArrayList<>();
            List<Room> connectedRooms = new ArrayList<>();
            connectedRooms.add(rooms.get(0)); // Start with the first room

            // Connect rooms one by one ensuring all are connected
            for (int i = 1; i < rooms.size(); i++) {
                Room currRoom = rooms.get(i);
                if(currRoom.connections >= Room.MAX_CONNECTIONS) continue;

                Room prevRoom = findRoomToConnect(currRoom, connectedRooms);
                if(prevRoom == null) continue;
                if(prevRoom.connections >= Room.MAX_CONNECTIONS) continue;

                carveCorridorWithAStar(prevRoom, currRoom);
                connectedRooms.add(currRoom);

                prevRoom.connections++;
                currRoom.connections++;

                // Check connectivity in a separate thread
                futures.add(executor.submit(() -> areAllRoomsConnected(rooms)));
            }

            // Optionally connect rooms randomly for more interconnectivity
            for (Room roomA : connectedRooms) {
                if(roomA.connections >= Room.MAX_CONNECTIONS) continue;

                for (Room roomB : connectedRooms) {
                    if(roomB.connections >= Room.MAX_CONNECTIONS) continue;

                    if (roomA != roomB) {
                        carveCorridorWithAStar(roomA, roomB);

                        roomA.connections++;
                        roomB.connections++;

                        // Check connectivity in a separate thread
                        futures.add(executor.submit(() -> areAllRoomsConnected(rooms)));
                    }
                }
            }

            // Gather results and handle disconnections
            for (Future<Boolean> future : futures) {
                try {
                    if (!future.get()) {
                        // Handle disconnection if necessary (e.g., log a warning)
                    }
                } catch (InterruptedException | ExecutionException e) {
                    Game.LOGGER.debug(e.getMessage(), e);
                }
            }

            executor.shutdown(); // Shutdown the executor
        }
    }

    private Room findRoomToConnect(Room currRoom, List<Room> connectedRooms) {
        for (Room prevRoom : connectedRooms) {
            if(currRoom == prevRoom) continue;
            if (prevRoom.connections < Room.MAX_CONNECTIONS) return prevRoom;
        }

        return null; // No suitable room found
    }

    private boolean areAllRoomsConnected(List<Room> rooms) {
        Node start = new Node(rooms.get(0).x + rooms.get(0).width / 2, rooms.get(0).y + rooms.get(0).height / 2);

        for (Room room : rooms) {
            Node goal = new Node(room.x + room.width / 2, room.y + room.height / 2);
            if (aStar(start, goal).isEmpty()) return false;
        }

        return true;
    }

    private void carveCorridorWithAStar(Room a, Room b) {
        // Use A* algorithm to find path from center of room A to center of room B
        Node start = new Node(a.x + a.width / 2, a.y + a.height / 2);
        Node goal = new Node(b.x + b.width / 2, b.y + b.height / 2);
        List<Node> path = aStar(start, goal);

        // Carve corridor along the path
        for (Node node : path) {
            m_Tiles[node.x][node.y] = TileType.FLOOR; // Carve out the corridor as a floor
        }
    }

    private List<Node> aStar(Node start, Node goal) {
        // A* pathfinding logic
        List<Node> openSet = new ArrayList<>();
        List<Node> closedSet = new ArrayList<>();
        openSet.add(start);

        while (!openSet.isEmpty()) {
            Node current = openSet.remove(0);
            if (current.equals(goal))
                return reconstructPath(current); // Return the path to the goal

            closedSet.add(current);
            for (Node neighbor : getNeighbors(current)) {
                if (closedSet.contains(neighbor) || !isValidTile(neighbor.x, neighbor.y))
                    continue;

                if (!openSet.contains(neighbor)) {
                    neighbor.parent = current; // Set parent for path reconstruction
                    openSet.add(neighbor);
                }
            }
        }

        return new ArrayList<>(); // Return an empty path if no path found
    }

    private List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        // Add 4-directional neighbors (up, down, left, right)
        if (node.x > 0) neighbors.add(new Node(node.x - 1, node.y));
        if (node.x < m_Width - 1) neighbors.add(new Node(node.x + 1, node.y));
        if (node.y > 0) neighbors.add(new Node(node.x, node.y - 1));
        if (node.y < m_Height - 1) neighbors.add(new Node(node.x, node.y + 1));
        return neighbors;
    }

    private boolean isValidTile(int x, int y) {
        return m_Tiles[x][y] == TileType.NONE || m_Tiles[x][y] == TileType.FLOOR;
    }

    private List<Node> reconstructPath(Node current) {
        List<Node> path = new ArrayList<>();
        while (current != null) {
            path.add(current);
            current = current.parent; // Backtrack to construct the path
        }
        return path; // Return the path in reverse order
    }

    private void setWalls() {
        for (int x = 0; x < m_Width; ++x) {
            for (int y = 0; y < m_Height; ++y) {
                if (m_Tiles[x][y] == TileType.NONE) {
                    // Check adjacent tiles to determine if this tile should be a wall
                    if (isAdjacentToFloor(x, y)) {
                        m_Tiles[x][y] = TileType.WALL; // Set as wall
                    }
                }
            }
        }
    }

    private boolean isAdjacentToFloor(int x, int y) {
        // Check 4-directional neighbors for floor tiles
        return (x > 0 && m_Tiles[x - 1][y] == TileType.FLOOR) ||
            (x < m_Width - 1 && m_Tiles[x + 1][y] == TileType.FLOOR) ||
            (y > 0 && m_Tiles[x][y - 1] == TileType.FLOOR) ||
            (y < m_Height - 1 && m_Tiles[x][y + 1] == TileType.FLOOR);
    }

    // Node class to hold x, y coordinates and the parent for path reconstruction
    private static class Node {
        int x, y;
        Node parent;

        Node(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // Override equals for proper comparison in lists
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Node other)) return false;
            return x == other.x && y == other.y;
        }
    }

    // Point class to represent a vertex
    private static class Point {
        int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // Room class to define room properties and behavior
    private static class Room {
        static final int MAX_CONNECTIONS = 2;

        int x, y, width, height;
        int connections;

        Room(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.connections = 0;
        }

        boolean intersects(Room other) {
            return x < other.x + other.width && x + width > other.x &&
                y < other.y + other.height && y + height > other.y;
        }

        // Custom carving method for polygon shapes
        void carvePolygon(TileType[][] tiles, List<Point> vertices) {
            for (int x = this.x; x < this.x + this.width; x++) {
                for (int y = this.y; y < this.y + this.height; y++) {
                    // Use a point-in-polygon algorithm to determine if the tile should be carved
                    if (isPointInPolygon(x, y, vertices)) {
                        tiles[x][y] = TileType.FLOOR; // Carve out the floor
                    }
                }
            }
        }

        // Implement a point-in-polygon algorithm
        private boolean isPointInPolygon(int x, int y, List<Point> vertices) {
            boolean result = false;
            int j = vertices.size() - 1;

            for (int i = 0; i < vertices.size(); i++) {
                if (vertices.get(i).y < y && vertices.get(j).y >= y || vertices.get(j).y < y && vertices.get(i).y >= y) {
                    if (vertices.get(i).x + (y - vertices.get(i).y) / (vertices.get(j).y - vertices.get(i).y) * (vertices.get(j).x - vertices.get(i).x) < x) {
                        result = !result;
                    }
                }
                j = i;
            }
            return result;
        }
    }

    // LRoom class for L-shaped rooms
    private static class LRoom extends Room {
        int width2, height2; // Second part dimensions
        int orientation; // 0: left, 1: right, 2: up, 3: down

        LRoom(int x, int y, int width1, int height1, int width2, int height2, int orientation) {
            super(x, y, width1, height1);
            this.width2 = width2;
            this.height2 = height2;
            this.orientation = orientation;
        }

        void carveLShape(TileType[][] tiles) {
            // Carve the main part of the L shape
            for (int x = this.x; x < this.x + this.width; x++) {
                for (int y = this.y; y < this.y + this.height; y++) {
                    tiles[x][y] = TileType.FLOOR; // Carve out the floor
                }
            }

            // Carve the second part based on orientation
            switch (orientation) {
                case 0: // Left L
                    for (int x = this.x; x < this.x + width2; x++) {
                        for (int y = this.y + this.height; y < this.y + this.height + height2; y++) {
                            tiles[x][y] = TileType.FLOOR; // Carve out the second part
                        }
                    }
                    break;
                case 1: // Right L
                    for (int x = this.x + this.height; x < this.x + this.width + width2; x++) {
                        for (int y = this.y; y < this.y + this.height + height2; y++) {
                            tiles[x][y] = TileType.FLOOR; // Carve out the second part
                        }
                    }
                    break;
                case 2: // Up L
                    for (int x = this.x; x < this.x + this.width + width2; x++) {
                        for (int y = this.y + this.height; y < this.y + this.height + height2; y++) {
                            tiles[x][y] = TileType.FLOOR; // Carve out the second part
                        }
                    }
                    break;
                case 3: // Down L
                    for (int x = this.x; x < this.x + this.width; x++) {
                        for (int y = this.y; y < this.y + this.height + height2; y++) {
                            tiles[x][y] = TileType.FLOOR; // Carve out the second part
                        }
                    }
                    break;
            }
        }
    }

    // PolygonRoom class extending Room
    private static class PolygonRoom extends Room {
        List<Point> vertices;

        PolygonRoom(int x, int y, List<Point> vertices) {
            super(x, y, 0, 0); // Width and height will be managed differently for polygons
            this.vertices = vertices;
            updateBounds(); // Calculate bounds for the polygon
        }

        // Method to update the room bounds based on vertices
        private void updateBounds() {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

            for (Point p : vertices) {
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }

            // Set the bounds to be the actual dimensions of the polygon
            this.x = minX;
            this.y = minY;
            this.width = maxX - minX;
            this.height = maxY - minY;
        }
    }
}
