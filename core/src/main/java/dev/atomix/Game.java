package dev.atomix;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ScreenUtils;

import dev.atomix.screens.GameScreen;
import dev.atomix.screens.MainScreen;

import java.util.HashMap;
import java.util.Map;

public class Game extends ApplicationAdapter {

    public static final Logger LOGGER = new Logger("Main", Logger.DEBUG);

    private static final Map<String, Screen> SCREENS = new HashMap<>();

    private static Screen s_Screen;
    public static void SetScreen(String name) {
        if(s_Screen != null) s_Screen.hide();

        Screen screen = null;
        if(SCREENS.containsKey(name))
            screen = SCREENS.get(name);

        s_Screen = screen;
        if(s_Screen != null) s_Screen.show();
    }

    private Assets m_Assets;

    @Override
    public void create() {
        m_Assets = new Assets();

        SCREENS.put("main", new MainScreen(m_Assets));
        SCREENS.put("game", new GameScreen(m_Assets));

        SetScreen("game");
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        if(s_Screen != null) s_Screen.render(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void dispose() {
        m_Assets.dispose();

        SetScreen("nil");
        for(Screen screen : SCREENS.values())
            screen.dispose();

        SCREENS.clear();
    }
}
