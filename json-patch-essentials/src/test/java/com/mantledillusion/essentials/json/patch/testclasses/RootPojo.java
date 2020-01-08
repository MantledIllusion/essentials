package com.mantledillusion.essentials.json.patch.testclasses;

import com.mantledillusion.essentials.json.patch.ignore.NoPatch;

import java.util.ArrayList;
import java.util.List;

public class RootPojo {

    private SubPojo first;
    private SubPojo second;
    @NoPatch
    private SubPojo unpatchable;
    private List<ListedPojo> listed = new ArrayList<>();

    public SubPojo getFirst() {
        return first;
    }

    public void setFirst(SubPojo first) {
        this.first = first;
    }

    public SubPojo getSecond() {
        return second;
    }

    public void setSecond(SubPojo second) {
        this.second = second;
    }

    public SubPojo getUnpatchable() {
        return unpatchable;
    }

    public void setUnpatchable(SubPojo unpatchable) {
        this.unpatchable = unpatchable;
    }

    public List<ListedPojo> getListed() {
        return listed;
    }

    public void setListed(List<ListedPojo> listed) {
        this.listed = listed;
    }
}
