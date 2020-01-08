package com.mantledillusion.essentials.json.patch.testclasses;

import java.util.Objects;

public class SubPojo {

    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubPojo subPojo = (SubPojo) o;
        return Objects.equals(id, subPojo.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
