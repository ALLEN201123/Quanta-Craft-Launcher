package com.tungsten.hmclpe.utils;

import android.os.Build;

/**
 * This class aims at providing a simple and easy way to deal with the device architecture.
 */
public class Architecture {
	public static int UNSUPPORTED_ARCH = -1;
	public static int ARCH_ARM64 = 0x1;
	public static int ARCH_ARM = 0x2;
	public static int ARCH_X86 = 0x4;
	public static int ARCH_X86_64 = 0x8;

	/**
	 * Returns the architecture the app is actually RUNNING with (from the installed ABI).
	 *
	 * Emulators are the trap here: the device may be x86_64 while the APK is installed as
	 * armeabi-v7a (translated). Selecting a Java runtime from the device architecture would
	 * make every JVM library fail to dlopen ("libGLESv2_CM.dll not found" was exactly that).
	 *
	 * @return ARCH_ARM || ARCH_ARM64 || ARCH_X86 || ARCH_X86_64
	 */
	public static int getInstalledAbiArchitecture(){
		try {
			Class<?> globals = Class.forName("android.app.AppGlobals");
			Object app = globals.getMethod("getInitialApplication").invoke(null);
			if (app != null) {
				Object info = app.getClass().getMethod("getApplicationInfo").invoke(app);
				String abi = (String) info.getClass().getField("primaryCpuAbi").get(info);
				int arch = archAsInt(abi);
				if (arch != UNSUPPORTED_ARCH) return arch;
			}
		} catch (Throwable ignored) {
			// Not on Android (desktop/test) or too early in startup: fall back below.
		}
		try {
			Class<?> process = Class.forName("android.os.Process");
			boolean is64 = (Boolean) process.getMethod("is64Bit").invoke(null);
			return isx86Device() ? (is64 ? ARCH_X86_64 : ARCH_X86)
					: (is64 ? ARCH_ARM64 : ARCH_ARM);
		} catch (Throwable ignored) {
		}
		return getDeviceArchitecture();
	}

	/**
	 * Tell us if the device supports 64 bits architecture
	 * @return If the device supports 64 bits architecture
	 */
	public static boolean is64BitsDevice(){
		return Build.SUPPORTED_64_BIT_ABIS.length != 0;
	}

	/**
	 * Tell us if the device supports 32 bits architecture
	 * Note, that a 64 bits device won't be reported as supporting 32 bits.
	 * @return If the device supports 32 bits architecture
	 */
	public static boolean is32BitsDevice(){
		return !is64BitsDevice();
	}

	/**
	 * Tells the device supported architecture.
	 * Since mips(/64) has been phased out long ago, is isn't checked here.
	 *
	 * @return ARCH_ARM || ARCH_ARM64 || ARCH_X86 || ARCH_86_64
	 */
	public static int getDeviceArchitecture(){
		if(isx86Device()){
			return is64BitsDevice() ? ARCH_X86_64 : ARCH_X86;
		}
		return is64BitsDevice() ? ARCH_ARM64 : ARCH_ARM;
	}

	/**
	 * Tell is the device is based on an x86 processor.
	 * It doesn't tell if the device is 64 or 32 bits.
	 * @return Whether or not the device is x86 based.
	 */
	public static boolean isx86Device(){
		//We check the whole range of supported ABIs,
		//Since asus zenfones can place arm before their native instruction set.
		String[] ABI = is64BitsDevice() ? Build.SUPPORTED_64_BIT_ABIS : Build.SUPPORTED_32_BIT_ABIS;
		int comparedArch = is64BitsDevice() ? ARCH_X86_64 : ARCH_X86;
		for (String str : ABI) {
			if (archAsInt(str) == comparedArch) return true;
		}
		return false;
	}

	/**
	 * Tell is the device is based on an arm processor.
	 * It doesn't tell if the device is 64 or 32 bits.
	 * @return Whether or not the device is arm based.
	 */
	public static boolean isArmDevice(){
		return !isx86Device();
	}


	/**
	 * Convert an architecture from a String to an int.
	 * @param arch The architecture as a String
	 * @return The architecture as an int, can be UNSUPPORTED_ARCH if unknown.
	 */
	public static int archAsInt(String arch){
		arch = arch.toLowerCase().trim().replace(" ", "");
		if(arch.contains("arm64") || arch.equals("aarch64")) return ARCH_ARM64;
		if(arch.contains("arm") || arch.equals("aarch32")) return ARCH_ARM;
		if(arch.contains("x86_64") || arch.contains("amd64")) return ARCH_X86_64;
		if(arch.contains("x86") || (arch.startsWith("i") && arch.endsWith("86"))) return ARCH_X86;
		//Shouldn't happen
		return UNSUPPORTED_ARCH;
	}

	/**
	 * Convert to a string an architecture.
	 * @param arch The architecture as an int.
	 * @return "arm64" || "arm" || "x86_64" || "x86" || "UNSUPPORTED_ARCH"
	 */
	public static String archAsString(int arch){
		if(arch == ARCH_ARM64) return "arm64";
		if(arch == ARCH_ARM) return "arm";
		if(arch == ARCH_X86_64) return "x86_64";
		if(arch == ARCH_X86) return "x86";
		return "UNSUPPORTED_ARCH";
	}


	/** 运行时位数模式：0=自动、1=强制 64 位、2=强制 32 位 */
	private static int bitMode = 0;

	public static void setBitMode(int mode) {
		bitMode = mode;
	}

	public static boolean isProcess64Bit() {
		try {
			Class<?> process = Class.forName("android.os.Process");
			return (Boolean) process.getMethod("is64Bit").invoke(null);
		} catch (Throwable t) {
			return true;
		}
	}

	/** 实际可用的运行时架构：自动跟随应用 ABI；强制模式在进程位数不匹配时回退。 */
	public static int getRuntimeArchitecture() {
		int abi = getInstalledAbiArchitecture();
		boolean x86 = abi == ARCH_X86 || abi == ARCH_X86_64;
		if (bitMode == 1 && isProcess64Bit()) {
			return x86 ? ARCH_X86_64 : ARCH_ARM64;
		}
		if (bitMode == 2 && !isProcess64Bit()) {
			return x86 ? ARCH_X86 : ARCH_ARM;
		}
		return abi;
	}
}
