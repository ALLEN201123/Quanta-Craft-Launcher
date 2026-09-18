package com.qcl.launcher.utils.versioning;

import com.qcl.launcher.utils.string.StringUtils;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.IntPredicate;

/* loaded from: classes2.dex */
public class VersionNumber implements Comparable<VersionNumber> {
    public static final Comparator<String> VERSION_COMPARATOR = Comparator.comparing(new Function() { // from class: com.qcl.launcher.utils.versioning.VersionNumber$$ExternalSyntheticLambda0
        @Override // java.util.function.Function
        public final Object apply(Object obj) {
            return VersionNumber.asVersion((String) obj);
        }
    });
    private String canonical;
    private ListItem items;
    private String value;

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public interface Item {
        public static final int INTEGER_ITEM = 0;
        public static final int LIST_ITEM = 2;
        public static final int STRING_ITEM = 1;

        int compareTo(Item item);

        int getType();

        boolean isNull();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public static /* synthetic */ boolean lambda$isIntVersionNumber$0(int i) {
        return i != 46 && (i < 48 || i > 57);
    }

    public static VersionNumber asVersion(String str) {
        Objects.requireNonNull(str);
        return new VersionNumber(str);
    }

    public static String normalize(String str) {
        return new VersionNumber(str).getCanonical();
    }

    public static boolean isIntVersionNumber(String str) {
        if (!str.chars().noneMatch(new IntPredicate() { // from class: com.qcl.launcher.utils.versioning.VersionNumber$$ExternalSyntheticLambda2
            @Override // java.util.function.IntPredicate
            public final boolean test(int i) {
                return VersionNumber.lambda$isIntVersionNumber$0(i);
            }
        }) || str.contains("..") || !StringUtils.isNotBlank(str)) {
            return false;
        }
        for (String str2 : str.split("\\.")) {
            if (str2.length() > 9) {
                return false;
            }
        }
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class IntegerItem implements Item {
        public static final IntegerItem ZERO = new IntegerItem();
        private final BigInteger value;

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public int getType() {
            return 0;
        }

        private IntegerItem() {
            this.value = BigInteger.ZERO;
        }

        IntegerItem(String str) {
            this.value = new BigInteger(str);
        }

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public boolean isNull() {
            return BigInteger.ZERO.equals(this.value);
        }

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public int compareTo(Item item) {
            if (item == null) {
                return !BigInteger.ZERO.equals(this.value) ? 1 : 0;
            }
            int type = item.getType();
            if (type == 0) {
                return this.value.compareTo(((IntegerItem) item).value);
            }
            if (type == 1 || type == 2) {
                return 1;
            }
            throw new RuntimeException("invalid item: " + item.getClass());
        }

        public String toString() {
            return this.value.toString();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class StringItem implements Item {
        private String value;

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public int getType() {
            return 1;
        }

        StringItem(String str) {
            this.value = str;
        }

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public boolean isNull() {
            return this.value.isEmpty();
        }

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public int compareTo(Item item) {
            if (item == null) {
                return 1;
            }
            int type = item.getType();
            if (type == 0) {
                return -1;
            }
            if (type == 1) {
                return this.value.compareTo(((StringItem) item).value);
            }
            if (type == 2) {
                return -1;
            }
            throw new RuntimeException("invalid item: " + item.getClass());
        }

        public String toString() {
            return this.value;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes2.dex */
    public static class ListItem extends ArrayList<Item> implements Item {
        Character separator;

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public int getType() {
            return 2;
        }

        public ListItem() {
        }

        public ListItem(char c) {
            this.separator = Character.valueOf(c);
        }

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public boolean isNull() {
            return size() == 0;
        }

        void normalize() {
            for (int size = size() - 1; size >= 0; size--) {
                Item item = get(size);
                if (item.isNull()) {
                    remove(size);
                } else if (!(item instanceof ListItem)) {
                    return;
                }
            }
        }

        @Override // com.qcl.launcher.utils.versioning.VersionNumber.Item
        public int compareTo(Item item) {
            int compareTo;
            if (item == null) {
                if (size() == 0) {
                    return 0;
                }
                return get(0).compareTo(null);
            }
            int type = item.getType();
            if (type == 0) {
                return -1;
            }
            if (type == 1) {
                return 1;
            }
            if (type == 2) {
                Iterator<Item> it = iterator();
                Iterator<Item> it2 = ((ListItem) item).iterator();
                do {
                    if (!it.hasNext() && !it2.hasNext()) {
                        return 0;
                    }
                    Item next = it.hasNext() ? it.next() : null;
                    Item next2 = it2.hasNext() ? it2.next() : null;
                    if (next == null) {
                        compareTo = next2 == null ? 0 : next2.compareTo(next) * (-1);
                    } else {
                        compareTo = next.compareTo(next2);
                    }
                } while (compareTo == 0);
                return compareTo;
            }
            throw new RuntimeException("invalid item: " + item.getClass());
        }

        @Override // java.util.AbstractCollection
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Iterator<Item> it = iterator();
            while (it.hasNext()) {
                Item next = it.next();
                if (sb.length() > 0 && !(next instanceof ListItem)) {
                    sb.append('.');
                }
                sb.append(next);
            }
            if (this.separator != null) {
                return this.separator + sb.toString();
            }
            return sb.toString();
        }
    }

    public VersionNumber(String str) {
        parseVersion(str);
    }

    private void parseVersion(String str) {
        this.value = str;
        ListItem listItem = new ListItem();
        this.items = listItem;
        Stack stack = new Stack();
        stack.push(listItem);
        int i = 0;
        boolean z = false;
        for (int i2 = 0; i2 < str.length(); i2++) {
            char charAt = str.charAt(i2);
            if (charAt == '.') {
                if (i2 == i) {
                    listItem.add(IntegerItem.ZERO);
                } else {
                    listItem.add(parseItem(str.substring(i, i2)));
                }
                i = i2 + 1;
            } else if ("!\"#$%&'()*+,-/:;<=>?@[\\]^_`{|}~".indexOf(charAt) != -1) {
                if (i2 == i) {
                    listItem.add(IntegerItem.ZERO);
                } else {
                    listItem.add(parseItem(str.substring(i, i2)));
                }
                i = i2 + 1;
                ListItem listItem2 = new ListItem(charAt);
                listItem.add(listItem2);
                stack.push(listItem2);
                listItem = listItem2;
            } else if (Character.isDigit(charAt)) {
                if (!z && i2 > i) {
                    listItem.add(parseItem(str.substring(i, i2)));
                    ListItem listItem3 = new ListItem();
                    listItem.add(listItem3);
                    stack.push(listItem3);
                    listItem = listItem3;
                    i = i2;
                }
                z = true;
            } else {
                if (z && i2 > i) {
                    listItem.add(parseItem(str.substring(i, i2)));
                    ListItem listItem4 = new ListItem();
                    listItem.add(listItem4);
                    stack.push(listItem4);
                    listItem = listItem4;
                    i = i2;
                }
                z = false;
            }
        }
        if (str.length() > i) {
            listItem.add(parseItem(str.substring(i)));
        }
        while (!stack.isEmpty()) {
            ((ListItem) stack.pop()).normalize();
        }
        this.canonical = this.items.toString();
    }

    private static Item parseItem(String str) {
        return str.chars().allMatch(new IntPredicate() { // from class: com.qcl.launcher.utils.versioning.VersionNumber$$ExternalSyntheticLambda1
            @Override // java.util.function.IntPredicate
            public final boolean test(int i) {
                boolean isDigit;
                isDigit = Character.isDigit(i);
                return isDigit;
            }
        }) ? new IntegerItem(str) : new StringItem(str);
    }

    @Override // java.lang.Comparable
    public int compareTo(VersionNumber versionNumber) {
        return this.items.compareTo(versionNumber.items);
    }

    public String toString() {
        return this.value;
    }

    public String getCanonical() {
        return this.canonical;
    }

    public boolean equals(Object obj) {
        return (obj instanceof VersionNumber) && this.canonical.equals(((VersionNumber) obj).canonical);
    }

    public int hashCode() {
        return this.canonical.hashCode();
    }
}
