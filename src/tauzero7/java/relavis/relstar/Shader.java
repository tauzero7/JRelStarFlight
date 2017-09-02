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

import javax.media.opengl.GL2;

public class Shader {
    protected GL2     gl;
    protected boolean isLinked;
    protected int     prog;
    protected int     fragHandle;
    protected int     vertHandle;

    /**
     * Shader
     * 
     * @param gl
     *            : opengl.
     */
    public Shader(GL2 gl) {
        this.gl = gl;
        this.isLinked = false;
        this.prog = 0;
    }

    /**
     * Set shaders
     * 
     * @param gl
     *            : opengl.
     */
    public void setShaders(GL2 gl) {
        vertHandle = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
        String[] vt = getVertShaderString();
        gl.glShaderSource(vertHandle, 1, vt, null);
        gl.glCompileShader(vertHandle);
        printInfoLog("vs", vertHandle);

        fragHandle = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);
        String[] ft = getFragShaderString();
        gl.glShaderSource(fragHandle, 1, ft, null);
        gl.glCompileShader(fragHandle);
        printInfoLog("fs", fragHandle);

        prog = gl.glCreateProgram();
        gl.glAttachShader(prog, vertHandle);
        gl.glAttachShader(prog, fragHandle);

        gl.glBindAttribLocation(prog, Defs.VERTEX_MAG_ARRAY, "absMag");
        gl.glBindAttribLocation(prog, Defs.VERTEX_TEMP_ARRAY, "starTemp");

        gl.glLinkProgram(prog);
        printInfoLog("prog", prog);

        int[] mPrComp = new int[1];
        gl.glGetObjectParameterivARB(prog, GL2.GL_OBJECT_LINK_STATUS_ARB,
                mPrComp, 0);
        if (mPrComp[0] != 1) {
            isLinked = false;
        } else {
            isLinked = true;
        }
    }

    /**
     * Bind shader
     * 
     */
    public void bind() {
        if (isLinked) {
            gl.glUseProgram(this.prog);
        }
    }

    /**
     * Release shader
     * 
     */
    public void release() {
        gl.glUseProgram(0);
    }

    /**
     * Delete shader
     * 
     */
    public void delete() {
        release();
        if (isLinked) {
            // System.err.println("delete");

            gl.glDetachShader(prog, fragHandle);
            gl.glDetachShader(prog, vertHandle);
            gl.glDeleteShader(vertHandle);
            gl.glDeleteShader(fragHandle);
            gl.glDeleteProgram(prog);

            // gl.glDeleteObjectARB(prog);
        }
        isLinked = false;
        vertHandle = fragHandle = prog = 0;
    }

    /**
     * Location of shader variable
     * 
     * @param name
     *            : name of shader variable.
     * @return uniform location index.
     */
    public int location(String name) {
        return gl.glGetUniformLocation(prog, name);
    }

    /**
     * Print information log after shader initialization.
     * 
     * @param text
     *            : text.
     * @param handle
     *            : shader handle index.
     */
    public void printInfoLog(String text, int handle) {
        int[] status = new int[1];
        gl.glGetObjectParameterivARB(fragHandle,
                GL2.GL_OBJECT_COMPILE_STATUS_ARB, status, 0);

        if (status[0] == 1) {
            return;
        }

        int[] infoLogLength = new int[1];
        gl.glGetObjectParameterivARB(handle, GL2.GL_OBJECT_INFO_LOG_LENGTH_ARB,
                infoLogLength, 0);

        if (infoLogLength[0] == 0) {
            return;
        }
        
        int[] charsWritten = new int[1];
        byte[] infoLog = new byte[infoLogLength[0]];
        gl.glGetInfoLogARB(handle, infoLogLength[0], charsWritten, 0, infoLog,
                0);

        for (int i = 0; i < charsWritten[0]; i++) {
            System.err.format("%c", infoLog[i]);
        }
        System.err.println("");
    }

    /**
     * Vertex shader.
     * 
     * @return shader text.
     */
    protected String[] getVertShaderString() {
        String[] text = new String[1];
        text[0] = "#version 120\n";
        text[0] += "#define edlg10   0.434294482\n";
        text[0] += "#define PI       3.14159265\n";
        text[0] += "#define PI_2     1.57079633\n";
        text[0] += "#define invPI    0.31830989\n";
        text[0] += "#define invTwoPI 0.15915494\n";

        text[0] += "attribute float  absMag;";
        text[0] += "attribute float  starTemp;";

        text[0] += "uniform   int    spacetime;"; // Minkowski=0, Warp=1
        text[0] += "uniform   int    camera;"; // 4pi=0, Pinhole=1
        text[0] += "uniform   float  fovYh;"; // radians
        text[0] += "uniform   sampler2D  texWarp;";

        text[0] += "uniform   float beta;";
        text[0] += "uniform   float curr_pos;";

        text[0] += "uniform   vec2  wSize;";
        text[0] += "uniform   mat4  rotmat;";
        text[0] += "uniform   mat4  tetrad;";

        text[0] += "varying   float appMag;";
        text[0] += "varying   float logTemp;";
        text[0] += "varying   float one_over_mu;";

        text[0] += "void transCartSphere( const vec3 p, inout float theta, inout float phi )\n";
        text[0] += "{\n";
        text[0] += "  theta = atan(p.z,sqrt(p.x*p.x+p.y*p.y));\n";
        text[0] += "  phi   = atan(p.y,p.x); \n";
        text[0] += "}\n";

        text[0] += "void calcNewRaDec ( in vec3 dir, out float newRa, out float newDe )\n";
        text[0] += "{\n";
        text[0] += "  vec3 ex,ey,ez;\n";
        text[0] += "  ex = vec3(1.0,0.0,0.0);\n";
        text[0] += "  ez = normalize(cross(ex,dir));\n";
        text[0] += "  ey = normalize(cross(ez,ex));\n";
        text[0] += "  float phi = acos(dir.x);\n";
        text[0] += "  float newPhi = phi;\n";
        text[0] += "  newRa = newDe = 0.0;\n";
        text[0] += "  vec3  r = cos(newPhi)*ex + sin(newPhi)*ey;\n";
        text[0] += "  transCartSphere(r,newDe,newRa);\n";
        text[0] += "}\n";

        text[0] += "int calcWarpRaDec( in vec3 dir, out float omega, out float edmu, out float newRa, out float newDe )\n";
        text[0] += "{\n";
        text[0] += "  float phi = acos(dir.x)*invPI;\n";
        text[0] += "  vec3  val = texture2D(texWarp,vec2(phi,beta+0.01)).xyz;";

        text[0] += "  float xi  = val.x;";
        text[0] += "  omega     = 1.0/val.y;";
        text[0] += "  edmu      = 1.0/val.z;";
        text[0] += "  if (xi<0.0) return 0;";
        text[0] += "  vec3 ex,ey,ez;\n";
        text[0] += "  ex = vec3(1.0,0.0,0.0);\n";
        text[0] += "  ez = normalize(cross(ex,dir));\n";
        text[0] += "  ey = normalize(cross(ez,ex));\n";
        text[0] += "  vec3 r = cos(xi)*ex + sin(xi)*ey;";
        text[0] += "  transCartSphere(r,newDe,newRa);\n";
        text[0] += "  return 1;";
        text[0] += "}\n";

        text[0] += "void main()";
        text[0] += "{";
        text[0] += "  vec4 vert = vec4(0,0,0,1);";
        text[0] += "  float psc = 1000.0/gl_Vertex.x;";
        text[0] += "  float ra  = gl_Vertex.y;";
        text[0] += "  float dec = gl_Vertex.z;";

        // rotation of global coordinate system
        text[0] += "  vec4 starDir = psc*vec4(cos(dec)*cos(ra),cos(dec)*sin(ra),sin(dec),0.0)*3.26;";
        text[0] += "  starDir = rotmat*starDir;";

        // relative position between star and observer
        text[0] += "  vec3 ldir = starDir.xyz - vec3(curr_pos,0,0);";

        // light vector in global system
        text[0] += "  vec4 k = vec4(1.0,-normalize(ldir));";
        text[0] += "  float omega = 1.0;";
        text[0] += "  one_over_mu = 1.0;";
        text[0] += "  int validPoint = 1;";

        // light vector in observer system (Minkowski)
        text[0] += "  if (spacetime==0) {\n";
        text[0] += "    vec4 kn = tetrad*k;";
        text[0] += "    omega = length(kn.x);";
        text[0] += "    calcNewRaDec(-normalize(kn.yzw),ra,dec);";
        text[0] += "  } else {\n";
        text[0] += "    validPoint = calcWarpRaDec(-k.yzw,omega,one_over_mu,ra,dec);";
        text[0] += "  }\n";

        text[0] += "  if (camera==0) {\n";
        text[0] += "    vert.xy     = vec2(0.5-ra*invTwoPI,0.5+dec*invPI)*wSize;";
        text[0] += "  } else {\n";
        text[0] += "    float d = wSize.y*0.5/tan(fovYh);";
        text[0] += "    vec3 v = vec3(cos(dec)*cos(ra),cos(dec)*sin(ra),sin(dec));";
        text[0] += "    if (v.x>0.0)\n";
        text[0] += "      vert.xy = vec2(wSize.x*0.5-d*v.y/v.x,wSize.y*0.5+d*v.z/v.x);";
        text[0] += "    else\n";
        text[0] += "      vert.xy = vec2(0,0);";
        text[0] += "  }\n";

        text[0] += "  gl_Position = gl_ModelViewProjectionMatrix*vert;";

        text[0] += "  logTemp = log( starTemp*omega )*edlg10;";
        text[0] += "  appMag = absMag + 10.0 - 5.0*log(psc)*edlg10;";

        text[0] += "  if (appMag>14.0 || validPoint==0)\n";
        text[0] += "    gl_PointSize = 0.0;";
        text[0] += "  else\n";
        text[0] += "    gl_PointSize = 10.0;";
        text[0] += "}";
        return text;
    }

    /**
     * Fragment shader.
     * 
     * @return shader text.
     */
    protected String[] getFragShaderString() {
        String[] text = new String[1];
        text[0] = "#version 120\n";
        text[0] += "#define  DEF_APPMAG_FACTOR_4PI  -0.4\n";
        text[0] += "#define  DEF_APPMAG_FACTOR_PIN  -0.3\n";
        text[0] += "#define  DEF_gamma           1.0\n";
        text[0] += "#define  DEF_s0              1.0\n";
        text[0] += "#define  minTemp             3.0\n";
        text[0] += "#define  IlluminantC_x   0.3101\n";
        text[0] += "#define  IlluminantC_y   0.3162\n";

        text[0] += "#define  IlluminantD65_x 0.3127\n";
        text[0] += "#define  IlluminantD65_y 0.3291\n";

        text[0] += "#define  IlluminantE_x   0.33333\n";
        text[0] += "#define  IlluminantE_y   0.33333\n";

        text[0] += "#define  xRed    0.7355\n";
        text[0] += "#define  yRed    0.2645\n";
        text[0] += "#define  xGreen  0.2658\n";
        text[0] += "#define  yGreen  0.7243\n";
        text[0] += "#define  xBlue   0.1669\n";
        text[0] += "#define  yBlue   0.0085\n";
        text[0] += "#define  xWhite  IlluminantE_x\n";
        text[0] += "#define  yWhite  IlluminantE_y\n";

        text[0] += "uniform   sampler2D  texPsiTemp;";
        text[0] += "uniform   sampler2D  texSigma;";

        text[0] += "uniform   int  spacetime;"; // Minkowski=0, Warp=1
        text[0] += "uniform   int  camera;"; // 4pi=0, Pinhole=1

        text[0] += "uniform  float def_gamma;";
        text[0] += "uniform  float def_s0;";

        text[0] += "varying   float logTemp;";
        text[0] += "varying   float appMag;";
        text[0] += "varying   float one_over_mu;";

        text[0] += "void  xyz_to_rgb( in vec3 xc, out vec3 rgb )\n";
        text[0] += "{\n";
        text[0] += "  vec3 xx = vec3( xRed, xGreen, xBlue );\n";
        text[0] += "  vec3 yy = vec3( yRed, yGreen, yBlue );\n";
        text[0] += "  vec3 zz = vec3(1.0) - xx - yy;\n";

        text[0] += "  vec3 w = vec3( xWhite, yWhite, 1.0-xWhite-yWhite );\n";
        text[0] += "  vec3 rgbx, rgby, rgbz, rgbw;\n";

        /* xyz -> rgb matric, before scaling to white */
        text[0] += "  rgbx = yy.gbr*zz.brg - yy.brg*zz.gbr;\n";
        text[0] += "  rgby = xx.brg*zz.gbr - xx.gbr*zz.brg;\n";
        text[0] += "  rgbz = xx.gbr*yy.brg - xx.brg*yy.gbr;\n";

        /*
         * White scaling factors. Dividing by w.y scales the white luminance to
         * unity, as conventional.
         */
        text[0] += "  rgbw = (rgbx*w.x + rgby*w.y + rgbz*w.z)/w.y;\n";

        /* xyz -> rgb matrix, correctly scaled to white. */
        text[0] += "  rgbx = rgbx/rgbw;\n";
        text[0] += "  rgby = rgby/rgbw;\n";
        text[0] += "  rgbz = rgbz/rgbw;\n";

        text[0] += "  rgb = rgbx*xc.x + rgby*xc.y + rgbz*xc.z;\n";

        text[0] += "  rgb.r = pow( rgb.r, 1.0/def_gamma );\n";
        text[0] += "  rgb.g = pow( rgb.g, 1.0/def_gamma );\n";
        text[0] += "  rgb.b = pow( rgb.b, 1.0/def_gamma );\n";
        text[0] += "}\n";

        text[0] += "void main()";
        text[0] += "{";
        text[0] += "  float dist = length(gl_PointCoord - vec2(0.5));";
        text[0] += "  if (dist>0.5)\n";
        text[0] += "    discard;";
        text[0] += "  float tempPos = clamp((logTemp - minTemp)/1.7,0.0,0.9999);";
        text[0] += "  vec3  XYZ     = texture2D( texSigma,   vec2(dist,tempPos) ).xyz;";
        text[0] += "  float psiV    = texture2D( texPsiTemp, vec2(tempPos,0) ).a;";

        text[0] += "  float unlensed = pow(10.0,DEF_APPMAG_FACTOR_4PI*appMag)/psiV*def_s0;";
        text[0] += "  if (camera==1) {\n";
        text[0] += "    unlensed = pow(10.0,DEF_APPMAG_FACTOR_PIN*appMag)/psiV*def_s0;";
        text[0] += "  }\n";
        text[0] += "  vec3  xyz      = XYZ*unlensed/one_over_mu;";

        text[0] += "  vec3 rgb;";
        text[0] += "  xyz_to_rgb( xyz, rgb );";
        text[0] += "  vec4 color = vec4(rgb,1.0);";
        text[0] += "  gl_FragColor = color;";
        text[0] += "}";
        return text;
    }
}
