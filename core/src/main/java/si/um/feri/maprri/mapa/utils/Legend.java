package si.um.feri.maprri.mapa.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Legend {
    private final Skin skin;
    private final Stage stage;
    private final BitmapFont font;

    public Legend(Skin skin, Stage stage) {
        this.skin = skin;
        this.stage = stage;

        if (skin.has("font", BitmapFont.class)) {
            this.font = skin.getFont("font");
        } else {
            this.font = skin.getFont("default-font");
        }
        this.font.getData().setScale(0.9f);
    }

    public void draw(SpriteBatch batch, ShapeRenderer shapeRenderer) {
        batch.setProjectionMatrix(stage.getViewport().getCamera().combined);
        shapeRenderer.setProjectionMatrix(stage.getViewport().getCamera().combined);

        float screenWidth = stage.getViewport().getWorldWidth();
        float screenHeight = stage.getViewport().getWorldHeight();

        float startX = screenWidth - 180;
        float startY = screenHeight - 30;
        float spacing = 25;
        float circleRadius = 6;

        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.6f);
        shapeRenderer.rect(startX - 15, startY - 70, 185, 90);

        shapeRenderer.setColor(Color.YELLOW);
        shapeRenderer.circle(startX, startY, circleRadius);

        shapeRenderer.setColor(Color.RED);
        shapeRenderer.circle(startX, startY - spacing, circleRadius);

        shapeRenderer.setColor(Color.ORANGE);
        shapeRenderer.circle(startX, startY - spacing * 2, circleRadius);
        shapeRenderer.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);

        batch.begin();
        font.setColor(Color.WHITE);
        font.draw(batch, "Delavec", startX + 20, startY + 5);
        font.draw(batch, "Vozilo (polno)", startX + 20, startY - spacing + 5);
        font.draw(batch, "Vozilo (prazno)", startX + 20, startY - spacing * 2 + 5);
        batch.end();
    }
}
