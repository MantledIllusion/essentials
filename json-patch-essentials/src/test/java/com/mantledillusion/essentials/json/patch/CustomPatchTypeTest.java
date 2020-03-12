package com.mantledillusion.essentials.json.patch;

import com.mantledillusion.essentials.json.patch.model.PatchOperation;
import com.mantledillusion.essentials.json.patch.testclasses.RootPojo;
import com.mantledillusion.essentials.json.patch.testclasses.SubPojo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;

public class CustomPatchTypeTest implements TestConstants {

    public static class CustomPatchType {

        public CustomPatchType() {}

        public CustomPatchType(String op, String from, String path, String value) {
            this.op = op;
            this.from = from;
            this.path = path;
            this.value = value;
        }

        private String op;
        private String from;
        private String path;
        private String value;

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CustomPatchType that = (CustomPatchType) o;
            return op.equals(that.op) &&
                    Objects.equals(from, that.from) &&
                    path.equals(that.path) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, from, path, value);
        }

        @Override
        public String toString() {
            return "CustomPatchType{" +
                    "op='" + op + '\'' +
                    ", from='" + from + '\'' +
                    ", path='" + path + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

    @Test
    public void testCreateCustomPatches() {
        RootPojo pojo = new RootPojo();
        SubPojo first = new SubPojo();
        first.setId(ID_ABC);
        pojo.setFirst(first);

        PatchUtil.Snapshot snapshot = PatchUtil.take(pojo);

        first.setId(ID_DEF);

        List<CustomPatchType> ops = snapshot.capture(CustomPatchType.class);

        Assertions.assertNotNull(ops);
        Assertions.assertEquals(1, ops.size());
        Assertions.assertEquals(
                new CustomPatchType(PatchOperation.replace.name(), null, "/first/id", ID_DEF),
                ops.iterator().next());
    }
}
