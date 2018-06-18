package pers.cz.chaoxing.common;

public class PlayerInfo {
    private PlayerData[] attachments;
    private PlayerConfig defaults;
    private boolean control;

    public PlayerData[] getAttachments() {
        return attachments;
    }

    public void setAttachments(PlayerData[] attachments) {
        this.attachments = attachments;
    }

    public PlayerConfig getDefaults() {
        return defaults;
    }

    public void setDefaults(PlayerConfig defaults) {
        this.defaults = defaults;
    }

    public boolean isControl() {
        return control;
    }

    public void setControl(boolean control) {
        this.control = control;
    }
}
