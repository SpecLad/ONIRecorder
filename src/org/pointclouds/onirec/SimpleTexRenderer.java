package org.pointclouds.onirec;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static android.opengl.GLES20.*;

class SimpleTexRenderer implements GLSurfaceView.Renderer {
    private static final float[] positions = {0, 0, 1, 0, 1, 1, 0, 1};
    private static final FloatBuffer positionsBuf;

    private static final String vert_shader_source =
            "#version 100\n" +
            "uniform mat4 projection;\n" +
            "attribute vec2 position;\n" +
            "varying vec2 var_position;\n" +
            "void main() {\n" +
            "  gl_Position = projection * vec4(position, 0, 1);\n" +
            "  var_position = position;\n" +
            "}\n";

    private static final String frag_shader_source =
            "#version 100\n" +
            "uniform sampler2D tex;\n" +
            "varying mediump vec2 var_position;\n" +
            "void main() {\n" +
            "  gl_FragColor = texture2D(tex, var_position);\n" +
            "}\n";


    private final GLSurfaceView view;

    static {
        ByteBuffer bb = ByteBuffer.allocateDirect(positions.length * Float.SIZE / 8);
        bb.order(ByteOrder.nativeOrder());
        positionsBuf = bb.asFloatBuffer();
        positionsBuf.put(positions);
        positionsBuf.rewind();
    }

    public SimpleTexRenderer(GLSurfaceView view) {
        this.view = view;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        int[] ints = new int[1];

        int vert_shader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vert_shader, vert_shader_source);
        glCompileShader(vert_shader);

        glGetShaderiv(vert_shader, GL_COMPILE_STATUS, ints, 0);

        int frag_shader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(frag_shader, frag_shader_source);
        glCompileShader(frag_shader);

        glGetShaderiv(frag_shader, GL_COMPILE_STATUS, ints, 0);

        int program = glCreateProgram();
        glAttachShader(program, vert_shader);
        glAttachShader(program, frag_shader);
        glLinkProgram(program);

        glGetProgramiv(program, GL_LINK_STATUS, ints, 0);

        glDeleteShader(vert_shader);
        glDeleteShader(frag_shader);

        glUseProgram(program);

        int[] texes = new int[1];
        glGenTextures(1, texes, 0);
        int tex = texes[0];

        glBindTexture(GL_TEXTURE_2D, tex);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

        int[] bufs = new int[1];
        glGenBuffers(1, bufs, 0);
        int buf = bufs[0];

        glBindBuffer(GL_ARRAY_BUFFER, buf);
        glBufferData(GL_ARRAY_BUFFER, positionsBuf.capacity() * Float.SIZE / 8, positionsBuf, GL_STATIC_DRAW);

        int position_index = glGetAttribLocation(program, "position");

        glVertexAttribPointer(position_index, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(position_index);

        float[] projection = new float[16];
        Matrix.orthoM(projection, 0, 0, 1, 1, 0, -1, 1);

        glUniformMatrix4fv(glGetUniformLocation(program, "projection"), 1, false, projection, 0);
        glUniform1i(glGetUniformLocation(program, "tex"), 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        glClearColor(0, 0, 0, 1);
        glClear(GL_COLOR_BUFFER_BIT);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }

    public void requestUpdateTexture(final Bitmap bm) {
        view.queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (bm) {
                    GLUtils.texImage2D(GL_TEXTURE_2D, 0, bm, 0);
                }
            }
        });
    }

    public void requestRender() {
        view.requestRender();
    }
}
