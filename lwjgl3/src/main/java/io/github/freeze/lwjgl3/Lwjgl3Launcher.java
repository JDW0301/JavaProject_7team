package io.github.freeze.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import io.github.freeze.Core;   // core 모듈의 Game 클래스
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setHdpiMode(HdpiMode.Pixels);
        config.setTitle("DugeunDugeun Prism");
        config.setWindowedMode(1280, 960);  // 4:3
        config.useVsync(true);
        config.setResizable(true);
        new Lwjgl3Application(new Core(), config);
    }
}


