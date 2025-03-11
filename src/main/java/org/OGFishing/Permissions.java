package org.OGFishing;

public enum Permissions
{
    PERMISSION_RELOAD("ogfishing.reload"),
    PERMISSION_WAND("permission.wand"),
    PERMISSION_RESET("ogfishing.reset"),
    PERMISSION_SETPOOL("ogfishing.setpool");

    private final String value;

    Permissions(String value)
    {
        this.value = value;
    }

    public String getPermission()
    {
        return value;
    }
}
