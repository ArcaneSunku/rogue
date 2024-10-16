package dev.atomix;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;

/**
 * A basic wrapper class around {@link AssetManager} that can be passed around with more control
 */
public class Assets {

    private final AssetManager m_Manager;

    public Assets() {
        m_Manager = new AssetManager(new InternalFileHandleResolver(), true);
    }

    public boolean update(int millis) {
        return m_Manager.update(millis);
    }

    public boolean update() {
        return m_Manager.update();
    }

    public void finishLoading() {
        m_Manager.finishLoading();
    }

    public void unload(String name) {
        m_Manager.unload(name);
    }

    public void clear() {
        m_Manager.clear();
    }

    public <T> void load(AssetDescriptor<T> descriptor) {
        m_Manager.load(descriptor);
    }

    public <T> void load(String name, Class<T> type) {
        m_Manager.load(name, type);
    }

    public <T> void load(String name, Class<T> type, AssetLoaderParameters<T> parameters) {
        m_Manager.load(name, type, parameters);
    }

    public float getProgress() {
        return m_Manager.getProgress();
    }

    public boolean isFinished() {
        return m_Manager.isFinished();
    }

    public <T> T get(String name) {
        return m_Manager.get(name);
    }


    public <T> T get(AssetDescriptor<T> descriptor) {
        return m_Manager.get(descriptor);
    }

    public <T> T get(String name, Class<T> type) {
        return m_Manager.get(name, type);
    }

    public void dispose() {
        m_Manager.dispose();
    }

}
