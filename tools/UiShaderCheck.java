import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL33C.*;

/** Real GPU regression checks. Run with tools/check-ui-shaders.ps1 on Windows. */
public class UiShaderCheck {
    private static final int SIZE = 256;
    private static int vao, vbo, shape, glass, checks;

    public static void main(String[] args) throws Exception {
        GLFWErrorCallback errors = GLFWErrorCallback.createPrint(System.err);
        errors.set();
        if (!glfwInit()) throw new AssertionError("GLFW initialization failed");
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        long window = glfwCreateWindow(SIZE, SIZE, "UI shader regression", 0, 0);
        if (window == 0) throw new AssertionError("Cannot create a hidden OpenGL 3.3 context");
        try {
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("GPU: " + glGetString(GL_RENDERER));
            shape = program("ui_shape.frag");
            glass = program("ui_glass.frag");
            vao = glGenVertexArrays();
            vbo = glGenBuffers();
            glBindVertexArray(vao);
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            int[] components = {2, 2, 2, 4, 2, 4, 4};
            int[] offsets = {0, 8, 16, 24, 40, 48, 52};
            for (int i = 0; i < components.length; i++) {
                glEnableVertexAttribArray(i);
                glVertexAttribPointer(i, components[i], i < 5 ? GL_FLOAT : GL_UNSIGNED_BYTE, i >= 5, 56, offsets[i]);
            }
            FloatBuffer matrices = BufferUtils.createFloatBuffer(32);
            matrices.put(new float[]{2f / SIZE, 0, 0, 0, 0, -2f / SIZE, 0, 0, 0, 0, 1, 0, -1, 1, 0, 1});
            matrices.put(new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}).flip();
            int ubo = glGenBuffers();
            glBindBuffer(GL_UNIFORM_BUFFER, ubo);
            glBufferData(GL_UNIFORM_BUFFER, matrices, GL_STATIC_DRAW);
            glBindBufferBase(GL_UNIFORM_BUFFER, 0, ubo);
            glUniformBlockBinding(shape, glGetUniformBlockIndex(shape, "MeshData"), 0);
            glUniformBlockBinding(glass, glGetUniformBlockIndex(glass, "MeshData"), 0);
            int target = texture(null);
            int fbo = glGenFramebuffers();
            glBindFramebuffer(GL_FRAMEBUFFER, fbo);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, target, 0);
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) throw new AssertionError("Incomplete target");
            glViewport(0, 0, SIZE, SIZE);

            clear();
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 24, 24}, 0, 1, 0xffffffff, 0xffffffff);
            check("solid center", pixel(96, 80)[3], 255, 255);
            check("rounded corner", pixel(32, 32)[3], 0, 0);
            check("outside", pixel(20, 80)[3], 0, 0);
            clear();
            draw(shape, 32.5f, 32, 128, 96, new float[]{0, 0, 0, 0}, 0, 1, 0xffffffff, 0xffffffff);
            check("fractional edge AA", pixel(32, 80)[3], 110, 145);
            clear();
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 0, 0}, 0, 1, 0xffffffff, 0xffffffff);
            check("top-only rounded", pixel(32, 32)[3], 0, 0);
            check("square bottom corner", pixel(32, 127)[3], 240, 255);
            clear();
            draw(shape, 32, 32, 128, 96, new float[]{24, 0, 0, 24}, 0, 1, 0xffffffff, 0xffffffff);
            check("left-only rounded", pixel(32, 127)[3], 0, 0);
            check("square right corner", pixel(159, 32)[3], 240, 255);
            clear();
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 24, 24}, 2, 1, 0xffffffff, 0xffffffff);
            check("outline hollow center", pixel(96, 80)[3], 0, 0);
            check("outline visible edge", pixel(96, 32)[3], 240, 255);
            check("outline inner edge", pixel(96, 38)[3], 0, 0);
            clear();
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 24, 24}, -1, 8, 0xffffffff, 0xffffffff);
            check("shadow falloff", pixel(168, 80)[3], 120, 180);
            check("shadow far tail", pixel(182, 80)[3], 1, 10);
            clear();
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 24, 24}, 0, 1, 0xff0000ff, 0x0000ffff);
            check("gradient red top", pixel(96, 40)[0], 225, 240);
            check("gradient blue bottom", pixel(96, 118)[2], 220, 240);

            ByteBuffer backdrop = BufferUtils.createByteBuffer(SIZE * SIZE * 4);
            for (int y = 0; y < SIZE; y++) for (int x = 0; x < SIZE; x++)
                backdrop.put((byte) (y >= SIZE / 2 ? 255 : 0)).put((byte) 0).put((byte) (y < SIZE / 2 ? 255 : 0)).put((byte) 255);
            backdrop.flip();
            texture(backdrop);
            glUseProgram(glass);
            glUniform1i(glGetUniformLocation(glass, "u_Texture"), 0);
            clear();
            draw(glass, 32, 32, 192, 192, new float[]{24, 24, 24, 24}, 0, 1, 0xffffff80, 0xffffff80);
            check("glass top orientation", pixel(96, 64)[0], 255, 255);
            check("glass bottom orientation", pixel(96, 200)[2], 255, 255);
            check("glass mask", pixel(32, 32)[3], 0, 0);
            check("glass alpha", pixel(96, 64)[3], 128, 128);

            clear();
            glEnable(GL_BLEND);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 24, 24}, 0, 1, 0x0000ffff, 0x0000ffff);
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 24, 24}, 0, 1, 0xff000080, 0xff000080);
            check("painter order red", pixel(96, 80)[0], 127, 129);
            check("painter order blue", pixel(96, 80)[2], 126, 128);
            glDisable(GL_BLEND);
            clear();
            glEnable(GL_SCISSOR_TEST);
            glScissor(64, 0, 64, SIZE);
            draw(shape, 32, 32, 128, 96, new float[]{24, 24, 24, 24}, 0, 1, 0xffffffff, 0xffffffff);
            glDisable(GL_SCISSOR_TEST);
            check("scissor inside", pixel(96, 80)[3], 255, 255);
            check("scissor outside", pixel(48, 80)[3], 0, 0);
            check("OpenGL error", glGetError(), GL_NO_ERROR, GL_NO_ERROR);
            System.out.println("PASS: " + checks + " GPU pixel checks; both shader programs compiled and linked.");
        } finally {
            glfwDestroyWindow(window);
            glfwTerminate();
            errors.free();
        }
    }

    private static int program(String fragment) throws Exception {
        Path shaders = Path.of("src/main/resources/assets/meteor-client/shaders");
        int vs = compile(GL_VERTEX_SHADER, Files.readString(shaders.resolve("ui_shape.vert")));
        int fs = compile(GL_FRAGMENT_SHADER, Files.readString(shaders.resolve(fragment)));
        int program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) throw new AssertionError(glGetProgramInfoLog(program));
        glDeleteShader(vs);
        glDeleteShader(fs);
        return program;
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) throw new AssertionError(glGetShaderInfoLog(shader));
        return shader;
    }

    private static int texture(ByteBuffer data) {
        int texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, SIZE, SIZE, 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        return texture;
    }

    private static void clear() { glClearColor(0, 0, 0, 0); glClear(GL_COLOR_BUFFER_BIT); }

    private static void draw(int program, float x, float y, float width, float height, float[] radii,
                             float stroke, float softness, int top, int bottom) {
        float padding = stroke < 0 ? (float) Math.ceil(softness * 3) : 1.5f;
        float[][] points = {{-padding, -padding}, {-padding, height + padding}, {width + padding, height + padding}, {width + padding, -padding}};
        ByteBuffer vertices = BufferUtils.createByteBuffer(4 * 56).order(ByteOrder.nativeOrder());
        for (float[] local : points) {
            vertices.putFloat(x + local[0]).putFloat(y + local[1]).putFloat(local[0]).putFloat(local[1]);
            vertices.putFloat(width).putFloat(height);
            for (float radius : radii) vertices.putFloat(radius);
            vertices.putFloat(stroke).putFloat(softness);
            for (int color : new int[]{top, bottom}) for (int shift : new int[]{24, 16, 8, 0}) vertices.put((byte) (color >>> shift));
        }
        vertices.flip();
        glUseProgram(program);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STREAM_DRAW);
        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
    }

    private static int[] pixel(int x, int y) {
        ByteBuffer rgba = BufferUtils.createByteBuffer(4);
        glReadPixels(x, SIZE - 1 - y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, rgba);
        return new int[]{rgba.get(0) & 255, rgba.get(1) & 255, rgba.get(2) & 255, rgba.get(3) & 255};
    }

    private static void check(String name, int value, int min, int max) {
        if (value < min || value > max) throw new AssertionError(name + ": " + value + " outside " + min + ".." + max);
        checks++;
    }
}
