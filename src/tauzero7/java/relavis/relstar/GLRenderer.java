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

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.glu.GLU;

import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

public class GLRenderer implements GLEventListener {

    private int         width;
    private int         height;

    private GLU         glu             = new GLU();
    private Shader      shader          = null;

    private Hipparcos   hipCat          = null;
    private Texture     mTempTex        = null;
    private Texture     mSigmaTex       = null;
    private Texture     mWarpTex        = null;

    private FloatBuffer mVertices       = null;
    private FloatBuffer mAbsMag         = null;
    private FloatBuffer mTemps          = null;

    int[]               mVBO            = new int[1];

    double[]            rotAngle        = { Math.PI, 0.0 };
    double[]            oldAngle        = { Math.PI, 0.0 };
    float[]             rotation_matrix = { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };

    double              beta            = 0.0;
    double              oldBeta         = 0.0;
    float[]             tetrad_matrix   = { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f };

    int                 mSpacetime      = 0;                             // Minkowski=0,
                                                                          // Warp=1
    int                 mCamera         = 0;                             // 4pi=0,
                                                                          // Pinhole=1

    double              mCurrPos        = 0;

    GLRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        setRotMatrix(rotAngle[1], rotAngle[0]);
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        System.out.println("Graphics board details:");
        System.out.printf("\tVendor         : %s\n",
                gl.glGetString(GL2.GL_VENDOR));
        System.out.printf("\tGPU            : %s\n",
                gl.glGetString(GL2.GL_RENDERER));
        System.out.printf("\tOpenGL version : %s\n",
                gl.glGetString(GL2.GL_VERSION));
        System.out.printf("\tGLSL version   : %s\n",
                gl.glGetString(GL2.GL_SHADING_LANGUAGE_VERSION));
        
        glu = new GLU();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, width, 0.0, height);

        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);

        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE);
        gl.glShadeModel(GL2.GL_SMOOTH);
        gl.glEnable(GL2.GL_POINT_SPRITE);

        shader = new Shader(gl);
        shader.setShaders(gl);
        shader.release();

        hipCat = new Hipparcos();
        loadPsiTempTex(gl);
        loadSigmaTex(gl);
        loadWarpTex(gl);

        System.err.printf("Load %s ... %d\n", Defs.BIN_NAME_HIPPARCOS,
                hipCat.getNumStars());
        mVertices = hipCat.getVertices();
        mAbsMag = hipCat.getMagnitudes();
        mTemps = hipCat.getTemps();
        // System.err.printf("%f %f %f\n",mVertices.get(0),mVertices.get(1),mVertices.get(2));
        createVBO(gl);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        // TODO Auto-generated method stub

    }

    @Override
    public void display(GLAutoDrawable drawable) {
        render(drawable);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width,
            int height) {
        this.width = width;
        this.height = height;

        GL2 gl = drawable.getGL().getGL2();
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, width, 0.0, height);
    }

    public void render(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        gl.glColor3d(1, 1, 1);

        gl.glActiveTexture(GL.GL_TEXTURE0);
        mTempTex.bind(gl);
        mTempTex.enable(gl);

        gl.glActiveTexture(GL.GL_TEXTURE1);
        mSigmaTex.bind(gl);
        mSigmaTex.enable(gl);

        gl.glActiveTexture(GL.GL_TEXTURE2);
        mWarpTex.bind(gl);
        mWarpTex.enable(gl);

        gl.glPointSize(1.0f);
        shader.bind();
        gl.glUniform2f(shader.location("wSize"), (float) width, (float) height);
        gl.glUniform1i(shader.location("texPsiTemp"), 0);
        gl.glUniform1i(shader.location("texSigma"), 1);
        gl.glUniform1i(shader.location("texWarp"), 2);
        gl.glUniform1i(shader.location("spacetime"), mSpacetime);
        if (mSpacetime == 0) {
            gl.glUniform1f(shader.location("beta"), (float) beta);
        } else {
            gl.glUniform1f(shader.location("beta"),
                    (float) (Math.log(beta + 1) / Math.log(10)));
        }
        gl.glUniform1f(shader.location("curr_pos"), (float) mCurrPos);
        gl.glUniform1f(shader.location("def_gamma"), 1.5f);
        gl.glUniform1f(shader.location("def_s0"), 5.0f);
        gl.glUniform1i(shader.location("camera"), mCamera);
        gl.glUniform1f(shader.location("fovYh"),
                (float) Math.toRadians(Defs.PINHOLECAM_FOVY * 0.5));
        gl.glUniformMatrix4fv(shader.location("rotmat"), 1, false,
                rotation_matrix, 0);
        gl.glUniformMatrix4fv(shader.location("tetrad"), 1, false,
                tetrad_matrix, 0);

        drawPoints(gl);

        shader.release();

        mWarpTex.disable(gl);
        mSigmaTex.disable(gl);
        mTempTex.disable(gl);
    }

    private void drawPoints(GL2 gl) {

        gl.glVertexAttribPointer(Defs.VERTEX_MAG_ARRAY, 1, GL.GL_FLOAT, false,
                0, mAbsMag);
        gl.glEnableVertexAttribArray(Defs.VERTEX_MAG_ARRAY);
        gl.glVertexAttribPointer(Defs.VERTEX_TEMP_ARRAY, 1, GL.GL_FLOAT, false,
                0, mTemps);
        gl.glEnableVertexAttribArray(Defs.VERTEX_TEMP_ARRAY);

        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, mVBO[0]);
        gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);

        gl.glEnable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);
        gl.glTexEnvi(GL2.GL_POINT_SPRITE, GL2.GL_COORD_REPLACE, GL.GL_TRUE);
        gl.glDrawArrays(GL.GL_POINTS, 0, hipCat.getNumStars());
        gl.glDisable(GL2.GL_VERTEX_PROGRAM_POINT_SIZE);

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);

        gl.glDisableVertexAttribArray(Defs.VERTEX_MAG_ARRAY);
        gl.glDisableVertexAttribArray(Defs.VERTEX_TEMP_ARRAY);
    }

    private void loadPsiTempTex(GL2 gl) {
        FileInputStream fs = null;
        DataInputStream in = null;

        try {
            fs = new FileInputStream(Defs.BIN_NAME_PSITEMP);
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(1);
        }

        int tex_width = 0;
        int tex_height = 0;
        int num = 0;
        int byteSize = 0;
        byte[] array = null;

        try {
            byte[] head_code = new byte[4];
            byte[] head_size = new byte[13];

            in = new DataInputStream(fs);
            in.read(head_code, 0, 4);
            in.read(head_size, 0, 12);

            // System.err.println(new String(head_code));
            tex_width = byteArrayToInt(head_size, 0);
            tex_height = byteArrayToInt(head_size, 4);
            num = byteArrayToInt(head_size, 8);
            System.err.printf("Load %s ... %d x %d x %d\n",
                    Defs.BIN_NAME_PSITEMP, tex_width, tex_height, num);
            if (tex_height != 1 || num != 1) {
                System.err.println("psitemp size is wrong!");
                System.exit(1);
            }
            byteSize = tex_width * tex_height * num * Float.SIZE;
            array = new byte[byteSize];
            in.read(array, 0, byteSize);
        } catch (IOException e) {
            System.err.println("Cannot read buffer");
        }

        FloatBuffer tBuf = FloatBuffer.allocate(tex_width);
        int offset = 0;
        for (int n = 0; n < tex_width; n++) {
            tBuf.put(n, byteArrayToFloat(array, offset));
            offset += 4;
            // System.err.println(tBuf.get(n));
        }

        GLProfile glp = GLProfile.getDefault();
        TextureData texData = new TextureData(glp, GL2.GL_ALPHA32F, tex_width,
                tex_height, 0, GL.GL_ALPHA, GL.GL_FLOAT, false, false, false,
                tBuf, null);
        mTempTex = TextureIO.newTexture(texData);
        mTempTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        mTempTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
    }

    /**
     * Load sigma texture.
     * 
     * @param gl
     *            : opengl.
     */
    private void loadSigmaTex(GL2 gl) {
        FileInputStream fs = null;
        DataInputStream in = null;

        try {
            fs = new FileInputStream(Defs.BIN_NAME_SIGMA);
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(1);
        }

        int tex_width = 0;
        int tex_height = 0;
        int num = 0;
        int byteSize = 0;
        byte[] array = null;

        try {
            byte[] head_code = new byte[4];
            byte[] head_size = new byte[13];

            in = new DataInputStream(fs);
            in.read(head_code, 0, 4);
            in.read(head_size, 0, 12);
            // System.err.println(new String(head_code));
            tex_width = byteArrayToInt(head_size, 0);
            tex_height = byteArrayToInt(head_size, 4);
            num = byteArrayToInt(head_size, 8);
            System.err.printf("Load %s ... %d x %d x %d\n",
                    Defs.BIN_NAME_SIGMA, tex_width, tex_height, num);

            byteSize = tex_width * tex_height * num * Float.SIZE;
            array = new byte[byteSize];
            in.read(array, 0, byteSize);
        } catch (IOException e) {
            System.err.println("Cannot read buffer");
        }

        FloatBuffer tBuf = FloatBuffer.allocate(tex_width * tex_height * num);
        int offset = 0;
        for (int n = 0; n < tex_width * tex_height * num; n++) {
            tBuf.put(n, byteArrayToFloat(array, offset));
            offset += 4;
            // System.err.println(tBuf.get(n));
        }

        GLProfile glp = GLProfile.getDefault();
        TextureData texData = new TextureData(glp, GL2.GL_RGB32F, tex_width,
                tex_height, 0, GL.GL_RGB, GL.GL_FLOAT, false, false, false,
                tBuf, null);
        mSigmaTex = TextureIO.newTexture(texData);
        mSigmaTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        mSigmaTex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
    }

    /**
     * Load distortion texture for warp metric.
     * 
     */
    private void loadWarpTex(GL2 gl) {
        FileInputStream fs = null;
        DataInputStream in = null;

        try {
            fs = new FileInputStream(Defs.BIN_NAME_WARP);
        } catch (FileNotFoundException e) {
            System.err.println("FileNotFoundException: " + e.getMessage());
            System.exit(1);
        }

        int tex_width = 0;
        int tex_height = 0;
        int num = 0;
        int byteSize = 0;
        byte[] array = null;

        try {
            byte[] head_code = new byte[4];
            byte[] head_size = new byte[13];

            in = new DataInputStream(fs);
            in.read(head_code, 0, 4);
            in.read(head_size, 0, 12);

            // System.err.println(new String(head_code));
            tex_width = byteArrayToInt(head_size, 0);
            tex_height = byteArrayToInt(head_size, 4);
            num = byteArrayToInt(head_size, 8);
            System.err.printf("Load %s ... %d x %d x %d\n", Defs.BIN_NAME_WARP,
                    tex_width, tex_height, num);

            byteSize = tex_width * tex_height * num * Float.SIZE;
            array = new byte[byteSize];
            in.read(array, 0, byteSize);
        } catch (IOException e) {
            System.err.println("Cannot read buffer");
        }

        FloatBuffer tBuf = FloatBuffer.allocate(tex_width * tex_height * num);
        int offset = 0;
        for (int n = 0; n < tex_width * tex_height * num; n++) {
            tBuf.put(n, byteArrayToFloat(array, offset));
            offset += 4;
            // System.err.println(tBuf.get(n));
            // if (n==10) System.exit(1);
        }

        GLProfile glp = GLProfile.getDefault();
        TextureData texData = new TextureData(glp, GL2.GL_RGB32F, tex_width,
                tex_height, 0, GL.GL_RGB, GL.GL_FLOAT, false, false, false,
                tBuf, null);
        mWarpTex = TextureIO.newTexture(texData);
        mWarpTex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP);
        mWarpTex.setTexParameteri(gl, GL.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP);
        mWarpTex.setTexParameteri(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        mWarpTex.setTexParameteri(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    }

    /**
     * Convert byte array into float value.
     * 
     * @param b
     *            : byte array.
     * @param offset
     *            : offset for array access.
     * @return float value.
     */
    private float byteArrayToFloat(byte[] b, int offset) {
        int accum = 0;
        for (int shiftBy = 0, i = 0; shiftBy < 32; i++, shiftBy += 8) {
            accum |= ((int) (b[offset + i] & 0xff)) << shiftBy;
        }
        return Float.intBitsToFloat(accum);
    }

    /**
     * Convert byte array into integer value.
     * 
     * @param b
     *            : byte array.
     * @param offset
     *            : offset for array access.
     * @return integer value.
     */
    private int byteArrayToInt(byte[] b, int offset) {
        int accum = 0;
        for (int shiftBy = 0, i = 0; shiftBy < 32; i++, shiftBy += 8) {
            accum |= ((int) (b[offset + i] & 0xff)) << shiftBy;
        }
        return accum;
    }

    /**
     * Create vertex buffer object.
     * 
     * @param gl
     *            : opengl.
     */
    private void createVBO(GL gl) {
        gl.glGenBuffers(1, mVBO, 0);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, mVBO[0]);
        gl.glBufferData(GL.GL_ARRAY_BUFFER, mVertices.capacity() * Float.SIZE,
                mVertices, GL.GL_STATIC_DRAW);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
    }

    /**
     * Set rotation matrix.
     * 
     * @param alpha
     *            : rotation angle.
     * @param beta
     *            : rotation angle.
     */
    public void setRotMatrix(double alpha, double beta) {
        rotation_matrix[0] = (float) (Math.cos(alpha) * Math.cos(beta));
        rotation_matrix[1] = (float) (Math.cos(alpha) * Math.sin(beta));
        rotation_matrix[2] = (float) (Math.sin(alpha));
        rotation_matrix[4] = -(float) (Math.sin(beta));
        rotation_matrix[5] = (float) (Math.cos(beta));
        rotation_matrix[6] = 0.0f;
        rotation_matrix[8] = -(float) (Math.sin(alpha) * Math.cos(beta));
        rotation_matrix[9] = -(float) (Math.sin(alpha) * Math.sin(beta));
        rotation_matrix[10] = (float) Math.cos(alpha);
    }

    /**
     * Set rotation matrix with mouse.
     * 
     * @param x
     *            : mouse position x.
     * @param y
     *            : mouse position y.
     */
    public void setMouseRot(int x, int y) {
        double b = rotAngle[0] = oldAngle[0] - x / (double) width * 2.0
                * Math.PI;
        double a = rotAngle[1] = oldAngle[1] + y / (double) height * Math.PI;
        setRotMatrix(a, b);
    }

    public void setMouseRelease() {
        oldAngle[0] = rotAngle[0];
        oldAngle[1] = rotAngle[1];
        oldBeta = beta;
    }

    public void setMouseMotion(int y) {
        beta = oldBeta + y * Defs.movStepY;
        if (mSpacetime == 0) {
            if (beta > Defs.betaMax) {
                beta = Defs.betaMax;
            }
            if (beta < -Defs.betaMax) {
                beta = -Defs.betaMax;
            }
        } else {
            if (beta > Defs.betaMaxWarp) {
                beta = Defs.betaMaxWarp;
            }
            if (beta < 0.0) {
                beta = 0.0;
            }
        }

        double gamma = 1.0 / Math.sqrt(1.0 - beta * beta);

        tetrad_matrix[0] = (float) gamma;
        tetrad_matrix[1] = -(float) (gamma * beta);
        tetrad_matrix[4] = -(float) (gamma * beta);
        tetrad_matrix[5] = (float) gamma;
    }

    public double setMotion(double beta) {
        if (mSpacetime == 0) {
            if (Math.abs(beta) > Defs.betaMax) {
                return this.beta;
            }
            this.beta = this.oldBeta = beta;
            double gamma = 1.0 / Math.sqrt(1.0 - beta * beta);

            tetrad_matrix[0] = (float) gamma;
            tetrad_matrix[1] = -(float) (gamma * beta);
            tetrad_matrix[4] = -(float) (gamma * beta);
            tetrad_matrix[5] = (float) gamma;
        } else {
            if (beta > Defs.betaMaxWarp) {
                return this.beta;
            } else if (beta < 0.0) {
                this.beta = 0.0;
            } else {
                this.beta = this.oldBeta = beta;
            }
        }
        return this.beta;
    }

    public double getBeta() {
        return this.beta;
    }

    public void setCurrentPosition(double currPos) {
        mCurrPos = currPos;
    }

    public double currentPosition() {
        return mCurrPos;
    }

    public void setSpacetime(int spacetime) {
        if (spacetime == 0 || spacetime == 1) {
            mSpacetime = spacetime;
        }
    }

    public void setCamera(int camera) {
        if (camera == 0 || camera == 1) {
            mCamera = camera;
        }
    }
}
