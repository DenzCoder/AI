package androidx.room;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test-only stub of androidx.room.Index. */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface Index {
    String[] value();
    String name() default "";
    boolean unique() default false;
}
