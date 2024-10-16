package dev.atomix.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import dev.atomix.Assets;
import dev.atomix.level.Map;

public class GameScreen extends ScreenImpl {

    public GameScreen(Assets assets) {
        super(assets);
    }

    private SpriteBatch batch = null;
    private Map map;

    @Override
    public void show() {
        load("tiles", "textures/tiles.png", Texture.class);
        finishLoading();

        if(batch == null) batch = new SpriteBatch();

        Texture atlas = get("tiles", Texture.class);

        TextureRegion empty, floor, wall;
        empty = new TextureRegion(atlas, 0,  0, 16, 16);
        floor = new TextureRegion(atlas, 0, 16, 16, 16);
        wall = new TextureRegion(atlas, 16, 16, 16, 16);

        map = new Map(Gdx.graphics.getWidth() / 8, Gdx.graphics.getHeight() / 8, wall, empty, floor);
    }

    @Override
    public void render(float delta) {
        batch.begin();
        map.render(batch, 16);
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
