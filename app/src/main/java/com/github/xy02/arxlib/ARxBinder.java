package com.github.xy02.arxlib;

import android.content.Context;
import android.os.RemoteException;

import com.alibaba.fastjson.JSON;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by xy on 18-5-7.
 */

public class ARxBinder extends IARxService.Stub {
    private Context context;

    public ARxBinder(Context context) {
        this.context = context;
    }

    @Override
    public void call(String id, String apiType, String apiJsonData, IObservableEmitter e) throws RemoteException {
        Observable.fromCallable(() -> Class.forName(apiType))
                .map(it -> JSON.parseObject(getJSONData(apiJsonData), it))
                .map(it -> (API) it)
                .flatMap(it -> it.invoke(context))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext(it -> e.onNext(id, JSON.toJSONString(it)))
                .doOnComplete(() -> e.onComplete(id))
                .doOnError(it -> e.onError(id, it.toString() + " @" + context.getPackageName()))
//                .doOnError(Throwable::printStackTrace)
                .subscribe(x->{},Throwable::printStackTrace);
    }

    private String getJSONData(String data) {
        if (data == null || data.isEmpty())
            return "{}";
        return data;
    }

}
