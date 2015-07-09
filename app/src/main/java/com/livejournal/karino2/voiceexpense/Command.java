package com.livejournal.karino2.voiceexpense;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by karino on 7/9/15.
 */
public abstract class Command {
    ArrayList<String> names;
    public Command(String name) {
        names = new ArrayList<>();
        names.add(name);
    }
    public Command(String[] names) {
        this.names = new ArrayList<>(Arrays.asList(names));
    }

    public boolean isMatch(String word) {
        return names.contains(word);
    }
    public abstract void action();
}
