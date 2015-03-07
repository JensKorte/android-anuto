package ch.bfh.anuto.game;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

public class BasicTower extends Tower {
    public final static int RELOAD_TIME = 20;

    public BasicTower(Game game, PointF position) {
        super(game, position);

        mPaint.setColor(Color.GREEN);
    }

    public int ticksUntilShot = RELOAD_TIME;

    @Override
    public void tick() {
        ticksUntilShot--;

        if (ticksUntilShot < 0) {
            ticksUntilShot = RELOAD_TIME;

            Enemy enemy = nextEnemy();
            if (enemy != null) {
                mGame.addObject(new BasicShot(mGame, this, enemy));
            }
        }
    }

    @Override
    public void draw(Canvas canvas) {
        canvas.drawRect(mGame.getBlockOnScreen(mPosition), mPaint);
    }
}
