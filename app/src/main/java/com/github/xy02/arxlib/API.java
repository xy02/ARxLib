package com.github.xy02.arxlib;

import android.content.Context;

import io.reactivex.Observable;

/**
 * Created by xy on 18-5-7.
 */

public interface API {
    Observable<Object> invoke(Context context);
}
