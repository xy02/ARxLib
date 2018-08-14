package com.github.xy02.arxlib;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.annotations.Nullable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

/**
 * Created by xy on 18-5-7.
 */

public class ARxClient {
    private static String tag = ARxClient.class.getSimpleName();
    private static String ERR_CONNECT = "ERR_CONNECT";

    private Intent intent;
    private Context context;

    public ARxClient(Context context, String packageName, String action) {
        this.intent = new Intent(action);
        this.intent.setPackage(packageName);
        this.context = context;
    }

    private class CbData {
        public String id;
        private String message;

        public CbData(String id, String message) {
            this.id = id;
            this.message = message;
        }
    }

    private PublishSubject<CbData> onNextSubject = PublishSubject.create();
    private PublishSubject<CbData> onFinishSubject = PublishSubject.create();

    private IObservableEmitter.Stub serviceCb = new IObservableEmitter.Stub() {
        @Override
        public void onNext(String id, String jsonData) throws RemoteException {
            onNextSubject.onNext(new CbData(id, jsonData));
        }

        @Override
        public void onError(String id, String errMessage) throws RemoteException {
            onFinishSubject.onNext(new CbData(id, errMessage));
        }

        @Override
        public void onComplete(String id) throws RemoteException {
            onFinishSubject.onNext(new CbData(id, null));
        }
    };


    public <T> Observable<T> callARxService(String apiName, @Nullable Object reqData, Class<T> observableDataClazz) {
        return Observable.create(e -> {
            ServiceConnection conn = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    Log.i(tag, "onServiceConnected, tid:" + Thread.currentThread().getId());
                    IARxService arxService = IARxService.Stub.asInterface(service);
                    String id = UUID.randomUUID().toString();
                    Disposable d = onNextSubject
                            .filter(it -> it.id.equals(id))
                            .map(it -> JSON.parseObject(it.message, observableDataClazz))
                            .doOnNext(e::onNext)
                            .doOnError(e::tryOnError)
                            .subscribe();
                    onFinishSubject
                            .filter(it -> it.id.equals(id))
                            .take(1)
                            .doOnNext(it -> d.dispose())
                            .doOnNext(it -> {
                                if (it.message != null)
                                    e.tryOnError(new Exception(it.message));
                                else
                                    e.onComplete();
                            })
                            .doOnNext(it -> context.unbindService(this))
                            .subscribe();
                    String apiJSONData = null;
                    if (reqData != null) {
                        apiJSONData = JSON.toJSONString(reqData);
                    }
                    try {
                        arxService.call(id, apiName, apiJSONData, serviceCb);
                    } catch (RemoteException ex) {
                        e.tryOnError(ex);
                    }
                }


                @Override
                public void onServiceDisconnected(ComponentName name) {
                    Log.i(tag, "onServiceDisconnected, tid:" + Thread.currentThread().getId());
                    e.onComplete();
                }
            };
            boolean connected = context.bindService(intent, conn, Context.BIND_AUTO_CREATE);
            if (!connected) {
                Log.e(tag, ERR_CONNECT);
                e.tryOnError(new Exception(ERR_CONNECT));
            }
        });
    }

}
