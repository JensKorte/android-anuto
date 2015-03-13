package ch.bfh.anuto.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.PointF;
import android.util.Log;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameEngine implements Runnable {
    /*
    ------ Constants ------
     */

    private final static String TAG = GameEngine.class.getName();

    public final static int TARGET_FPS = 30;
    private final static int MAX_FRAME_SKIPS = 5;
    private final static int FRAME_PERIOD = 1000 / TARGET_FPS;

    /*
    ------ Members ------
     */

    private final SurfaceHolder mSurfaceHolder;
    private Thread mGameThread;
    private boolean mRunning = false;

    private final ArrayList<GameObject> mGameObjects = new ArrayList<>();
    private final ArrayList<GameObject> mObjectsToAdd = new ArrayList<>();
    private final ArrayList<GameObject> mObjectsToRemove = new ArrayList<>();

    private Point mGameSize;
    private Point mScreenSize;
    private Matrix mScreenMatrix;

    private final ArrayList<GameListener> mListeners = new ArrayList<>();

    /*
    ------ Constructors ------
     */

    public GameEngine(SurfaceHolder surfaceHolder) {
        mSurfaceHolder = surfaceHolder;
    }

    /*
    ------ Methods ------
     */

    public void addObject(GameObject obj) {
        mObjectsToAdd.add(obj);
    }

    public void removeObject(GameObject obj) {
        mObjectsToRemove.add(obj);
    }

    public List<GameObject> getObjects() {
        return Collections.unmodifiableList(mGameObjects);
    }

    public void setGameSize(int width, int height) {
        mGameSize = new Point(width, height);

        if (mScreenSize != null) {
            calcScreenMatrix();
        }
    }

    public void setScreenSize(int width, int height) {
        mScreenSize = new Point(width, height);

        if (mGameSize != null) {
            calcScreenMatrix();
        }
    }

    private void calcScreenMatrix() {
        mScreenMatrix = new Matrix();

        float tileSize = Math.min(mScreenSize.x / mGameSize.x, mScreenSize.y / mGameSize.y);
        mScreenMatrix.postTranslate(0.5f, 0.5f);
        mScreenMatrix.postScale(tileSize, tileSize);

        float paddingLeft = (mScreenSize.x - (tileSize * mGameSize.x)) / 2f;
        float paddingTop = (mScreenSize.y - (tileSize * mGameSize.y)) / 2f;
        mScreenMatrix.postTranslate(paddingLeft, paddingTop);
    }

    /*
    ------ GameEngine Loop ------
     */

    private void tick() {
        for (GameObject obj : mGameObjects) {
            obj.tick();
        }

        for (GameObject obj : mObjectsToAdd) {
            mGameObjects.add(obj);
            obj.setGame(this);
        }

        for (GameObject obj : mObjectsToRemove) {
            mGameObjects.remove(obj);
            obj.setGame(null);
        }

        mObjectsToAdd.clear();
        mObjectsToRemove.clear();
    }

    private void draw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        canvas.setMatrix(mScreenMatrix);

        for (GameObject obj : mGameObjects) {
            PointF pos = obj.getPosition();

            if (pos == null) {
                continue;
            }

            canvas.save();
            canvas.translate(pos.x, pos.y);

            obj.draw(canvas);

            canvas.restore();
        }
    }

    public void start() {
        mRunning = true;
        mGameThread = new Thread(this);
        mGameThread.start();
    }

    public void shutdown() throws InterruptedException {
        mRunning = false;
        mGameThread.join();
    }

    @Override
    public void run() {
        Canvas canvas;
        Log.d(TAG, "Starting game loop");

        // see http://www.javacodegeeks.com/2011/07/android-game-development-game-loop.html

        long beginTime;		// the time when the cycle begun
        long timeDiff;		// the time it took for the cycle to execute
        int sleepTime;		// ms to sleep (<0 if we're behind)
        int framesSkipped;	// number of frames being skipped

        try {
            while (mRunning) {
                canvas = null;

                beginTime = System.currentTimeMillis();
                framesSkipped = 0;

                // update game logic
                tick();

                // try locking the canvas for exclusive pixel editing in the surface
                try {
                    canvas = mSurfaceHolder.lockCanvas();

                    // render current game state
                    synchronized (mSurfaceHolder) {
                        draw(canvas);
                    }
                } finally {
                    if (canvas != null) {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

                // calculate the required sleep time for this cycle
                timeDiff = System.currentTimeMillis() - beginTime;
                sleepTime = (int)(FRAME_PERIOD - timeDiff);

                if (sleepTime > 0) {
                    // send the thread to sleep
                    Thread.sleep(sleepTime);
                }

                while (sleepTime < 0 && framesSkipped < MAX_FRAME_SKIPS) {
                    // we need to catch up --> update without rendering
                    tick();

                    // recalculate sleep time
                    timeDiff = System.currentTimeMillis() - beginTime;
                    sleepTime = (int)((1 + framesSkipped) * FRAME_PERIOD - timeDiff);

                    framesSkipped++;
                }

                if (framesSkipped > 0) {
                    Log.w(TAG, String.format("rendering of %d frames skipped", framesSkipped));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mRunning = false;
        }

        Log.d(TAG, "Stopping game loop");
    }

    /*
    ------ Listener Stuff ------
     */

    public void addListener(GameListener listener) {
        mListeners.add(listener);
    }

    public void removeListener(GameListener listener) {
        mListeners.remove(listener);
    }
}
