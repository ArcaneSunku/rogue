package dev.atomix.screens;

import com.badlogic.gdx.Screen;
import dev.atomix.Assets;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@link Screen} implementation that takes in an {@link com.badlogic.gdx.assets.AssetManager} wrapper class and
 * gives select capabilities over to the Screen.
 */
public abstract class ScreenImpl implements Screen {

    private final Assets m_Assets;
    private final Map<String, String> m_AssetPaths;

    public ScreenImpl(Assets assets) {
        m_Assets =  assets;
        m_AssetPaths = new HashMap<>();
    }

    @Override
    public void pause() {
        // Optional support
    }

    @Override
    public void resume() {
        // Optional support
    }

    protected <T> void load(String name, String path, Class<T> type) {
        m_AssetPaths.put(name, path);
        m_Assets.load(path, type);
    }

    protected void unload(String name) {
        if(!m_AssetPaths.containsKey(name)) return;

        m_Assets.unload(m_AssetPaths.get(name));
        m_AssetPaths.remove(name);
    }

    protected void clear() {
        m_Assets.clear();
        m_AssetPaths.clear();
    }

    protected void finishLoading() {
        m_Assets.finishLoading();
    }

    protected float loadingProgress() {
        return m_Assets.getProgress();
    }

    protected boolean processAssets() {
        return m_Assets.update();
    }

    protected boolean finishedLoading() {
        return m_Assets.isFinished();
    }

    protected <T> T get(String name, Class<T> type) {
        if(!m_AssetPaths.containsKey(name)) return null;
        return m_Assets.get(m_AssetPaths.get(name), type);
    }

}
