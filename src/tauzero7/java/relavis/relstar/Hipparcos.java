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


import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.Vector;

import com.jogamp.common.nio.Buffers;

public class Hipparcos {
    private final String    filename = Defs.BIN_NAME_HIPPARCOS;
    private final int       num_rows = 118218;
    private final int       num_cols = 7;
    private final int       size     = num_rows * num_cols;

    private Vector<Integer> hipID    = null;
    private Vector<Double>  ra       = null;
    private Vector<Double>  de       = null;
    private Vector<Double>  plx      = null;
    private Vector<Double>  Vmag     = null;
    private Vector<Double>  bmv      = null;
    private Vector<Double>  temp     = null;

    private int             numStars = 0;

    /**
     * Hipparcos.
     * 
     */
    Hipparcos() {
        readBinaryFile();
    }

    /**
     * Get number of stars.
     * 
     * @return number of stars.
     */
    public int getNumStars() {
        return numStars;
    }

    /**
     * Get position data of stars.
     * 
     * @return 3d vertices float buffer.
     */
    public FloatBuffer getVertices() {
        // FloatBuffer verts = FloatBuffer.allocate(ra.size()*3);
        FloatBuffer verts = Buffers.newDirectFloatBuffer(ra.size() * 3);
        for (int i = 0; i < numStars; i++) {
            verts.put(3 * i + 0, plx.elementAt(i).floatValue());
            verts.put(3 * i + 1, ra.elementAt(i).floatValue() * (float) Math.PI
                    / 180.0f);
            verts.put(3 * i + 2, de.elementAt(i).floatValue() * (float) Math.PI
                    / 180.0f);
        }
        return verts;
    }

    /**
     * Get magnitudes of stars.
     * 
     * @return magnitude float buffer.
     */
    public FloatBuffer getMagnitudes() {
        // FloatBuffer magBuf = FloatBuffer.allocate(Vmag.size());
        FloatBuffer magBuf = Buffers.newDirectFloatBuffer(Vmag.size());
        for (int i = 0; i < numStars; i++) {
            float absMag = Vmag.elementAt(i).floatValue()
                    + (float) (5.0 * Math.log(1000.0 / plx.elementAt(i))
                            / Math.log(10.0) - 10.0);
            magBuf.put(i, absMag);
        }
        return magBuf;
    }

    /**
     * Get log of temperature of stars.
     * 
     * @return temperature float buffer.
     */
    public FloatBuffer getTemps() {
        // FloatBuffer tbuf = FloatBuffer.allocate(temp.size());
        FloatBuffer tbuf = Buffers.newDirectFloatBuffer(temp.size());
        for (int i = 0; i < numStars; i++) {
            tbuf.put(i, temp.elementAt(i).floatValue());
        }
        return tbuf;
    }

    /**
     * Read Hipparcos file in binary form.
     * 
     */
    private void readBinaryFile() {
        FileInputStream fs = null;
        DataInputStream in = null;

        try {
            fs = new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(1);
        }

        /*
         * try { int av = fs.available(); System.err.println("Size: " + av); }
         * catch (IOException e) { }
         */

        int byteSize = size * Double.SIZE;
        byte[] array = new byte[byteSize];

        try {
            in = new DataInputStream(fs);
            in.read(array, 0, byteSize);
        } catch (IOException e) {
            System.err.println("Cannot read buffer");
        }

        hipID = new Vector<Integer>();
        ra = new Vector<Double>();
        de = new Vector<Double>();
        plx = new Vector<Double>();
        Vmag = new Vector<Double>();
        bmv = new Vector<Double>();
        temp = new Vector<Double>();

        int offset = 0;
        double[] rowData = new double[num_cols];

        numStars = 0;
        for (int row = 0; row < num_rows; row++) {
            convertRow(array, offset, rowData);
            offset += num_cols * 8;
            // otherwise the parallax is negative
            if (rowData[6] > 0.0) {
                hipID.add((int) rowData[0]);
                ra.add(rowData[1]);
                de.add(rowData[2]);
                plx.add(rowData[3]);
                Vmag.add(rowData[4]);
                bmv.add(rowData[5]);
                temp.add(rowData[6]);
                numStars++;
            }
        }

        try {
            in.close();
            fs.close();
        } catch (IOException e) {
        }
    }

    /**
     * Convert data
     * 
     * @param a
     *            : byte array.
     * @param off
     *            : offset.
     * @param data
     *            : double data array.
     */
    private void convertRow(byte[] a, int off, double[] data) {
        int offset = off;
        for (int i = 0; i < num_cols; i++) {
            data[i] = byteArrayToDouble(a, offset);
            offset += 8;
        }
    }

    /**
     * Convert byte array to double.
     * 
     * @param b
     *            : byte array.
     * @param offset
     *            : offset in byte array.
     * @return double value.
     */
    private double byteArrayToDouble(byte[] b, int offset) {
        long accum = 0;
        for (int shiftBy = 0, i = 0; shiftBy < 64; i++, shiftBy += 8) {
            // must cast to long or the shift would be done modulo 32
            accum |= ((long) (b[offset + i] & 0xff)) << shiftBy;
        }
        return Double.longBitsToDouble(accum);
    }
}
