package androidx.room;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Test-only stub of androidx.room.ForeignKey. */
@Target({})
@Retention(RetentionPolicy.CLASS)
public @interface ForeignKey {
    int NO_ACTION = 1;
    int RESTRICT = 2;
    int SET_NULL = 3;
    int SET_DEFAULT = 4;
    int CASCADE = 5;

    Class<?> entity();
    String[] parentColumns();
    String[] childColumns();
    int onDelete() default NO_ACTION;
    int onUpdate() default NO_ACTION;
    boolean deferred() default false;
}
