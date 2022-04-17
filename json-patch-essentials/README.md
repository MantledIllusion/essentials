# JSON Patch Essentials

This library is meant to provide direct RFC 6902 support for Java objects, by providing functionality to capture and reapply changes to Java objects as patch DIFFs.

It uses [zjsonpatch](https://github.com/flipkart-incubator/zjsonpatch) for RFC compatibility and [Jackson](https://github.com/FasterXML/jackson-databind) for Java<->JSON mapping.

```xml
<dependency>
    <groupId>com.mantledillusion.essentials</groupId>
    <artifactId>json-patch-essentials</artifactId>
</dependency>
```

Get the newest version at [mvnrepository.com/json-patch-essentials](https://mvnrepository.com/artifact/com.mantledillusion.essentials/json-patch-essentials)

## Capturing changes

First, use the **_PatchUtil_** to take a **_Snapshot_** of your Java object <u>before</u> doing any changes; afterwards, the object can be changed arbitrarily. Using the Snapshot <u>after</u> changes being made, all changes will be captured as patches:

```java
Pojo object = new Pojo();

PatchUtil.Snapshot snapshot = PatchUtil.take(object);

object.setValue("some value");
object.getValueList().remove(1);


List<Patch> patches = snapshot.capture();
```

## Applying changes

The **_PatchUtil_** simply allows applying patches onto a given object instance, returning a new instance of that object with the changes applied:

```java
Pojo objectBefore = new Pojo();

Pojo objectAfter = PatchUtil.apply(objectBefore, patches);
```

## Permitting changes to unmodifiable values

There might be values in your Java objects that are not meant to be changed. This can be accomplished by annotating such fields with _@NoPatch_:

```java
public class Pojo() {

    @NoPatch
    private Long id;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
```

On the client side, the annotation will silently prevent patches being created for values that are being made to annotated values.

On the server side, the annotation will cause all incoming patching to unchangable values to throw a **_NoPatchException_** when applying them.
