# ARX
It's a RxJava **Observable** wrapper for AIDL

## Usage
### Install
Step 1. Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```gradle
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```
Step 2. Add the dependency
```gradle
dependencies {
    implementation 'com.github.xy02:ARxLib:0.1.5'
}
```
### On server side 
Define a service class extends **android.app.Service**, 
and return a **ARxBinder** instance onBind.
```java
public class TestService extends Service {
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ARxBinder(this);
    }
}
```
Register this Service in **AndroidManifest.xml** and set **exported** = true.
```xml
<service android:name=".TestService" android:exported="true">
    <intent-filter>
        <action android:name="action.TEST_SERVICE"/>
    </intent-filter>
</service>
```
Define your API class extends **com.github.xy02.arxlib.API**   
***Note: The class require a default empty constructor*** 

java example:
```java
public class TestAPI implements API {
    public String data;
    @NotNull
    @Override
    public Observable<Object> invoke(@NotNull Context context) {
        //your logic here 
        return Observable
                    .interval(1, TimeUnit.SECONDS)
                    .map(aLong -> aLong + ":" + data);
    }
}
```
### On client side
java example:
```java
ARxClient client = new ARxClient(this, "server.package.name", "action.TEST_SERVICE");
client.callARxService("package.name.TestAPI", new TestRequest("some data"), String.class)
        .subscribe(it -> Log.d("test", "receive:" + it), it -> Log.e("test3", it.getMessage()));
```
```java
class TestRequest {

    public String data;

    private TestRequest(String data) {
        this.data = data;
    }
}
```
