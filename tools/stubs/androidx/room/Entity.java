package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test-only stub mirroring the subset of androidx.room.Entity used by this project. */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Entity {
    String tableName() default "";
    Index[] indices() default {};
    ForeignKey[] foreignKeys() default {};
}
