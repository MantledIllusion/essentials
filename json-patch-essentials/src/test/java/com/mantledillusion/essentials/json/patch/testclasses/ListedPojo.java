package com.mantledillusion.essentials.json.patch.testclasses;

import com.mantledillusion.essentials.json.patch.ignore.NoPatch;

import java.util.Objects;

public class ListedPojo {

    private String id;
    @NoPatch
    private String unpatchable;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUnpatchable() {
        return unpatchable;
    }

    public void setUnpatchable(String unpatchable) {
        this.unpatchable = unpatchable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListedPojo that = (ListedPojo) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
