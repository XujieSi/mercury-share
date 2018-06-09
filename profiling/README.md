
Our profiling implementation is based on the DexHunter project: https://github.com/zyq8709/DexHunter

Please set up DexHunter according to their instructions. 

We share some notes helpful for Android hacking:

## Build Android

1. Build and run current infrastructure
	- http://source.android.com/source/building-running.html
	- http://source.android.com/source/building-devices.html
	- http://m8051.blogspot.com/2013/08/building-android-from-source-for-nexus.html
	- http://forum.xda-developers.com/showthread.php?t=2163974
	- http://forum.xda-developers.com/showthread.php?p=46509927
	- http://javigon.com/2012/09/04/mount-unmount-android-system-images/

2. Build android-x86:
	- http://www.android-x86.org/getsourcecode

## JNI (Java to Native)

 The field storing native function pointer is Method::nativeFunc. It has the following signature:
```
 typedef void (*DalvikBridgeFunc)(const u4* args, JValue* pResult, const Method* method, struct Thread* self);

 typedef void (*DalvikNativeFunc)(const u4* args, JValue* pResult);
```

The function pointer is invoked in 
`GOTO_TARGET(invokeMethod, bool methodCallRange, const Method* _methodToCall, u2 count, u2 regs)` of gotoTargets.cpp.


Let’s see where it is set:

1. A special case in reflection: 
	- `dalvik/vm/reflect.cpp`
	- `./reflect/Proxy.cpp:    meth->nativeFunc = proxyConstructor;` in createConstructor
	- `./reflect/Proxy.cpp:    dstMeth->nativeFunc = proxyInvoker;` in createHandlerMethod
	- Proxy class is a special class used in reflection: http://docs.oracle.com/javase/6/docs/api/java/lang/reflect/Proxy.html


2. When a class is loaded from dex file. dalvik/vm/oo/Class.cpp (the most common case for JNI)
	- ./oo/Class.cpp:            meth->nativeFunc = dvmResolveNativeMethod; in loadMethodFromDex.

3. The first time the native method is invoked, dvmResolveNativeMethod will be first called as it is set in b. And then it will be replaced with the actual method.
	- Native.cpp: dvmResolveNativeMethod -> dvmSetNativeMethod (this is for internal native methods)
	- Native.cpp: dvmResolveNativeMethod -> dvmUseJNIBridge->dvmSetNativeFunc (this is for app-defined native methods)

4. Stub implementation.
	- dalvik/vm/oo/Class.cpp
	- ./oo/Class.cpp: meth->nativeFunc = (DalvikBridgeFunc)dvmAbstractMethodStub; in insertMethodStubs

5. Registered by the app code, in Jni.cpp
	- RegisterNatives->dvmRegisterJNIMethod->dvmUseJNIBridge->dvmSetNativeFunc


## QEMU

qemu-kvm is deprecated at some point. Now the trunk of qemu has built-in support for kvm. The qemu in debian 6 repo is highly outdated. 

1. To install the newest qemu:

	- install kvm : sudo apt-get install kvm
	- build and install qemu: http://www.linuxfromscratch.org/blfs/view/svn/postlfs/qemu.html

2. Start Andoird-x86 with QEMU (use qemu-system-x86_64 or qemu directly):

	- qemu-img create android.img 8G
	- sudo qemu-system-x86_64 -vga std -enable-kvm -hda ./android.img -vnc :0 -cdrom android_x86.iso -boot d -m 2048 -usbdevice tablet
	- This will start Android-x86 from live cd. 
	Note: `-usbdevice tablet` solves the mouse line up problem, `-vga std` fixes the problem that android doesn’t start gui

3. Connecting to the KVM from your machine:
	- your machine terminal 1: ssh server_host  -L 5900:localhost:5900
	- your machine terminal 2: vncviewer localhost:5900 (tight vnc is recommended)

4. Setup
	- Install android-x86 to the image
	- Next time, when you want to use:
	`sudo qemu-system-x86_64 -vga std -enable-kvm -hda ./android.img -vnc :0 -m 2048 -usbdevice tablet -smp 8`
	This will enable android-x86 to boot from the disk. Note: -smp 8 emulates 8 cores

5. To connect to the virtual machine with adb:
	-  `attach -net nic -net user,hostfwd=tcp::4444-:5555` to qemu command
	- your machine terminal 1: ssh server_host -L 4444:localhost:4444
	- your machine terminal 2: adb connect 127.0.0.1:4444
	- If Android-x86 shows a black screen, try to use `adb shell input keyevent 26`

## Other resources

1. ramdisk:http://www.jamescoyle.net/how-to/943-create-a-ram-disk-in-linux

2. What if there’s fork in dalvik VM? Fork will potentially make pin in multiple processes write to the same file. (I read online, dalvik doesn’t spawn new processes. Our problem in Java is that we turn on the instrumentation by package id and multiple processes might share the same package id)

3. Way to find main activity of an apk(needed for start pin instrumentation): http://voyager.wordpress.com/2013/10/03/finding-package-and-activity-of-android-apk-files/

4. Some app cannot run on emulator is not because they have arm library, but qemu has no gpu emulation.

5. system call number: https://android.googlesource.com/platform/prebuilts/gcc/linux-x86/arm/arm-linux-androideabi-4.6/+/ics-plus-aosp/share/gdb/syscalls/i386-linux.xml



