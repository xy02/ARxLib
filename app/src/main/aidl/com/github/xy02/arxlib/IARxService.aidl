// IARxService.aidl
package com.github.xy02.arxlib;

import com.github.xy02.arxlib.IObservableEmitter;

// Declare any non-default types here with import statements

interface IARxService {
    /**
     * common interface
     */
    void call(String id,String apiType, String apiJsonData,IObservableEmitter e);
}
