// IObservableEmitter.aidl
package com.github.xy02.arxlib;

// Declare any non-default types here with import statements

interface IObservableEmitter {
    /**
     * like onNext in rxjava
     */
    void onNext(String id,  String jsonData);
    void onError(String id, String errMessage);
    void onComplete(String id);
}
