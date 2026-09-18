package com.qcl.launcher.utils.platform;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/* loaded from: classes2.dex */
public enum Architecture {
    X86(Bits.BIT_32, "x86"),
    X86_64(Bits.BIT_64, "x86-64"),
    IA32(Bits.BIT_32, "IA-32"),
    IA64(Bits.BIT_64, "IA-64"),
    SPARC(Bits.BIT_32),
    SPARCV9(Bits.BIT_64, "SPARC V9"),
    ARM32(Bits.BIT_32),
    ARM64(Bits.BIT_64),
    MIPS(Bits.BIT_32),
    MIPS64(Bits.BIT_64),
    MIPSEL(Bits.BIT_32, "MIPSel"),
    MIPS64EL(Bits.BIT_64, "MIPS64el"),
    PPC(Bits.BIT_32, "PowerPC"),
    PPC64(Bits.BIT_64, "PowerPC-64"),
    PPCLE(Bits.BIT_32, "PowerPC (Little-Endian)"),
    PPC64LE(Bits.BIT_64, "PowerPC-64 (Little-Endian)"),
    S390(Bits.BIT_32),
    S390X(Bits.BIT_64, "S390x"),
    RISCV(Bits.BIT_64, "RISC-V"),
    UNKNOWN(Bits.UNKNOWN, "Unknown");

    public static final Architecture CURRENT_ARCH;
    public static final String CURRENT_ARCH_NAME;
    public static final Architecture SYSTEM_ARCH;
    public static final String SYSTEM_ARCH_NAME;
    private final Bits bits;
    private final String checkedName;
    private final String displayName;

    static {
        int indexOf;
        String property = System.getProperty("os.arch");
        CURRENT_ARCH_NAME = property;
        CURRENT_ARCH = parseArchName(property);
        String str = null;
        if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            String str2 = System.getenv("PROCESSOR_IDENTIFIER");
            if (str2 != null && (indexOf = str2.indexOf(32)) > 0) {
                str = str2.substring(0, indexOf);
            }
        } else {
            try {
                Process exec = Runtime.getRuntime().exec(new String[]{"/bin/uname", "-m"});
                if (exec.waitFor(3L, TimeUnit.SECONDS)) {
                    try {
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(exec.getInputStream(), OperatingSystem.NATIVE_CHARSET));
                        try {
                            str = bufferedReader.readLine().trim();
                            bufferedReader.close();
                        } catch (Throwable th) {
                            try {
                                bufferedReader.close();
                            } catch (Throwable th2) {
                                th.addSuppressed(th2);
                            }
                            throw th;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Throwable unused) {
            }
        }
        Architecture parseArchName = parseArchName(str);
        if (parseArchName == UNKNOWN) {
            SYSTEM_ARCH_NAME = CURRENT_ARCH_NAME;
            SYSTEM_ARCH = CURRENT_ARCH;
        } else {
            SYSTEM_ARCH_NAME = str;
            SYSTEM_ARCH = parseArchName;
        }
    }

    Architecture(Bits bits) {
        this.checkedName = toString().toLowerCase(Locale.ROOT);
        this.displayName = toString();
        this.bits = bits;
    }

    Architecture(Bits bits, String str) {
        this.checkedName = toString().toLowerCase(Locale.ROOT);
        this.displayName = str;
        this.bits = bits;
    }

    Architecture(Bits bits, String str, String str2) {
        this.checkedName = str2;
        this.displayName = str;
        this.bits = bits;
    }

    public Bits getBits() {
        return this.bits;
    }

    public String getCheckedName() {
        return this.checkedName;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isX86() {
        return this == X86 || this == X86_64;
    }

    public static Architecture parseArchName(String str) {
        if (str == null) {
            return UNKNOWN;
        }
        String lowerCase = str.trim().toLowerCase(Locale.ROOT);
        lowerCase.hashCode();
        char c = 65535;
        switch (lowerCase.hashCode()) {
            case -2011719692:
                if (lowerCase.equals("sparc32")) {
                    c = 0;
                    break;
                }
                break;
            case -2011719597:
                if (lowerCase.equals("sparc64")) {
                    c = 1;
                    break;
                }
                break;
            case -2011717608:
                if (lowerCase.equals("sparcv9")) {
                    c = 2;
                    break;
                }
                break;
            case -1358309465:
                if (lowerCase.equals("itanium64")) {
                    c = 3;
                    break;
                }
                break;
            case -1294355355:
                if (lowerCase.equals("mips32el")) {
                    c = 4;
                    break;
                }
                break;
            case -1294264060:
                if (lowerCase.equals("mips64el")) {
                    c = 5;
                    break;
                }
                break;
            case -1221096139:
                if (lowerCase.equals("aarch64")) {
                    c = 6;
                    break;
                }
                break;
            case -1073971394:
                if (lowerCase.equals("mips32")) {
                    c = 7;
                    break;
                }
                break;
            case -1073971299:
                if (lowerCase.equals("mips64")) {
                    c = '\b';
                    break;
                }
                break;
            case -1073969786:
                if (lowerCase.equals("mipsel")) {
                    c = '\t';
                    break;
                }
                break;
            case -930751760:
                if (lowerCase.equals("risc-v")) {
                    c = '\n';
                    break;
                }
                break;
            case -806098410:
                if (lowerCase.equals("x86-32")) {
                    c = 11;
                    break;
                }
                break;
            case -806098315:
                if (lowerCase.equals("x86-64")) {
                    c = '\f';
                    break;
                }
                break;
            case -806050360:
                if (lowerCase.equals("x86_32")) {
                    c = '\r';
                    break;
                }
                break;
            case -806050265:
                if (lowerCase.equals("x86_64")) {
                    c = 14;
                    break;
                }
                break;
            case -387946056:
                if (lowerCase.equals("powerpc")) {
                    c = 15;
                    break;
                }
                break;
            case -379338501:
                if (lowerCase.equals("ppc32le")) {
                    c = 16;
                    break;
                }
                break;
            case -379247206:
                if (lowerCase.equals("ppc64le")) {
                    c = 17;
                    break;
                }
                break;
            case 96860:
                if (lowerCase.equals("arm")) {
                    c = 18;
                    break;
                }
                break;
            case 111203:
                if (lowerCase.equals("ppc")) {
                    c = 19;
                    break;
                }
                break;
            case 116951:
                if (lowerCase.equals("x32")) {
                    c = 20;
                    break;
                }
                break;
            case 117046:
                if (lowerCase.equals("x64")) {
                    c = 21;
                    break;
                }
                break;
            case 117110:
                if (lowerCase.equals("x86")) {
                    c = 22;
                    break;
                }
                break;
            case 3178856:
                if (lowerCase.equals("i386")) {
                    c = 23;
                    break;
                }
                break;
            case 3179817:
                if (lowerCase.equals("i486")) {
                    c = 24;
                    break;
                }
                break;
            case 3180778:
                if (lowerCase.equals("i586")) {
                    c = 25;
                    break;
                }
                break;
            case 3181739:
                if (lowerCase.equals("i686")) {
                    c = 26;
                    break;
                }
                break;
            case 3222903:
                if (lowerCase.equals("ia32")) {
                    c = 27;
                    break;
                }
                break;
            case 3222998:
                if (lowerCase.equals("ia64")) {
                    c = 28;
                    break;
                }
                break;
            case 3351711:
                if (lowerCase.equals("mips")) {
                    c = 29;
                    break;
                }
                break;
            case 3476791:
                if (lowerCase.equals("s390")) {
                    c = 30;
                    break;
                }
                break;
            case 92926582:
                if (lowerCase.equals("amd64")) {
                    c = 31;
                    break;
                }
                break;
            case 93084091:
                if (lowerCase.equals("arm32")) {
                    c = ' ';
                    break;
                }
                break;
            case 93084186:
                if (lowerCase.equals("arm64")) {
                    c = '!';
                    break;
                }
                break;
            case 96576462:
                if (lowerCase.equals("em64t")) {
                    c = '\"';
                    break;
                }
                break;
            case 98693466:
                if (lowerCase.equals("i86pc")) {
                    c = '#';
                    break;
                }
                break;
            case 99910094:
                if (lowerCase.equals("ia32e")) {
                    c = '$';
                    break;
                }
                break;
            case 99913048:
                if (lowerCase.equals("ia64n")) {
                    c = '%';
                    break;
                }
                break;
            case 99913057:
                if (lowerCase.equals("ia64w")) {
                    c = '&';
                    break;
                }
                break;
            case 106867714:
                if (lowerCase.equals("ppc32")) {
                    c = '\'';
                    break;
                }
                break;
            case 106867809:
                if (lowerCase.equals("ppc64")) {
                    c = '(';
                    break;
                }
                break;
            case 106869532:
                if (lowerCase.equals("ppcle")) {
                    c = ')';
                    break;
                }
                break;
            case 107780641:
                if (lowerCase.equals("s390x")) {
                    c = '*';
                    break;
                }
                break;
            case 108523151:
                if (lowerCase.equals("riscv")) {
                    c = '+';
                    break;
                }
                break;
            case 109638357:
                if (lowerCase.equals("sparc")) {
                    c = ',';
                    break;
                }
                break;
            case 112544341:
                if (lowerCase.equals("x8632")) {
                    c = '-';
                    break;
                }
                break;
            case 112544436:
                if (lowerCase.equals("x8664")) {
                    c = '.';
                    break;
                }
                break;
            case 845996567:
                if (lowerCase.equals("powerpc32")) {
                    c = '/';
                    break;
                }
                break;
            case 845996662:
                if (lowerCase.equals("powerpc64")) {
                    c = '0';
                    break;
                }
                break;
            case 845998385:
                if (lowerCase.equals("powerpcle")) {
                    c = '1';
                    break;
                }
                break;
            case 1253885392:
                if (lowerCase.equals("powerpc32le")) {
                    c = '2';
                    break;
                }
                break;
            case 1253976687:
                if (lowerCase.equals("powerpc64le")) {
                    c = '3';
                    break;
                }
                break;
        }
        switch (c) {
            case 0:
            case ',':
                return SPARC;
            case 1:
            case 2:
                return SPARCV9;
            case 3:
            case 28:
            case '&':
                return IA64;
            case 4:
            case '\t':
                return MIPSEL;
            case 5:
                return MIPS64EL;
            case 6:
            case '!':
                return ARM64;
            case 7:
            case 29:
                return MIPS;
            case '\b':
                return MIPS64;
            case '\n':
            case '+':
                return RISCV;
            case 11:
            case '\r':
            case 20:
            case 22:
            case 23:
            case 24:
            case 25:
            case 26:
            case 27:
            case '#':
            case '-':
                return X86;
            case '\f':
            case 14:
            case 21:
            case 31:
            case '\"':
            case '$':
            case '.':
                return X86_64;
            case 15:
            case 19:
            case '\'':
            case '/':
                return PPC;
            case 16:
            case ')':
            case '1':
            case '2':
                return PPCLE;
            case 17:
            case '3':
                return PPC64LE;
            case 18:
            case ' ':
                return ARM32;
            case 30:
                return S390;
            case '%':
                return IA32;
            case '(':
            case '0':
                return "little".equals(System.getProperty("sun.cpu.endian")) ? PPC64LE : PPC64;
            case '*':
                return S390X;
            default:
                if (lowerCase.startsWith("armv7")) {
                    return ARM32;
                }
                if (lowerCase.startsWith("armv8") || lowerCase.startsWith("armv9")) {
                    return ARM64;
                }
                return UNKNOWN;
        }
    }
}
