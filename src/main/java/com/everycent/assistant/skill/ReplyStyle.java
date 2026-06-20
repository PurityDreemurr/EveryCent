package com.everycent.assistant.skill;

import java.util.ArrayList;
import java.util.List;

public class ReplyStyle {

    private SceneType scene = SceneType.UNKNOWN;

    private DialogueAct dialogueAct = DialogueAct.CHAT;

    private String tone = "concise";

    private Integer mood;

    private String emoji;

    private List<String> tags = new ArrayList<>();

    public SceneType getScene() {
        return scene;
    }

    public void setScene(SceneType scene) {
        this.scene = scene;
    }

    public DialogueAct getDialogueAct() {
        return dialogueAct;
    }

    public void setDialogueAct(DialogueAct dialogueAct) {
        this.dialogueAct = dialogueAct;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public Integer getMood() {
        return mood;
    }

    public void setMood(Integer mood) {
        this.mood = mood;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags == null ? new ArrayList<>() : tags;
    }
}
