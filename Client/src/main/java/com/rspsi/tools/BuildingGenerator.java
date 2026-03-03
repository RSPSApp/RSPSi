package com.rspsi.tools;

import com.jagex.Client;
import com.jagex.map.SceneGraph;
import com.jagex.map.procedural.HouseType;
import com.jagex.map.tile.SceneTile;
import com.jagex.util.BitFlag;
import com.rspsi.options.Options;
import javafx.application.Platform;
import javafx.scene.SnapshotParameters;
import org.major.map.RenderFlags;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BuildingGenerator {



    // Roof object types
    private static int roofEdgeSide = 18;
    private static int roofEdgeCorner = 21;
    private static int roofTopCorner = 16;
    private static int roofTopFlat = 17;
    private static int roofTopSide = 12;
    private static int roofTopCornerInset = 14;
    private static int roofEdgeCornerInset = 20;

    public static void generateBuilding(int numFloors) throws Exception {
        Client client = Client.getSingleton();
        if(client.loadState == Client.LoadState.ACTIVE) {
            if(Options.currentHeight.get() != 0) {
                throw new Exception("Invalid height");
            }

            int oldHeight = Options.tileHeightLevel.get();
            BitFlag oldFlag = Options.tileFlags.get();

            Options.allHeightsVisible.set(true);

            int lowerZ = Options.currentHeight.get();
            List<SceneTile> selectedTiles = client.sceneGraph.getSelectedTiles();

            List<SceneTile> tilesAbove = selectedTiles.stream().map(tile -> client.sceneGraph.tiles[tile.plane + 1][tile.positionX][tile.positionY]).collect(Collectors.toList());
            List<SceneTile> tilesAround = selectedTiles
                    .stream()
                    .flatMap(tile -> IntStream
                            .rangeClosed(-2, 2)
                            .boxed()
                            .flatMap(x -> IntStream.rangeClosed(-2, 2).mapToObj(y -> client.sceneGraph.tiles[tile.plane][tile.positionX + x][tile.positionY + y])))
                    .filter(tile -> !tilesAbove.contains(tile))
                    .collect(Collectors.toList());

            double averageTileHeight = tilesAround.stream().mapToInt(tile -> -client.mapRegion.tileHeights[lowerZ][tile.positionX][tile.positionY]).average().getAsDouble();
            int minimumX = tilesAround.stream().mapToInt(tile -> tile.positionX).min().getAsInt();
            int minimumY = tilesAround.stream().mapToInt(tile -> tile.positionY).min().getAsInt();
            int maximumX = tilesAround.stream().mapToInt(tile -> tile.positionX).max().getAsInt();
            int maximumY = tilesAround.stream().mapToInt(tile -> tile.positionY).max().getAsInt();


            Options.tileHeightLevel.set((int) averageTileHeight);

            System.out.println(tilesAround.size());
            System.out.println("Average tile height is " + Options.tileHeightLevel.get());
            client.sceneGraph.setTileHeights(selectedTiles, true);
            client.sceneGraph.setTileHeights(tilesAround, true);


            HouseType houseType = ThreadLocalRandom.current().nextInt(0,1) == 1 ? HouseType.NORMAL : HouseType.VARROCK;
            // Put the door facing the camera

            int cameraX = client.sceneGraph.absoluteCameraX,
                    cameraY = client.sceneGraph.absoluteCameraY;

            // By default, just have the door facing south
            int doorOrientation = 1,
            doorCoord = -1 /* No door */;

            if (cameraY > maximumY-1) {
                // Camera is north of house
                doorOrientation = 3;
            } else if (cameraX > maximumX-1) {
                // Camera is east of the house
                doorOrientation = 0;
            } else if (cameraX < minimumX+1) {
                // Camera is west of house
                doorOrientation = 2;
            }

            if (doorOrientation == 0 || doorOrientation == 2) {
                // Door is either on north or south wall, pick a random Y coord
                doorCoord = ThreadLocalRandom.current().nextInt(minimumY+3,maximumY-3);
            } else {
                // Door is either on west or east wall, pick a random X coord
                doorCoord = ThreadLocalRandom.current().nextInt(minimumX+3,maximumX-3);
            }

            for (int fl = 0; fl < numFloors; fl++) {
                boolean isTopFloor = (fl == numFloors-1);

                for (int i = minimumY + 2; i < maximumY - 1; i++) {
                    // West wall
                    if (fl == 0 && doorOrientation == 2 && i == doorCoord) {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.doorType, minimumX + 1, i, fl, 0, 2, false);
                    } else {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallId, minimumX + 1, i, fl, 0, 2, false);
                    }

                    // East wall
                    if (fl == 0 && doorOrientation == 0 && i == doorCoord) {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.doorType, maximumX - 1, i, fl, 0, 0, false);
                    } else {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallId, maximumX - 1, i, fl, 0, 0, false);
                    }

                    if (isTopFloor) {
                        // West roof edge
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, minimumX + 1, i, fl + 1, roofEdgeSide, 2, false);
                        // Top roof edge (slant)
                        if (i >= minimumY + 3 && i <= maximumY - 3) {
                            client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, minimumX + 2, i, fl + 1, roofTopSide, 2, false);
                        }

                        // East roof edge
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, maximumX - 1, i, fl + 1, roofEdgeSide, 0, false);
                        // Top roof edge (slant)
                        if (i >= minimumY + 3 && i <= maximumY - 3) {
                            client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, maximumX - 2, i, fl + 1, roofTopSide, 0, false);
                        }
                    }
                }


                for (int i = minimumX + 2; i < maximumX - 1; i++) {
                    // South wall
                    if (fl == 0 && doorOrientation == 1 && i == doorCoord) {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.doorType, i, minimumY + 1, fl, 0, 1, false);
                    } else {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallId, i, minimumY + 1, fl, 0, 1, false);
                    }

                    // North wall
                    if (fl == 0 && doorOrientation == 3 && i == doorCoord) {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.doorType, i, maximumY - 1, fl, 0, 3, false);
                    } else {
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallId, i, maximumY - 1, fl, 0, 3, false);
                    }

                    if (isTopFloor) {
                        // North roof edge
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, i, maximumY - 1, fl + 1, roofEdgeSide, 3, false);
                        // Top roof edge (slant)
                        if (i >= minimumX + 3 && i <= maximumX - 3) {
                            client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, i, maximumY - 2, fl + 1, roofTopSide, 3, false);
                        }

                        // South roof edge
                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, i, minimumY + 1, fl + 1, roofEdgeSide, 1, false);
                        // Top roof edge (slant)
                        if (i >= minimumX + 3 && i <= maximumX - 3) {
                            client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, i, minimumY + 2, fl + 1, roofTopSide, 1, false);
                        }
                    }

                }

                // Roof center tops
                if (isTopFloor) {
                    // South east roof corner
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, minimumX + 1, minimumY + 1, fl + 1, roofEdgeCorner, 1, false);
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, minimumX + 2, minimumY + 2, fl + 1, roofTopCorner, 1, false);
                    // North east roof corner
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, minimumX + 1, maximumY - 1, fl + 1, roofEdgeCorner, 2, false);
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, minimumX + 2, maximumY - 2, fl + 1, roofTopCorner, 2, false);
                    // North west roof corner
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, maximumX - 1, maximumY - 1, fl + 1, roofEdgeCorner, 3, false);
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, maximumX - 2, maximumY - 2, fl + 1, roofTopCorner, 3, false);
                    // South west roof corner
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofEdgeType, maximumX - 1, minimumY + 1, fl + 1, roofEdgeCorner, 0, false);
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, maximumX - 2, minimumY + 2, fl + 1, roofTopCorner, 0, false);

                    for (SceneTile sceneTile : selectedTiles) {
                        if (sceneTile.positionX < minimumX + 3 || sceneTile.positionX > maximumX - 3) {
                            continue;
                        }
                        if (sceneTile.positionY < minimumY + 3 || sceneTile.positionY > maximumY - 3) {
                            continue;
                        }

                        client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.roofTopType, sceneTile.positionX, sceneTile.positionY, fl + 1, roofTopFlat, 0, false);
                    }
                }

                // Wall corners
                if (houseType.wallType.wallCornerId != -1) {
                    // South east
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallCornerId, minimumX + 1, minimumY + 1, fl, houseType.wallType.wallCornerType, 1, false);

                    // North east
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallCornerId, minimumX + 1, maximumY - 1, fl, houseType.wallType.wallCornerType, 2, false);

                    // North west
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallCornerId, maximumX - 1, maximumY - 1, fl, houseType.wallType.wallCornerType, 3, false);

                    // South west
                    client.mapRegion.spawnObjectToWorld(client.sceneGraph, houseType.wallType.wallCornerId, maximumX - 1, minimumY + 1, fl, houseType.wallType.wallCornerType, 0, false);
                }
            }

            client.mapRegion.updateTiles();
            //SceneGraph.commitChanges();


            // Set remove roof flags for all tiles inside house
            BitFlag removeRoofFlag = new BitFlag();
            removeRoofFlag.flag(RenderFlags.FORCE_LOWEST_PLANE);
            Options.tileFlags.set(removeRoofFlag);
            client.sceneGraph.setTileFlags(selectedTiles);

            // Set the ground flooring
            Options.overlayPaintShapeId.set(1);
            Options.overlayPaintId.set(houseType.floorUnderlay);
            client.sceneGraph.setTileOverlays(selectedTiles, numFloors);

            Options.tileHeightLevel.set(oldHeight);
            Options.tileFlags.set(oldFlag);

            client.sceneGraph.tileQueue.clear();

        }
    }
}
