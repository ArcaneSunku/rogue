package dev.atomix.screens;

import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import dev.atomix.Assets;

public class GameScreen extends ScreenImpl {

    public GameScreen(Assets assets) {
        super(assets);
    }

    private SpriteBatch batch = null;

    @Override
    public void show() {
        load("tiles", "textures/tiles.png", Texture.class);
        finishLoading();

        if(batch == null) batch = new SpriteBatch();
    }

    @Override
    public void render(float delta) {
        batch.begin();

        batch.end();
    }

    @Override
    public void resize(int width, int height) {

    }

    @Override
    public void hide() {
        clear();
    }

    @Override
    public void dispose() {
        if(batch != null) batch.dispose();
    }
}
