/**
 * JRelStarflight realizes the special relativistic and warp flight 
 * through the Hipparcos star field.
 * 
 * Copyright (c) 2011, 2017, Thomas Mueller
 * 
 * @author   Thomas Mueller
 * @version  1.1
 */
package tauzero7.java.relavis.relstar;

public final class Defs {
    public static int    VERTEX_MAG_ARRAY   = 10;
    public static int    VERTEX_TEMP_ARRAY  = 11;

    public static double PINHOLECAM_FOVY    = 50.0;

    public static double timeStep           = 0.4;

    public static double betaMax            = 0.99;
    public static double betaMaxWarp        = 9.0;
    public static double movStepY           = 0.003;

    public static String BIN_NAME_HIPPARCOS = "data/hip.bin";
    public static String BIN_NAME_PSITEMP   = "data/psitemp.bin";
    public static String BIN_NAME_SIGMA     = "data/sigma.bin";
    public static String BIN_NAME_WARP      = "data/warpDistort_max9.bin";
}