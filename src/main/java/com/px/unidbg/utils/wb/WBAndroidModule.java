package com.px.unidbg.utils.wb;

import com.github.unidbg.Emulator;
import com.github.unidbg.arm.Arm64Svc;
import com.github.unidbg.arm.ArmSvc;
import com.github.unidbg.arm.backend.BackendException;
import com.github.unidbg.arm.context.RegisterContext;
import com.github.unidbg.linux.android.dvm.DvmClass;
import com.github.unidbg.linux.android.dvm.DvmObject;
import com.github.unidbg.linux.android.dvm.VM;
import com.github.unidbg.linux.android.dvm.api.Asset;
import com.github.unidbg.memory.MemoryBlock;
import com.github.unidbg.memory.SvcMemory;
import com.github.unidbg.pointer.UnidbgPointer;
import com.github.unidbg.virtualmodule.VirtualModule;
import com.github.unidbg.virtualmodule.android.AndroidModule;
import com.sun.jna.Pointer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * 重写unidbg 虚拟器 对于libandroid.so 加载的部分方法
 * 在原有基础上，新增部分检测（这里新增了传感器的处理）
 */
public class WBAndroidModule extends VirtualModule<VM> {

    private static final Log log = LogFactory.getLog(AndroidModule.class);

    public WBAndroidModule(Emulator<?> emulator, VM vm) {
        super(emulator, vm, "libandroid.so");
    }


    @Override
    protected void onInitialize(Emulator<?> emulator, VM vm, Map<String, UnidbgPointer> symbols) {
        boolean is64Bit = emulator.is64Bit();
        SvcMemory svcMemory = emulator.getSvcMemory();
        symbols.put("AAssetManager_fromJava", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return fromJava(emulator, vm);
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return fromJava(emulator, vm);
            }
        }));
        symbols.put("AAssetManager_open", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return open(emulator, vm);
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return open(emulator, vm);
            }
        }));
        symbols.put("AAsset_close", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return close(emulator, vm);
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return close(emulator, vm);
            }
        }));
        symbols.put("AAsset_getBuffer", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return getBuffer(emulator, vm);
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return getBuffer(emulator, vm);
            }
        }));
        symbols.put("AAsset_getLength", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return getLength(emulator, vm);
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return getLength(emulator, vm);
            }
        }));
        symbols.put("AAsset_read", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return read(emulator, vm);
            }
        }));
        // 新增传感器部分
        symbols.put("ASensorManager_getInstance", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensorManager_getInstance(emulator, vm);
            }
        }));
        symbols.put("ASensor_getName", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getName(emulator, vm);
            }
        }));
        symbols.put("ASensor_getVendor", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getVendor(emulator, vm);
            }
        }));
        symbols.put("ASensor_getType", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getType(emulator, vm);
            }
        }));
        symbols.put("ASensor_getResolution", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getResolution(emulator, vm);
            }
        }));
        symbols.put("ASensor_getMinDelay", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getMinDelay(emulator, vm);
            }
        }));
        symbols.put("ASensor_getFifoMaxEventCount", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getFifoMaxEventCount(emulator, vm);
            }
        }));
        symbols.put("ASensor_getFifoReservedEventCount", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getFifoReservedEventCount(emulator, vm);
            }
        }));
        symbols.put("ASensor_getStringType", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getStringType(emulator, vm);
            }
        }));
        symbols.put("ASensor_getReportingMode", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_getReportingMode(emulator, vm);
            }
        }));

        symbols.put("ASensor_isWakeUpSensor", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensor_isWakeUpSensor(emulator, vm);
            }
        }));

        symbols.put("ASensorManager_getSensorList", svcMemory.registerSvc(is64Bit ? new Arm64Svc() {
            @Override
            public long handle(Emulator<?> emulator) {
                throw new BackendException();
            }
        } : new ArmSvc() {
            @Override
            public long handle(Emulator<?> emulator) {
                return ASensorManager_getSensorList(emulator, vm);
            }
        }));
    }

    private static long fromJava(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        Pointer env = context.getPointerArg(0);
        UnidbgPointer assetManager = context.getPointerArg(1);
        DvmObject<?> obj = vm.getObject(assetManager.toIntPeer());
        if (log.isDebugEnabled()) {
            log.debug("AAssetManager_fromJava env=" + env + ", assetManager=" + obj.getObjectType() + ", LR=" + context.getLRPointer());
        }
        return assetManager.peer;
    }

    private static long open(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        Pointer amgr = context.getPointerArg(0);
        String filename = context.getPointerArg(1).getString(0);
        int mode = context.getIntArg(2);
        if (log.isDebugEnabled()) {
            log.debug("AAssetManager_open amgr=" + amgr + ", filename=" + filename + ", mode=" + mode + ", LR=" + context.getLRPointer());
        }
        final int AASSET_MODE_UNKNOWN = 0;
        final int AASSET_MODE_RANDOM = 1;
        final int AASSET_MODE_STREAMING = 2;
        final int AASSET_MODE_BUFFER = 3;
        if (mode == AASSET_MODE_STREAMING || AASSET_MODE_BUFFER == mode ||
                mode == AASSET_MODE_UNKNOWN || mode == AASSET_MODE_RANDOM) {
            byte[] data = vm.openAsset(filename);
            if (data == null) {
                return 0L;
            }
            Asset asset = new Asset(vm, filename);
            asset.open(emulator, data);
            return vm.addLocalObject(asset);
        }
        throw new BackendException("filename=" + filename + ", mode=" + mode + ", LR=" + context.getLRPointer());
    }

    private static long close(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer pointer = context.getPointerArg(0);
        Asset asset = vm.getObject(pointer.toIntPeer());
        asset.close();
        if (log.isDebugEnabled()) {
            log.debug("AAsset_close pointer=" + pointer + ", LR=" + context.getLRPointer());
        }
        return 0;
    }

    private static long getBuffer(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer pointer = context.getPointerArg(0);
        Asset asset = vm.getObject(pointer.toIntPeer());
        UnidbgPointer buffer = asset.getBuffer();
        if (log.isDebugEnabled()) {
            log.debug("AAsset_getBuffer pointer=" + pointer + ", buffer=" + buffer + ", LR=" + context.getLRPointer());
        }
        return buffer.peer;
    }

    private static long getLength(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer pointer = context.getPointerArg(0);
        Asset asset = vm.getObject(pointer.toIntPeer());
        int length = asset.getLength();
        if (log.isDebugEnabled()) {
            log.debug("AAsset_getLength pointer=" + pointer + ", length=" + length + ", LR=" + context.getLRPointer());
        }
        return length;
    }

    private static long read(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer pointer = context.getPointerArg(0);
        Pointer buf = context.getPointerArg(1);
        int count = context.getIntArg(2);
        Asset asset = vm.getObject(pointer.toIntPeer());
        byte[] bytes = asset.read(count);
        if (log.isDebugEnabled()) {
            log.debug("AAsset_read pointer=" + pointer + ", buf=" + buf + ", count=" + count + ", LR=" + context.getLRPointer());
        }
        buf.write(0, bytes, 0, bytes.length);
        return bytes.length;
    }

    private static long ASensorManager_getInstance(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        DvmClass SensorManager = vm.resolveClass("android/hardware/SensorManager");
        DvmObject<?> mpSensorManager = SensorManager.newObject(null);
        if (log.isDebugEnabled()) {
            log.debug("ASensorManager_getInstance newInstance=" + mpSensorManager +  ", LR=" + context.getLRPointer());
        }
        return mpSensorManager.hashCode();
    }

    private static long ASensor_getName(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        UnidbgPointer namePtr = sensor.getPointer(0);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getName sensor Address=" + sensor +  ", name=" + namePtr.getString(0) +", LR=" + context.getLRPointer());
        }
        return namePtr.peer;
    }


    private static long ASensor_getVendor(Emulator<?> emulator, VM vm) {

        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        UnidbgPointer VendorPtr = sensor.getPointer(4);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", Vendor=" + VendorPtr.getString(0) +", LR=" + context.getLRPointer());
        }
        return VendorPtr.peer;
    }


    private static long ASensor_getType(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        System.out.println(sensor);

        int Type = sensor.getInt(8);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", Type=" + Type +", LR=" + context.getLRPointer());
        }

        return Type;
    }


    private static Long ASensor_getResolution(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        float Resolution = sensor.getFloat(12);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", Resolution=" + Resolution +", LR=" + context.getLRPointer());
        }
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(Resolution);
        buffer.flip();
        return (buffer.getInt() & 0xffffffffL);
    }

    private static long ASensor_getMinDelay(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        int MinDelay = sensor.getInt(16);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", MinDelay=" + MinDelay +", LR=" + context.getLRPointer());
        }
        return MinDelay;
    }

    private static long ASensor_getFifoMaxEventCount(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        int FifoMaxEventCount = sensor.getInt(20);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", FifoMaxEventCount=" + FifoMaxEventCount +", LR=" + context.getLRPointer());
        }
        return FifoMaxEventCount;
    }


    private static long ASensor_getFifoReservedEventCount(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        int FifoReservedEventCount = sensor.getInt(24);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", FifoReservedEventCount=" + FifoReservedEventCount +", LR=" + context.getLRPointer());
        }
        return FifoReservedEventCount;
    }

    private static long ASensor_getStringType(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        UnidbgPointer StringType = sensor.getPointer(28);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", StringType=" + StringType.getString(0) +", LR=" + context.getLRPointer());
        }
        return StringType.peer;
    }


    private static long ASensor_getReportingMode(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        int ReportingMode = sensor.getInt(32);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", ReportingMode=" + ReportingMode +", LR=" + context.getLRPointer());
        }
        return ReportingMode;
    }

    private static long ASensor_isWakeUpSensor(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();
        UnidbgPointer sensor = context.getPointerArg(0);
        int isWakeUpSensor = sensor.getInt(36);
        if (log.isDebugEnabled()) {
            log.debug("ASensor_getVendor sensor Address=" + sensor +  ", isWakeUpSensor=" + isWakeUpSensor +", LR=" + context.getLRPointer());
        }
        return isWakeUpSensor;
    }


    private static long ASensorManager_getSensorList(Emulator<?> emulator, VM vm) {
        RegisterContext context = emulator.getContext();

        Pointer listptr = context.getPointerArg(1);

        Pointer pointer = allocateMemoryBlock(emulator, 40);

        String name = "BMI160 accelerometer";
        Pointer namePointer = emulator.getMemory().malloc(name.length() + 1, true).getPointer();
        namePointer.setString(0, name);
        pointer.setPointer(0, namePointer);

        String Vendor = "Bosch";
        Pointer VendorPointer = emulator.getMemory().malloc(Vendor.length() + 1, true).getPointer();
        VendorPointer.setString(0, Vendor);
        pointer.setPointer(4, VendorPointer); // ？

        int type = 1;
        pointer.setInt(8, type);

        float mResolution = 0.0047884034F;
        pointer.setFloat(12, mResolution);

        int MinDelay = 2500;
        pointer.setInt(16, MinDelay);

        int FifoMaxEventCount = 5736;
        pointer.setInt(20, FifoMaxEventCount);

        int FifoReservedEventCount = 3000;
        pointer.setInt(24, FifoReservedEventCount);

        String StringType = "android.sensor.accelerometer";
        Pointer StringTypePointer = emulator.getMemory().malloc(StringType.length() + 1, true).getPointer();
        StringTypePointer.setString(0, StringType);
        pointer.setPointer(28, StringTypePointer);

        int ReportingMode = 0;
        pointer.setInt(32, ReportingMode);

        int isWakeUpSensor = 0;
        pointer.setInt(36, isWakeUpSensor);

        Pointer pointerstub = allocateMemoryBlock(emulator, 40);

        listptr.setPointer(0, pointerstub);
        pointerstub.setPointer(0, pointer);

        return 1;
    }

    protected static UnidbgPointer allocateMemoryBlock(Emulator<?> emulator, int length) {

        MemoryBlock memoryBlock = emulator.getMemory().malloc(length, true);
        return memoryBlock.getPointer();
    }


}
