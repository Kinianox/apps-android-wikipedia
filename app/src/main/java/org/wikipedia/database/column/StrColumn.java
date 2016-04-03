package org.wikipedia.database.column;

import android.database.Cursor;
import android.support.annotation.NonNull;

public class StrColumn extends Column<String> {
    @Deprecated public StrColumn(@NonNull String name, @NonNull String type) {
        super(name, type);
    }

    public StrColumn(@NonNull String tbl, @NonNull String name, @NonNull String type) {
        super(tbl, name, type);
    }

    @Override
    public String val(@NonNull Cursor cursor) {
        return getString(cursor);
    }
}