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

    public Observable<String> callARxService(String apiName, @Nullable String reqJSONData) {
        return observableIARxService
                .flatMap(service->{
                    String id = UUID.randomUUID().toString();
                    service.call(id, apiName, reqJSONData, serviceCb);
                    return onNextSubject
                            .filter(it -> it.id.equals(id))
                            .map(it -> it.message)
                            .takeUntil(onFinishSubject
                                    .filter(it -> it.id.equals(id))
                            );
                });
    }

    public <T> Observable<T> callARxService(String apiName, @Nullable Object reqData, Class<T> observableDataClazz) {
        String apiJSONData = null;
        if (reqData != null) {
            apiJSONData = JSON.toJSONString(reqData);
        }
        return callARxService(apiName, apiJSONData)
                .map(it -> JSON.parseObject(it, observableDataClazz));
    }

    private class CbData {
         String id;
         String message;

         CbData(String id, String message) {
            this.id = id;
            this.message = message;
        }
    }

    private class ConnectionState{
        ServiceConnection conn;
        IBinder.DeathRecipient dr;
        IARxService service;
        void clear(){
            if (service!=null)
                service.asBinder().unlinkToDeath(dr,0);
            if(conn!= null)
                context.unbindService(conn);
        }
    }

    private Observable<IARxService> observableIARxService = Observable.<ConnectionState>create(emitter -> {
        ConnectionState state = new ConnectionState();
        state.dr = ()->{
            state.clear();
            emitter.tryOnError(new Exception("service:"+intent.getPackage()+":"+intent.getAction()+" died"));
        };
        state.conn = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                Log.i(tag, "onServiceConnected, tid:" + Thread.currentThread().getId());
                try {
                    service.linkToDeath(state.dr,0);
//                    arxService.call(id, apiName, reqJSONData, serviceCb);
                } catch (RemoteException ex) {
                    emitter.tryOnError(ex);
                    return;
                }
                state.service = IARxService.Stub.asInterface(service);
                emitter.onNext(state);
            }


            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(tag, "onServiceDisconnected, tid:" + Thread.currentThread().getId());
                emitter.onComplete();
            }
        };
        boolean connected = context.bindService(intent, state.conn, Context.BIND_AUTO_CREATE);
        if (!connected) {
            Log.e(tag, ERR_CONNECT);
            emitter.tryOnError(new Exception(ERR_CONNECT));
        }
    })
            .map(state-> state.service)
            .replay(1)
            .refCount();

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

}
