package com.mantledillusion.essentials.json.patch.ignore;

import com.mantledillusion.essentials.json.patch.PatchUtil;
import com.mantledillusion.essentials.json.patch.PatchUtil.Snapshot;
import com.mantledillusion.essentials.json.patch.model.Patch;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

/**
 * Annotation for {@link Field}s that are not allowed to cause {@link Patch}es when {@link Snapshot#capture()}d or
 * changed when {@link Patch}es are {@link PatchUtil#apply(Object, Patch...)}d.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoPatch {

}