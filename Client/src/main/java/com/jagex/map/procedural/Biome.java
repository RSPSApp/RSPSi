package com.jagex.map.procedural;

import com.jagex.cache.def.ObjectDefinition;
import com.jagex.cache.loader.object.ObjectDefinitionLoader;
import com.jagex.io.Buffer;
import com.rspsi.misc.ArrayUtils;

public enum Biome {

    WATER(57, 0, 0, 0, 0, 19),

    WILDERNESS(57, 0, 336, 4.57, 1.0, 19),

    DESERT(5, 0, 300, 6.55, 2.0),

    SWAMP(24, 0, 336, 4.57, 1.87, 7),

    FREMMINIK_LAKES(57, 0, 336, 4.57, 1.87, 6, new int[] { 1282, 1286 });

    public byte[] underlays;
    public int minHeight;
    public int maxHeight;
    public double frequency;
    public double peaks;
    public int[] trees;
    public byte waterOverlay;
    public HouseType houseType = HouseType.VARROCK;

    Biome(int underlay, int minHeight, int maxHeight, double frequency, double peaks) {
        this.underlays = new byte[] { (byte) underlay };
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.frequency = frequency;
        this.peaks = peaks;
        this.waterOverlay = (byte) 6;
        this.houseType = HouseType.NORMAL;
    }

    Biome(int underlay, int minHeight, int maxHeight, double frequency, double peaks, int waterOverlay) {
        this.underlays = new byte[] { (byte) underlay };
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.frequency = frequency;
        this.peaks = peaks;
        this.waterOverlay = (byte) waterOverlay;
    }

    Biome(int underlay, int minHeight, int maxHeight, double frequency, double peaks, int waterOverlay, int[] trees) {
        this.underlays = new byte[] { (byte) underlay };
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.frequency = frequency;
        this.peaks = peaks;
        this.waterOverlay = (byte) waterOverlay;
        this.trees = trees;
    }

//    Biome(int underlay, int minHeight, int maxHeight, double frequency, double peaks, int waterOverlay) {
//        //this.underlays = ArrayUtils.integersToBytes(underlays);
//        this.minHeight = minHeight;
//        this.maxHeight = maxHeight;
//        this.frequency = frequency;
//        this.peaks = peaks;
//        this.waterOverlay = (byte) waterOverlay;
//        this.trees = trees;
//    }

    /**
     * A method returning all possible object ids spawned by this Biome.
     * This is needed for Chunk loading and preloading object models.
     *
     * @return
     */
    public int[] getObjectIds() {
        return ArrayUtils.merge(trees, houseType.getObjectIds());
    };

    /**
     * A method determining whether objects have been loaded into memory or not for this Biome.
     *
     * @return
     */
    public boolean objectsReady() {
        int[] allObjectIds = getObjectIds();

        for (int i = 0; i < allObjectIds.length; i++) {
            int id = allObjectIds[i];
            ObjectDefinition definition = ObjectDefinitionLoader.lookup(id);
            if (definition == null)
                continue;

            if (!definition.ready()) {
                return false;
            }
        }

        return true;
    }
};