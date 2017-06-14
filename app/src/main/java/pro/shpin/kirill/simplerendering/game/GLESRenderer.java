package pro.shpin.kirill.simplerendering.game;

import static android.opengl.GLES20.*;
import static android.opengl.GLES30.*;
import static android.opengl.GLES31.*;
import static javax.microedition.khronos.opengles.GL11ExtensionPack.GL_RGBA8;

import android.opengl.GLSurfaceView;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import pro.shpin.kirill.simplerendering.R;

public class GLESRenderer implements GLSurfaceView.Renderer{

	public static float width;
	public static float height;
	public static int fbowidth;
	public static int fboheight;
	public static float aspectX;
	public static float aspectY;

	public static int renderMode = 1;
	public static boolean updatedClearRender = false;

	private boolean firstDraw;
	private boolean surfaceCreated;

	private long lastTime;
	private int FPS;
	private int frames;

	private int vbo;
	private int ibo;

	private int vertShader;
	private int fragShader;
	private int shaderProgram;

	private int fractalvertShader;
	private int fractalfragShader;
	private int fractalshaderProgram;

	private int aspectLoc;
	private int cLoc;
	private int maxIterationLoc;
	private int colorSchemeLoc;
	private int colorInsideLoc;
	private int colorOutsideLoc;
	private int scaleLoc;
	private int scLoc;
	private int offLoc;
	private int runLoc;
	private int texLoc;


	private int zxTex;
	private int zyTex;
	private int zzTex;
	private int zwTex;
	private int ziTex;

	public static float SCALING = 4.0f;

	private int loadShader(String source, int type) {
		int shader;

		shader = glCreateShader(type);

		if (shader == 0) {
			return 0;
		}

		glShaderSource(shader, source);

		glCompileShader(shader);

		IntBuffer intBuf= ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

		int status;
		glGetShaderiv(shader, GL_COMPILE_STATUS, intBuf);
		status=intBuf.get(0);

		if(status==0){
			glGetShaderiv(shader,GL_INFO_LOG_LENGTH,intBuf);
			status=intBuf.get(0);
			if (status>1){
				Log.i("Shader","Shader: " + glGetShaderInfoLog(shader));
			}
			glDeleteShader(shader);
			Log.w("Shader","Shader error.");
			return 0;
		}

		return shader;
	}

	private void initModel() {
		float[] tri = {
				 1,  1,
				-1,  1,
				 1, -1,
				-1, -1
		};

		int[] indicies = {1, 0, 2, 2, 1, 3};

		FloatBuffer buffer = ByteBuffer.allocateDirect(tri.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		buffer.put(tri);
		buffer.position(0);

		IntBuffer intBuf = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();

		glGenBuffers(1, intBuf);
		vbo = intBuf.get(0);
		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBufferData(GL_ARRAY_BUFFER, buffer.capacity()* 4, buffer, GL_STATIC_DRAW);
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		IntBuffer indexBuffer = ByteBuffer.allocateDirect(indicies.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
		indexBuffer.put(indicies);
		indexBuffer.position(0);

		glGenBuffers(1, intBuf);
		ibo = intBuf.get(0);

		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.capacity() * 4, indexBuffer, GL_STATIC_DRAW);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
	}

	private void initShaders() {
		vertShader = loadShader(Utils.readFromFile(R.raw.shader_vert), GL_VERTEX_SHADER);
		fragShader = loadShader(Utils.readFromFile(R.raw.shader_frag), GL_FRAGMENT_SHADER);

		shaderProgram  = glCreateProgram();

		glAttachShader(shaderProgram, vertShader);
		glAttachShader(shaderProgram, fragShader);

		glBindAttribLocation(shaderProgram, 0, "position");

		glLinkProgram(shaderProgram);

		aspectLoc = glGetUniformLocation(shaderProgram, "aspect");

		cLoc = glGetUniformLocation(shaderProgram, "c");
		maxIterationLoc = glGetUniformLocation(shaderProgram, "maxIteration");
		scaleLoc = glGetUniformLocation(shaderProgram, "scale");
		scLoc = glGetUniformLocation(shaderProgram, "sc");
		offLoc = glGetUniformLocation(shaderProgram, "off");
		runLoc = glGetUniformLocation(shaderProgram, "run");

		glUseProgram(shaderProgram);

		glUniform2f(cLoc, 0, 0);
		glUniform1i(maxIterationLoc, 500);
		glUniform1i(colorSchemeLoc, 2);
		glUniform3f(colorInsideLoc, 0, 0, 0);
		glUniform3f(colorOutsideLoc, 0, 1, 0);

		glUseProgram(0);

		fractalvertShader = loadShader(Utils.readFromFile(R.raw.fractal_vert), GL_VERTEX_SHADER);
		fractalfragShader = loadShader(Utils.readFromFile(R.raw.fractal_frag), GL_FRAGMENT_SHADER);

		fractalshaderProgram  = glCreateProgram();

		glAttachShader(fractalshaderProgram, fractalvertShader);
		glAttachShader(fractalshaderProgram, fractalfragShader);

		glBindAttribLocation(fractalshaderProgram, 0, "position");

		glLinkProgram(fractalshaderProgram);


		colorSchemeLoc = glGetUniformLocation(fractalshaderProgram, "colorScheme");
		colorInsideLoc = glGetUniformLocation(fractalshaderProgram, "colorInside");
		colorOutsideLoc = glGetUniformLocation(fractalshaderProgram, "colorOutside");
		texLoc = glGetUniformLocation(fractalshaderProgram, "tex");

		glUseProgram(fractalshaderProgram);

		glUniform1i(colorSchemeLoc, 2);
		glUniform3f(colorInsideLoc, 0, 0, 0);
		glUniform3f(colorOutsideLoc, 0, 1, 0);

		glUseProgram(0);
	}

	private void initTextures() {
		int[] texa = new int[5];

		glGenTextures(5, texa, 0);
		zxTex = texa[0];
		zyTex = texa[1];
		zzTex = texa[2];
		zwTex = texa[3];
		ziTex = texa[4];

		glBindTexture(GL_TEXTURE_2D, zxTex);
		glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, (int)width, (int)height);
		glBindTexture(GL_TEXTURE_2D, zyTex);
		glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, (int)width, (int)height);
		glBindTexture(GL_TEXTURE_2D, zzTex);
		glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, (int)width, (int)height);
		glBindTexture(GL_TEXTURE_2D, zwTex);
		glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32F, (int)width, (int)height);
		glBindTexture(GL_TEXTURE_2D, ziTex);
		glTexStorage2D(GL_TEXTURE_2D, 1, GL_R32I, (int)width, (int)height);
		glBindTexture(GL_TEXTURE_2D, 0);

		glBindImageTexture(0, ziTex, 0, false, 0, GL_READ_WRITE, GL_R32I);
		glBindImageTexture(1, zxTex, 0, false, 0, GL_READ_WRITE, GL_R32F);
		glBindImageTexture(2, zyTex, 0, false, 0, GL_READ_WRITE, GL_R32F);
		glBindImageTexture(3, zzTex, 0, false, 0, GL_READ_WRITE, GL_R32F);
		glBindImageTexture(4, zwTex, 0, false, 0, GL_READ_WRITE, GL_R32F);
	}

	public void initGL() {
		glClearColor(0.2f, 0.3f, 0.8f, 1f);

		initModel();
		initShaders();
		initTextures();
	}

	public GLESRenderer() {
		firstDraw = true;
		surfaceCreated = false;
		width = -1;
		height = -1;
		lastTime = System.currentTimeMillis();
		FPS = 0;
		vertShader = 0;
		fragShader = 0;
		shaderProgram = 0;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		surfaceCreated = true;
		width = -1;
		height = -1;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int w, int h) {
		if(!surfaceCreated && width == w && height == h) {
			return;
		}

		width = w;
		height = h;

		fbowidth = (int)(width/SCALING);
		fboheight = (int)(height/SCALING);

		float aspect = height/width;

		if(aspect >= 1) {
			aspectX = 1;
			aspectY = aspect;
		} else {
			aspectX = 1.0f/aspect;
			aspectY = 1;
		}

		initGL();

		surfaceCreated = false;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		onDrawFrame(firstDraw);

		frames++;
		long currentTime = System.currentTimeMillis();
		if(currentTime - lastTime >= 1000) {
			FPS = frames;
			Log.d("FPS", "FPS: " + FPS);
			frames = 0;
			lastTime = currentTime;
		}

		if(firstDraw) {
			firstDraw = false;
		}
	}

	public void renderFractal(float sc) {
		glUseProgram(shaderProgram);

		glUniform1i(runLoc, 1);
		glUniform1f(scaleLoc, sc);

		glUniform2f(aspectLoc, aspectX, aspectY);

		try {
			GameView.semaphore.acquire();

			float scHigh = (float)GameView.totalScale;
			float scLow = (float)(GameView.totalScale - (double)scHigh);

			glUniform2f(scLoc, scHigh, scLow);

			float offXHigh = (float)GameView.offsetX;
			float offYHigh = (float)GameView.offsetY;
			float offXLow = (float)(GameView.offsetX - (double)offXHigh);
			float offYLow = (float)(GameView.offsetY - (double)offYHigh);

			glUniform4f(offLoc, offXHigh, offYHigh, offXLow, offYLow);

			GameView.semaphore.release();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
	}

	public void onDrawFrame(boolean firstDraw) {
		glClear(GL_COLOR_BUFFER_BIT);

		glBindBuffer(GL_ARRAY_BUFFER, vbo);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);

		glEnableVertexAttribArray(0);
		glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * 4, 0);

		if(renderMode == 2) {
			renderFractal(SCALING);
		} else if(renderMode == 1) {
			renderFractal(1.0f);
			renderMode = 0;
		}

		glUseProgram(fractalshaderProgram);

		glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);

		glUseProgram(0);

		glDisableVertexAttribArray(0);

		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);

		int error;
		while((error = glGetError()) != GL_NO_ERROR) {
			Log.i("Info", "GLError: " + error);
		}
	}
}
